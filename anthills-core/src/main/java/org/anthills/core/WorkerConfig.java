package org.anthills.core;

import java.time.Duration;
import java.util.UUID;

public record WorkerConfig(String jobName, int workers, int retries, Duration workPollPeriod, Duration initialDelay) {

  public static WorkerConfig defaultConfig() {
    return new WorkerConfig("Job-" + UUID.randomUUID(), 1, 0, Duration.ofSeconds(5), Duration.ZERO);
  }

  // TODO create builder
}
