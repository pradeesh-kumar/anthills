package org.anthills.api;

public interface LeasedScheduler extends AutoCloseable {

  /**
   * Schedule a distributed job.
   *
   * @param jobName unique logical name (cluster-wide)
   * @param schedule cron or fixed-rate schedule
   * @param job business logic to execute
   *
   * @throws IllegalArgumentException if jobName is already scheduled
   */
  void schedule(String jobName, Schedule schedule, Job job);
}
