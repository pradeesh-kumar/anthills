package org.anthills.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class Watch {

  private static final Logger log = LoggerFactory.getLogger(Watch.class);

  private final String name;
  private final Runnable task;
  private final Duration period;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread thread;

  public Watch(String name, Runnable task, Duration period) {
    this.name = name;
    this.task = task;
    this.period = period;
  }

  public void start() {
    if (!running.compareAndSet(false, true)) {
      throw new IllegalStateException("Already started");
    }
    log.debug("[{}] Started watch", name);
    thread = Thread.startVirtualThread(() -> {
      Thread.currentThread().setName("Watch-" + name);
      while (running.get()) {
        try {
          task.run();
        } catch (Throwable t) {
          log.warn("[{}] Failed to run the watch task", name, t);
        }
        LockSupport.parkNanos(period.toNanos());
        if (Thread.interrupted()) {
          break;
        }
      }
      log.debug("[{}] Watch thread exiting", name);
    });
  }

  public void stop() {
    running.set(false);
    if (thread != null) {
      thread.interrupt();
    }
    log.debug("[{}] Stopped the watch", name);
  }
}
