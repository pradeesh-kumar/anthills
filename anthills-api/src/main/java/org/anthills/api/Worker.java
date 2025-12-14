package org.anthills.api;

public interface Worker extends AutoCloseable {
  void start();
  void stop();
  void awaitTermination() throws InterruptedException;

  @Override
  default void close() {
    stop();
  }
}
