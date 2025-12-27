package org.anthills.api.work;

import java.time.Duration;

/**
 * Configuration for a {@link WorkRequestProcessor}'s polling and execution behavior.
 *
 * @param workerThreads number of concurrent worker threads
 * @param defaultMaxRetries default retry limit per request when the request does not specify one
 * @param maxAllowedRetries hard upper bound on retries to avoid poison message amplification
 * @param pollInterval interval between store polls when no work is available
 * @param leaseDuration how long a claimed work item is leased to a worker
 * @param leaseRenewInterval how frequently an active lease is renewed (must be < leaseDuration)
 * @param shutdownTimeout maximum time to wait for graceful shutdown
 */
public record ProcessorConfig(
  int workerThreads,
  int defaultMaxRetries,
  int maxAllowedRetries, // to avoid poison distribution and harming the cluster
  Duration pollInterval,
  Duration leaseDuration,
  Duration leaseRenewInterval,
  Duration shutdownTimeout
) {

  /**
   * Validates configuration invariants:
   * - workerThreads > 0
   * - retries >= 0
   * - defaultMaxRetries <= maxAllowedRetries
   * - leaseRenewInterval < leaseDuration
   *
   * @throws IllegalArgumentException if configuration is invalid
   */
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

  /**
   * Provides a sensible default configuration derived from available CPUs.
   *
   * workerThreads = max(1, availableProcessors)
   * defaultMaxRetries = 3
   * maxAllowedRetries = 10
   * pollInterval = 1s
   * leaseDuration = 30s
   * leaseRenewInterval = 10s
   * shutdownTimeout = 30s
   *
   * @return default processor configuration
   */
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
