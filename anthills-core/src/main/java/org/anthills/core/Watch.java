package org.anthills.core;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class Watch {

  private final Runnable task;
  private final Duration period;
  private final AtomicBoolean running;

  public Watch(Runnable task, Duration period) {
    this.task = task;
    this.period = period;
    this.running = new AtomicBoolean(false);
  }

  public void start() {
    if (!running.compareAndSet(false, true)) {
      throw new IllegalStateException("Already started");
    }
    Thread.startVirtualThread(() -> {
      while (running.get()) {
        // TODO log.debug("watching")
        task.run();
        try {
          Thread.sleep(period.toMillis());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
  }

  public void stop() {
    // TODO log
    running.set(false);
  }
}
