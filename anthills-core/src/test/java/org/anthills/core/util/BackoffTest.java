package org.anthills.core.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class BackoffTest {

  @Test
  void fixedReturnsSameDelayForAnyAttempt() {
    Backoff b = Backoff.fixed(Duration.ofMillis(123));
    assertEquals(123, b.nextDelay(1).toMillis());
    assertEquals(123, b.nextDelay(2).toMillis());
    assertEquals(123, b.nextDelay(10).toMillis());
  }

  @Test
  void nextDelayAttemptMustBePositive() {
    Backoff b = Backoff.fixed(Duration.ofMillis(1));
    assertThrows(IllegalArgumentException.class, () -> b.nextDelay(0));
    assertThrows(IllegalArgumentException.class, () -> b.nextDelay(-5));
  }

  @Test
  void exponentialValidation_baseDelayMustBePositive() {
    assertThrows(IllegalArgumentException.class, () -> Backoff.exponential(Duration.ZERO, Duration.ofMillis(1), false));
    assertThrows(IllegalArgumentException.class, () -> Backoff.exponential(Duration.ofMillis(-1), Duration.ofMillis(1), false));
  }

  @Test
  void exponentialValidation_maxDelayAtLeastBase() {
    assertThrows(IllegalArgumentException.class, () -> Backoff.exponential(Duration.ofMillis(10), Duration.ofMillis(9), false));
  }

  @Test
  void exponentialNoJitterGrowsAndCaps() {
    Backoff b = Backoff.exponential(Duration.ofMillis(10), Duration.ofMillis(100), false);
    assertEquals(10, b.nextDelay(1).toMillis());
    assertEquals(20, b.nextDelay(2).toMillis());
    assertEquals(40, b.nextDelay(3).toMillis());
    assertEquals(80, b.nextDelay(4).toMillis());
    // cap at maxDelay
    assertEquals(100, b.nextDelay(5).toMillis());
    assertEquals(100, b.nextDelay(10).toMillis());
  }

  @RepeatedTest(5)
  void exponentialWithJitterWithinRange() {
    // base=100ms, attempt=3 -> 100 * 2^(3-1) = 400ms (pre-jitter)
    // jitter should yield [200, 400) ms
    Backoff b = Backoff.exponential(Duration.ofMillis(100), Duration.ofSeconds(5), true);
    long ms = b.nextDelay(3).toMillis();
    assertTrue(ms >= 200 && ms < 400, "jittered delay should be in [200, 400) ms, got " + ms);
  }

  @Test
  void exponentialWithJitterMillisLE1NoChange() {
    // millis <= 1 skips jitter; also capped by max
    Backoff b = Backoff.exponential(Duration.ofMillis(1), Duration.ofMillis(1), true);
    assertEquals(1, b.nextDelay(1).toMillis());
    assertEquals(1, b.nextDelay(2).toMillis()); // would grow to 2 but capped to 1; jitter should keep it unchanged
  }

  @Test
  void exponentialVeryLargeAttemptStillCapsToMax() {
    Backoff b = Backoff.exponential(Duration.ofMillis(1), Duration.ofMillis(1000), false);
    assertEquals(1000, b.nextDelay(1000).toMillis());
  }
}
