package org.anthills.api.config;

import java.time.Duration;

public record SchedulerConfig(
  Duration leasePeriod
) {
  public static SchedulerConfig defaults() {
    return new SchedulerConfig(Duration.ofMinutes(5));
  }
}
