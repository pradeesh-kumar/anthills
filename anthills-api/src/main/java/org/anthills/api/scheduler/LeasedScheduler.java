package org.anthills.api.scheduler;

/**
 * LeasedSchedule ensures your Job runs only on one node at a time in the cluster by utilizing a database lease.
 */
public interface LeasedScheduler extends AutoCloseable {

  /**
   * Registers a named job to be executed according to the provided schedule.
   *
   * @param jobName unique name of the job for identification and lease ownership
   * @param schedule execution plan (fixed rate or cron)
   * @param job the code to run when the schedule triggers
   * @throws IllegalArgumentException if any parameter is invalid
   */
  void schedule(String jobName, Schedule schedule, Job job);

  /**
   * Starts scheduling and executing registered jobs.
   * Idempotent: calling multiple times should be safe.
   */
  void start();

  /**
   * Initiates a graceful stop: no new executions are started and
   * in-flight jobs may continue until completion or lease expiry.
   */
  void stop();

  /**
   * Blocks until the scheduler has fully terminated after {@link #stop()} was invoked.
   *
   * @throws InterruptedException if the waiting thread is interrupted
   */
  void awaitTermination() throws InterruptedException;
}
