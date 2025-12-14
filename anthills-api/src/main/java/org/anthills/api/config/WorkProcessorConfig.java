package org.anthills.api.config;

import java.time.Duration;

public record WorkProcessorConfig(
  int parallelism,
  int maxBatchSize,
  Duration leaseDuration,
  Duration leaseRenewInterval,
  Duration pollInterval,
  Duration shutdownTimeout
) {
  public static WorkProcessorConfig defaults() {
  }
}
