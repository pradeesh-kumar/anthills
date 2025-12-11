package org.anthills.core;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public sealed class ScheduledWorker implements Worker permits LeasedScheduledWorker {

  private static final Logger log =  LoggerFactory.getLogger(ScheduledWorker.class);

  protected final SchedulerConfig config;
  protected final Runnable task;
  protected final AtomicBoolean started = new AtomicBoolean(false);
  protected final AtomicBoolean stopped = new AtomicBoolean(false);
  protected final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicReference<Thread> taskThread = new AtomicReference<>();

  private final String identity;
  private ScheduledExecutorService executor;

  ScheduledWorker(SchedulerConfig config, Runnable task) {
    Objects.requireNonNull(config);
    Objects.requireNonNull(config.period());
    Objects.requireNonNull(config.initialDelay());
    Objects.requireNonNull(task);
    if (config.period().isZero()) {
      throw new IllegalArgumentException("Schedule period cannot be zero");
    }
    this.config = config;
    this.task = task;
    this.identity = UUID.randomUUID().toString();
  }

  protected void runTask() {
    if (!running.compareAndSet(false, true)) {
      log.debug("[{}] Existing task is still running.", identity);
      return;
    }
    taskThread.set(Thread.currentThread());
    try {
      log.debug("[{}] Starting task", identity);
      task.run();
      log.debug("[{}]Task completed", identity);
    } catch (Exception e) {
      log.error("[{}] Error running task", identity, e);
    } finally {
      running.set(false);
    }
  }

  @PostConstruct
  @Override
  public void start() {
    if (stopped.get()) {
      throw new IllegalStateException("ScheduledWorker has been disposed already!");
    }
    if (!started.compareAndSet(false, true)) {
      throw new IllegalStateException("Already started");
    }
    Objects.requireNonNull(task, "Task cannot be null!");
    this.executor = Executors.newScheduledThreadPool(1);
    this.executor.scheduleAtFixedRate(this::runTask, config.initialDelay().getSeconds(), config.period().getSeconds(), TimeUnit.SECONDS);
  }

  @PreDestroy
  public void stop() {
    log.debug("[{}] Shutting down ScheduledWorker", identity);
    if (this.executor != null) {
      this.executor.shutdown();
    }
    this.stopped.set(true);
    log.info("[{}] ScheduledWorker Shutdown complete", identity);
  }

  protected void interruptCurrentTask() {
    log.debug("[{}] Interrupting the current task", identity);
    taskThread.get().interrupt();
  }

  @Override
  public void awaitTermination() {
    try {
      this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      log.error("[{}] Interrupted while waiting for task to complete", identity, e);
    }
  }

  protected String identity() {
    return this.identity;
  }
}
