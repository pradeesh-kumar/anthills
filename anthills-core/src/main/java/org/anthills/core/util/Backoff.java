package org.anthills.core.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Computes retry delays using fixed or exponential strategies, with optional jitter.
 *
 * Typical usage:
 * - Use {@link #fixed(Duration)} for constant delay between attempts.
 * - Use {@link #exponential(Duration, Duration, boolean)} to back off exponentially up to a maximum,
 *   optionally applying jitter to reduce thundering herd effects.
 *
 * All durations are treated as positive and validated at construction.
 */
public class Backoff {

  /**
   * Supported backoff strategies.
   */
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

  /**
   * Creates a fixed backoff with the same delay between every attempt.
   *
   * @param delay constant delay; must be positive
   * @return a backoff instance using fixed delays
   */
  public static Backoff fixed(Duration delay) {
    return new Backoff(Strategy.FIXED, delay, delay, false);
  }

  /**
   * Creates an exponential backoff that doubles the delay each attempt (capped by {@code maxDelay}).
   *
   * Example growth: base, 2x, 4x, 8x, ... up to {@code maxDelay}. Uses a clamp to avoid overflow.
   *
   * @param baseDelay starting delay; must be > 0
   * @param maxDelay upper bound for delay; must be >= baseDelay
   * @param jitter if true, randomizes the final delay within a range to avoid synchronized retries
   * @return an exponential backoff instance
   * @throws IllegalArgumentException if arguments are invalid
   */
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
   * Calculates the delay before the next attempt.
   *
   * For FIXED: returns the configured base delay.
   * For EXPONENTIAL: returns {@code baseDelay * 2^(attempt-1)} clamped to {@code maxDelay}.
   * If {@code jitter} is enabled, the computed delay is randomized to reduce synchronization.
   *
   * @param attempt attempt number (1-based); must be >= 1
   * @return delay to wait before the next attempt
   * @throws IllegalArgumentException if {@code attempt < 1}
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

  /**
   * Applies "full jitter" by selecting a random value in [delay/2, delay).
   *
   * @param delay original computed delay
   * @return jittered delay
   */
  private Duration applyJitter(Duration delay) {
    long millis = delay.toMillis();
    if (millis <= 1) {
      return delay;
    }
    long jittered = ThreadLocalRandom.current().nextLong(millis / 2, millis);
    return Duration.ofMillis(jittered);
  }
}
