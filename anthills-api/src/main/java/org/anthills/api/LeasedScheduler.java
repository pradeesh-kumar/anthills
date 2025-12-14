package org.anthills.api;

public interface LeasedScheduler {
  JobHandle schedule(String jobName, Schedule schedule, ScheduledJob job);
}
