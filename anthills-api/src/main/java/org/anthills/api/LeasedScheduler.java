package org.anthills.api;

public interface LeasedScheduler extends AutoCloseable {
  void start();
  void stop();
  void awaitTermination() throws InterruptedException;
}
