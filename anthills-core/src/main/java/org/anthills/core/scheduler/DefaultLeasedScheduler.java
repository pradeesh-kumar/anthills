package org.anthills.core.scheduler;

import org.anthills.api.scheduler.Job;
import org.anthills.api.scheduler.LeasedScheduler;
import org.anthills.api.scheduler.Schedule;
import org.anthills.api.scheduler.SchedulerConfig;
import org.anthills.api.work.WorkStore;
import org.anthills.core.concurrent.LeaseBoundExecutor;
import org.anthills.core.concurrent.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of {@link LeasedScheduler} that triggers scheduled jobs and
 * coordinates distributed execution using scheduler leases persisted in a {@link WorkStore}.
 *
 * Design
 * - A single-threaded scheduler computes the next trigger per job and enqueues work.
 * - Before executing, it tries to acquire a lease in the store so only one node runs the job.
 * - While the job runs, a {@code LeaseBoundExecutor} keeps the lease alive.
 * - Whether a trigger acquires a lease or not, the next trigger is always scheduled.
 *
 * Thread-safety
 * - Registration must happen before {@link #start()}.
 * - Start/stop are idempotent; internal executors coordinate lifecycle.
 */
public final class DefaultLeasedScheduler implements LeasedScheduler {

  private static final Logger log = LoggerFactory.getLogger(DefaultLeasedScheduler.class);

  private final SchedulerConfig config;
  private final WorkStore store;
  private final String ownerId;
  private final ScheduledExecutorService triggerScheduler;
  private final ExecutorService jobExecutor;
  private final LeaseBoundExecutor leaseExecutor;
  private final Map<String, ScheduledJob> jobs = new ConcurrentHashMap<>();
  private volatile boolean running = false;

  /**
   * Creates a scheduler with the supplied configuration and store.
   *
   * @param config tuning parameters controlling lease timing and shutdown behavior
   * @param store persistence used to acquire/renew scheduler leases
   * @throws NullPointerException if any parameter is null
   */
  public DefaultLeasedScheduler(SchedulerConfig config, WorkStore store) {
    this.store = Objects.requireNonNull(store, "store");
    this.config = Objects.requireNonNull(config, "config");
    this.ownerId = generateOwnerId();
    this.triggerScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("anthills-scheduler-trigger", true));
    this.jobExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("anthills-scheduler-job", false));
    this.leaseExecutor = new LeaseBoundExecutor(config.leaseRenewInterval(), "anthills-scheduler");
  }

  @Override
  /**
   * Registers a job to be executed according to the provided schedule.
   * Must be invoked before {@link #start()}.
   *
   * @param jobName unique logical name used for the distributed lease
   * @param schedule describes the cadence for firing the job
   * @param job code to execute on each trigger
   * @throws IllegalArgumentException if a job with the same name is already registered
   * @throws NullPointerException if any parameter is null
   */
  public void schedule(String jobName, Schedule schedule, Job job) {
    requireNotRunning();

    Objects.requireNonNull(jobName);
    Objects.requireNonNull(schedule);
    Objects.requireNonNull(job);

    if (jobs.putIfAbsent(jobName, new ScheduledJob(jobName, schedule, job)) != null) {
      throw new IllegalArgumentException("Job already scheduled: " + jobName);
    }
  }

  /**
   * Starts the scheduler if not already running and schedules the first trigger for all jobs.
   * Idempotent.
   */
  @Override
  public void start() {
    if (running) {
      return;
    }
    running = true;
    jobs.values().forEach(this::scheduleNext);
  }

  /**
   * Requests a graceful stop: trigger scheduler is shut down immediately,
   * worker executor is shut down, and the lease renewer is stopped.
   * Idempotent.
   */
  @Override
  public void stop() {
    running = false;

    triggerScheduler.shutdownNow();
    jobExecutor.shutdown();

    try {
      leaseExecutor.shutdown(config.shutdownTimeout());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Waits up to {@link SchedulerConfig#shutdownTimeout()} for worker tasks to finish.
   * The waiting thread is restored on interruption.
   */
  @Override
  public void awaitTermination() {
    try {
      jobExecutor.awaitTermination(config.shutdownTimeout().toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Closes the scheduler by calling {@link #stop()} followed by {@link #awaitTermination()}.
   * Safe to use with try-with-resources.
   */
  @Override
  public void close() {
    this.stop();
    this.awaitTermination();
  }

  /**
   * Computes and schedules the next trigger for the given job.
   * Always called after each trigger to ensure continuous scheduling.
   */
  private void scheduleNext(ScheduledJob job) {
    Duration delay = job.schedule().nextDelay();
    triggerScheduler.schedule(() -> onTrigger(job), delay.toMillis(), TimeUnit.MILLISECONDS);
  }

  /**
   * Callback executed at trigger time. Attempts to acquire a lease and, if successful,
   * dispatches the job execution to the worker executor.
   */
  private void onTrigger(ScheduledJob job) {
    if (!running) {
      return;
    }
    try {
      boolean acquired = store.tryAcquireSchedulerLease(job.name(), ownerId, config.leaseDuration());
      if (acquired) {
        jobExecutor.submit(() -> runJob(job));
      }
    } finally {
      // Always schedule next trigger
      scheduleNext(job);
    }
  }

  /**
   * Executes the job using a {@link org.anthills.core.concurrent.LeaseBoundExecutor} so that
   * the scheduler lease is periodically renewed while the job runs.
   * Any exception thrown by the job is logged; the next trigger is still scheduled.
   */
  private void runJob(ScheduledJob job) {
    leaseExecutor.execute(() -> {
      try {
        job.job().run();
      } catch (Exception e) {
        log.error("Scheduled Job {} Failed", job.name, e);
      }
    },() -> store.renewSchedulerLease(job.name(), ownerId, config.leaseDuration()), jobExecutor);
  }

  /**
   * Ensures the scheduler hasn't been started yet; used to guard configuration changes.
   *
   * @throws IllegalStateException if the scheduler is already running
   */
  private void requireNotRunning() {
    if (running) {
      throw new IllegalStateException("Cannot modify scheduler after start()");
    }
  }

  /**
   * Generates a unique owner id for distinguishing this scheduler instance during leasing.
   */
  private static String generateOwnerId() {
    return java.util.UUID.randomUUID().toString();
  }

  /**
   * Immutable registration of a scheduled job.
   */
  private record ScheduledJob(
    String name,
    Schedule schedule,
    Job job
  ) {}
}
