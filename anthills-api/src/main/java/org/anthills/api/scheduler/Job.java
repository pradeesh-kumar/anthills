package org.anthills.api.scheduler;

/**
 * A unit of work executed by a scheduler invocation.
 */
@FunctionalInterface
public interface Job {

  /**
   * Executes the job logic once.
   *
   * @throws Exception if execution fails; the scheduler determines how failures are handled
   *                   (e.g., retry, log, or skip) depending on implementation.
   */
  void run() throws Exception;
}
