package org.anthills.api;

import java.util.List;
import java.util.Optional;

public interface ScheduledJobClient {
  ScheduledJob create(String jobName, Schedule schedule, Job job);
  Optional<ScheduledJob> get(String id);
  List<ScheduledJob> list();
  void updateSchedule(String id, Schedule schedule);
  void activate(String id);
  void deactivate(String id);
  void delete(String id);
}
