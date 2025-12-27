package org.anthills.core.concurrent;

import org.anthills.core.LeaseRenewer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes a task while periodically renewing an associated lease.
 * A background scheduler triggers the provided {@link org.anthills.core.LeaseRenewer}
 * at a fixed {@code renewInterval}. If renewal fails, subsequent renewals stop and
 * the task will be allowed to complete, after which renewal stops automatically.
 *
 * Thread-safety: instances are thread-safe for concurrent {@link #execute(Runnable, org.anthills.core.LeaseRenewer, java.util.concurrent.Executor)}
 * calls. Shutdown stops only the internal renewal scheduler.
 */
public final class LeaseBoundExecutor {

  private final ScheduledExecutorService renewScheduler;
  private final Duration renewInterval;

  /**
   * Creates an executor that renews leases on a fixed interval.
   *
   * @param renewInterval how often to attempt renewal; must be positive
   * @param threadNamePrefix prefix used to name the renewal scheduler thread(s)
   * @throws NullPointerException if any argument is null
   */
  public LeaseBoundExecutor(Duration renewInterval, String threadNamePrefix) {
    this.renewInterval = Objects.requireNonNull(renewInterval, "renewInterval is required");
    this.renewScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(threadNamePrefix + "-lease-renew", true));
  }

  /**
   * Submits a task for execution, periodically invoking {@code renewer.renew()} while it runs.
   * If a renewal attempt returns {@code false} or throws, renewal is stopped (best-effort) and
   * the task continues until completion. The renewal task is cancelled when the task finishes.
   *
   * @param task the unit of work to run
   * @param renewer callback to renew the lease while {@code task} is running
   * @param executor the executor used to run {@code task}
   * @return a {@link Future} representing task completion
   * @throws NullPointerException if any parameter is null
   */
  public Future<?> execute(Runnable task, LeaseRenewer renewer, Executor executor) {
    Objects.requireNonNull(task);
    Objects.requireNonNull(renewer);
    Objects.requireNonNull(executor);

    AtomicBoolean running = new AtomicBoolean(true);

    ScheduledFuture<?> renewTask =
      renewScheduler.scheduleAtFixedRate(() -> {
        if (!running.get()) return;
        try {
          if (!renewer.renew()) {
            running.set(false);
          }
        } catch (Exception e) {
          // log and continue (best-effort)
        }
      }, renewInterval.toMillis(), renewInterval.toMillis(), TimeUnit.MILLISECONDS);

    return CompletableFuture.runAsync(() -> {
      try {
        task.run();
      } finally {
        running.set(false);
        renewTask.cancel(true);
      }
    }, executor);
  }

  /**
   * Shuts down the internal renewal scheduler and waits up to {@code timeout}
   * for termination of renewal tasks. Does not shut down the supplied task executor.
   *
   * @param timeout maximum time to wait for the renewal scheduler to terminate
   * @throws InterruptedException if interrupted while waiting
   */
  public void shutdown(Duration timeout) throws InterruptedException {
    renewScheduler.shutdown();
    renewScheduler.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
  }
}
