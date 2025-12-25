package org.anthills.core.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class Backoff {

  public enum Strategy {
    FIXED,
    EXPONENTIAL
  }

  private final Strategy strategy;
  private final Duration baseDelay;
  private final Duration maxDelay;
  private final boolean jitter;

  private Backoff(Strategy strategy, Duration baseDelay, Duration maxDelay, boolean jitter) {
    this.strategy = Objects.requireNonNull(strategy);
    this.baseDelay = Objects.requireNonNull(baseDelay);
    this.maxDelay = Objects.requireNonNull(maxDelay);
    this.jitter = jitter;
  }

  public static Backoff fixed(Duration delay) {
    return new Backoff(Strategy.FIXED, delay, delay, false);
  }

  public static Backoff exponential(Duration baseDelay, Duration maxDelay, boolean jitter) {
    if (baseDelay.isNegative() || baseDelay.isZero()) {
      throw new IllegalArgumentException("baseDelay must be > 0");
    }
    if (maxDelay.compareTo(baseDelay) < 0) {
      throw new IllegalArgumentException("maxDelay must be >= baseDelay");
    }
    return new Backoff(Strategy.EXPONENTIAL, baseDelay, maxDelay, jitter);
  }

  /**
   * Calculate the delay before the next attempt.
   *
   * @param attempt attempt number (1-based)
   */
  public Duration nextDelay(int attempt) {
    if (attempt <= 0) {
      throw new IllegalArgumentException("attempt must be >= 1");
    }
    Duration delay;
    switch (strategy) {
      case FIXED -> delay = baseDelay;
      case EXPONENTIAL -> {
        long multiplier = 1L << Math.min(attempt - 1, 30);
        delay = baseDelay.multipliedBy(multiplier);
        if (delay.compareTo(maxDelay) > 0) {
          delay = maxDelay;
        }
      }
      default -> throw new IllegalStateException("Unknown strategy: " + strategy);
    }
    if (jitter) {
      delay = applyJitter(delay);
    }
    return delay;
  }

  private Duration applyJitter(Duration delay) {
    long millis = delay.toMillis();
    if (millis <= 1) {
      return delay;
    }
    long jittered = ThreadLocalRandom.current().nextLong(millis / 2, millis);
    return Duration.ofMillis(jittered);
  }
}
