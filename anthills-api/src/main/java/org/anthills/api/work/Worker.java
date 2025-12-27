package org.anthills.api.work;

/**
 * Lifecycle contract for components that process work in the background.
 * Implementations should be thread-safe and support graceful shutdown.
 */
public interface Worker extends AutoCloseable {

  /**
   * Starts the worker. Calling this method multiple times should be safe (idempotent).
   * Implementations should return quickly and perform work asynchronously.
   */
  void start();

  /**
   * Initiates a graceful stop: no new units of work are started, while
   * in-flight work may continue until completion or lease expiry.
   * This method should return quickly.
   */
  void stop();

  /**
   * Blocks until the worker has fully terminated after {@link #stop()} was called.
   *
   * @throws InterruptedException if the waiting thread is interrupted
   */
  void awaitTermination() throws InterruptedException;

  @Override
  /**
   * Closes this worker, delegating to {@link #stop()} for convenience.
   * This enables try-with-resources usage.
   */
  default void close() {
    stop();
  }
}
