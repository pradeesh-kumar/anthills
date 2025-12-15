package org.anthills.api;

import java.time.Duration;

public record SchedulerConfig(
  Duration leaseDuration,
  Duration leaseRenewalInterval,
  Duration shutdownTimeout
) {
  public static SchedulerConfig defaultConfig() {
    return new SchedulerConfig(Duration.ofMinutes(5), Duration.ofMinutes(2), Duration.ofSeconds(30));
  }
}
