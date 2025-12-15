package org.anthills.api;

import java.time.Duration;

public record WorkProcessorConfig(
  int parallelism,
  int maxBatchSize,
  int defaultMaxRetries,
  int maxAllowedRetries, // to avoid poison distribution and harming the cluster
  Duration leaseDuration,
  Duration leaseRenewInterval,
  Duration pollInterval,
  Duration shutdownTimeout
) {
  public static WorkProcessorConfig defaults() {
    return new WorkProcessorConfig(1, 10, 5, 10, Duration.ofMinutes(5), Duration.ofMinutes(2), Duration.ofSeconds(5), Duration.ofSeconds(30));
  }
}
