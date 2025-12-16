package org.anthills.api;

public interface LeasedScheduler extends AutoCloseable {
  void schedule(String jobName, Schedule schedule, Job job);
  void start();
  void stop();
  void awaitTermination() throws InterruptedException;
}
