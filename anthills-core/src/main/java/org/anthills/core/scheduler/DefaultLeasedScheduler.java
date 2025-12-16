package org.anthills.core.scheduler;

import org.anthills.api.Job;
import org.anthills.api.LeasedScheduler;
import org.anthills.api.Schedule;
import org.anthills.api.SchedulerConfig;
import org.anthills.api.WorkStore;
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

  public DefaultLeasedScheduler(SchedulerConfig config, WorkStore store) {
    this.store = Objects.requireNonNull(store, "store");
    this.config = Objects.requireNonNull(config, "config");
    this.ownerId = generateOwnerId();
    this.triggerScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("anthills-scheduler-trigger", true));
    this.jobExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("anthills-scheduler-job", false));
    this.leaseExecutor = new LeaseBoundExecutor(config.leaseRenewInterval(), "anthills-scheduler");
  }

  @Override
  public void schedule(String jobName, Schedule schedule, Job job) {
    requireNotRunning();

    Objects.requireNonNull(jobName);
    Objects.requireNonNull(schedule);
    Objects.requireNonNull(job);

    if (jobs.putIfAbsent(jobName, new ScheduledJob(jobName, schedule, job)) != null) {
      throw new IllegalArgumentException("Job already scheduled: " + jobName);
    }
  }

  @Override
  public void start() {
    if (running) {
      return;
    }
    running = true;
    jobs.values().forEach(this::scheduleNext);
  }

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

  @Override
  public void awaitTermination() {
    try {
      jobExecutor.awaitTermination(config.shutdownTimeout().toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void close() {
    this.stop();
    this.awaitTermination();
  }

  private void scheduleNext(ScheduledJob job) {
    Duration delay = job.schedule().nextDelay();
    triggerScheduler.schedule(() -> onTrigger(job), delay.toMillis(), TimeUnit.MILLISECONDS);
  }

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

  private void runJob(ScheduledJob job) {
    leaseExecutor.execute(() -> {
      try {
        job.job().run();
      } catch (Exception e) {
        log.error("Scheduled Job {} Failed", job.name, e);
      }
    }, () -> store.renewSchedulerLease(job.name(), ownerId, config.leaseDuration()));
    store.releaseSchedulerLease(job.name(), ownerId);
  }

  private void requireNotRunning() {
    if (running) {
      throw new IllegalStateException("Cannot modify scheduler after start()");
    }
  }

  private static String generateOwnerId() {
    return java.util.UUID.randomUUID().toString();
  }

  private record ScheduledJob(
    String name,
    Schedule schedule,
    Job job
  ) {}
}
