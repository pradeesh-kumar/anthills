package org.anthills.core.concurrent;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

  private final String namePrefix;
  private final boolean daemon;
  private final int priority;
  private final Thread.UncaughtExceptionHandler exceptionHandler;
  private final AtomicInteger threadCounter = new AtomicInteger(1);

  public NamedThreadFactory(String namePrefix) {
    this(namePrefix, false);
  }

  public NamedThreadFactory(String namePrefix, boolean daemon) {
    this(namePrefix, daemon, Thread.NORM_PRIORITY, (t, e) -> {
      System.err.println("Uncaught exception in thread " + t.getName());
      e.printStackTrace(System.err);
    });
  }

  public NamedThreadFactory(String namePrefix, boolean daemon, int priority, Thread.UncaughtExceptionHandler exceptionHandler) {
    this.namePrefix = Objects.requireNonNull(namePrefix, "namePrefix");
    this.daemon = daemon;
    this.priority = priority;
    this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread thread = new Thread(r);
    thread.setName(namePrefix + "-" + threadCounter.getAndIncrement());
    thread.setDaemon(daemon);
    thread.setPriority(priority);
    thread.setUncaughtExceptionHandler(exceptionHandler);
    return thread;
  }
}
