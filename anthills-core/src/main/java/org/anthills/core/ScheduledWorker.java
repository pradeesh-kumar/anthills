package org.anthills.core;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public sealed class ScheduledWorker implements Worker permits LeasedScheduledWorker {

  protected final SchedulerConfig config;
  protected final Runnable task;
  protected final AtomicBoolean started = new AtomicBoolean(false);
  protected final AtomicBoolean stopped = new AtomicBoolean(false);
  protected final AtomicBoolean running = new AtomicBoolean(false);

  private final String identity;
  private ScheduledExecutorService executor;

  public ScheduledWorker(SchedulerConfig config, Runnable task) {
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

  public ScheduledWorker(Runnable task) {
    this(SchedulerConfig.defaultConfig(), task);
  }

  protected void runTask() {
    if (!running.compareAndSet(false, true)) {
      throw new IllegalStateException("Already running");
    }
    try {
      task.run();
    } catch (Exception e) {
      e.printStackTrace(); // TODO use Logger
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
    if (this.executor != null) {
      this.executor.shutdown();
    }
    this.stopped.set(true);
  }

  @Override
  public void awaitTermination() {
    try {
      this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      e.printStackTrace();
      // LOG WARN
    }
  }

  protected String identity() {
    return this.identity;
  }
}
