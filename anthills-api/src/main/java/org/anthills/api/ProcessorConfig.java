package org.anthills.api;

import java.time.Duration;

public record ProcessorConfig(
  int workerThreads,
  int defaultMaxRetries,
  int maxAllowedRetries, // to avoid poison distribution and harming the cluster
  Duration pollInterval,
  Duration leaseDuration,
  Duration leaseRenewInterval,
  Duration shutdownTimeout
) {

  public ProcessorConfig {
    if (workerThreads <= 0) {
      throw new IllegalArgumentException("workerThreads must be > 0");
    }
    if (defaultMaxRetries < 0 || maxAllowedRetries < 0) {
      throw new IllegalArgumentException("retries must be >= 0");
    }
    if (defaultMaxRetries > maxAllowedRetries) {
      throw new IllegalArgumentException("defaultMaxRetries cannot exceed maxAllowedRetries");
    }
    if (leaseRenewInterval.compareTo(leaseDuration) >= 0) {
      throw new IllegalArgumentException("leaseRenewInterval must be < leaseDuration");
    }
  }

  public static ProcessorConfig defaults() {
    int cpu = Runtime.getRuntime().availableProcessors();

    return new ProcessorConfig(
      Math.max(1, cpu),                 // workerThreads
      3,                                 // defaultMaxRetries
      10,                                // maxAllowedRetries
      Duration.ofSeconds(1),             // pollInterval
      Duration.ofSeconds(30),            // leaseDuration
      Duration.ofSeconds(10),            // leaseRenewInterval
      Duration.ofSeconds(30)            // shutdownTimeout
    );
  }
}
