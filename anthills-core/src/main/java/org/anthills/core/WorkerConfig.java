package org.anthills.core;

import java.time.Duration;

public record WorkerConfig(int workers, int retries, Duration workPollPeriod, Duration initialDelay) {

  public static WorkerConfig defaultConfig() {
    return new WorkerConfig(1, 0, Duration.ofSeconds(5), Duration.ZERO);
  }

  // TODO create builder
}
