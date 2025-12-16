package org.anthills.core.concurrent;

import org.anthills.core.LeaseRenewer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LeaseBoundExecutor {

  private final ScheduledExecutorService renewScheduler;
  private final Duration renewInterval;

  public LeaseBoundExecutor(Duration renewInterval, String threadNamePrefix) {
    this.renewInterval = Objects.requireNonNull(renewInterval, "renewInterval is required");
    this.renewScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(threadNamePrefix + "-lease-renew", true));
  }

  public Future<?> execute(Runnable task, LeaseRenewer renewer) {
    Objects.requireNonNull(task, "task is required");
    Objects.requireNonNull(renewer, "renewer is required");
    AtomicBoolean running = new AtomicBoolean(true);
    ScheduledFuture<?> renewTask = renewScheduler.scheduleAtFixedRate(() -> {
      if (!running.get()) {
        return;
      }
      if (!renewer.renew()) {
        running.set(false);
      }
    }, renewInterval.toMillis(), renewInterval.toMillis(), TimeUnit.MILLISECONDS);
    return CompletableFuture.runAsync(() -> {
      try {
        task.run();
      } finally {
        running.set(false);
        renewTask.cancel(true);
      }
    });
  }

  public void shutdown(Duration timeout) throws InterruptedException {
    renewScheduler.shutdown();
    renewScheduler.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
  }
}
