package org.anthills.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class Watch {

  private static final Logger log = LoggerFactory.getLogger(Watch.class);

  private final String name;
  private final Runnable task;
  private final Duration period;
  private final AtomicBoolean running;

  public Watch(String name, Runnable task, Duration period) {
    this.name = name;
    this.task = task;
    this.period = period;
    this.running = new AtomicBoolean(false);
  }

  public void start() {
    if (!running.compareAndSet(false, true)) {
      throw new IllegalStateException("Already started");
    }
    log.debug("[{}] Started watch", name);
    Thread.startVirtualThread(() -> {
      while (running.get()) {
        log.debug("[{}] Running the Watch task", name);
        try {
          task.run();
          Thread.sleep(period.toMillis());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (Throwable t) {
          log.warn("[{}] Failed to run the Watch Task", name, t);
        }
      }
    });
  }

  public void stop() {
    running.set(false);
    log.debug("[{}] Stopped the watch", name);
  }
}
