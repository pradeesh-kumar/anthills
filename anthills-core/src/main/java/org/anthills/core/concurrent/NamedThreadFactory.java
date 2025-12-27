package org.anthills.core.concurrent;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link ThreadFactory} that assigns a consistent name prefix, daemon flag,
 * priority, and uncaught-exception handler to created threads.
 *
 * Threads are named as {@code <namePrefix>-<counter>} where the counter is 1-based.
 */
public class NamedThreadFactory implements ThreadFactory {

  private final String namePrefix;
  private final boolean daemon;
  private final int priority;
  private final Thread.UncaughtExceptionHandler exceptionHandler;
  private final AtomicInteger threadCounter = new AtomicInteger(1);

  /**
   * Creates a factory that produces non-daemon threads with the given name prefix,
   * normal priority, and a default uncaught-exception handler that logs to stderr.
   *
   * @param namePrefix prefix used to name threads
   */
  public NamedThreadFactory(String namePrefix) {
    this(namePrefix, false);
  }

  /**
   * Creates a factory with the given name prefix and daemon setting.
   * Uses normal priority and a default stderr logging uncaught-exception handler.
   *
   * @param namePrefix prefix used to name threads
   * @param daemon whether created threads are daemon threads
   */
  public NamedThreadFactory(String namePrefix, boolean daemon) {
    this(namePrefix, daemon, Thread.NORM_PRIORITY, (t, e) -> {
      System.err.println("Uncaught exception in thread " + t.getName());
      e.printStackTrace(System.err);
    });
  }

  /**
   * Creates a fully configurable thread factory.
   *
   * @param namePrefix prefix used to name threads
   * @param daemon whether created threads are daemon threads
   * @param priority thread priority (e.g., {@link Thread#NORM_PRIORITY})
   * @param exceptionHandler handler for uncaught exceptions
   */
  public NamedThreadFactory(String namePrefix, boolean daemon, int priority, Thread.UncaughtExceptionHandler exceptionHandler) {
    this.namePrefix = Objects.requireNonNull(namePrefix, "namePrefix");
    this.daemon = daemon;
    this.priority = priority;
    this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
  }

  /**
   * Creates a new thread with the configured characteristics.
   *
   * @param r the runnable to execute
   * @return configured thread instance
   */
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
