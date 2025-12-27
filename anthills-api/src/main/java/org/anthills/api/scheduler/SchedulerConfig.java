package org.anthills.api.scheduler;

import java.time.Duration;

public record SchedulerConfig(
  Duration leaseDuration,
  Duration leaseRenewInterval,
  Duration shutdownTimeout
) {
  public static SchedulerConfig defaults() {
    return new SchedulerConfig(Duration.ofMinutes(5), Duration.ofMinutes(2), Duration.ofSeconds(30));
  }
}
