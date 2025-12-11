package org.anthills.core;

import java.time.Duration;

public record SchedulerConfig(String jobName, Duration initialDelay, Duration period) {

  public static SchedulerConfig defaultConfig(String jobName) {
    return new SchedulerConfig(jobName, Duration.ZERO, Duration.ofSeconds(1));
  }

  // TODO create builder
}
