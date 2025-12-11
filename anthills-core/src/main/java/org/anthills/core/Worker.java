package org.anthills.core;

public interface Worker {
  void start();
  void stop();
  void awaitTermination();
}
