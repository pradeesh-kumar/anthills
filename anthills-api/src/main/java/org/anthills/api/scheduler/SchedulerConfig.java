package org.anthills.api.scheduler;

import java.time.Duration;

/**
 * Configuration for a leased scheduler's runtime behavior.
 *
 * @param leaseDuration how long a scheduler lease is held for a job trigger
 * @param leaseRenewInterval how frequently an active lease should be renewed (must be < leaseDuration)
 * @param shutdownTimeout maximum time to wait for graceful shutdown
 */
public record SchedulerConfig(
  Duration leaseDuration,
  Duration leaseRenewInterval,
  Duration shutdownTimeout
) {

  /**
   * Provides a sensible default configuration:
   * leaseDuration=5m, leaseRenewInterval=2m, shutdownTimeout=30s.
   *
   * @return default scheduler configuration
   */
  public static SchedulerConfig defaults() {
    return new SchedulerConfig(Duration.ofMinutes(5), Duration.ofMinutes(2), Duration.ofSeconds(30));
  }
}
