package org.anthills.core;

import java.time.Duration;

public record SchedulerConfig(Duration initialDelay, Duration period) {

  public static SchedulerConfig defaultConfig() {
    return new SchedulerConfig(Duration.ZERO, Duration.ofSeconds(1));
  }
}
