package org.anthills.core.util;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ClockProvider {

  private static final AtomicReference<Supplier<Instant>> clock = new AtomicReference<>(Instant::now);

  private ClockProvider() {}

  /**
   * Returns the current time.
   */
  public static Instant now() {
    return clock.get().get();
  }

  /**
   * Override the clock (typically for tests).
   */
  public static void set(Supplier<Instant> supplier) {
    clock.set(Objects.requireNonNull(supplier, "supplier"));
  }

  /**
   * Reset clock back to system time.
   */
  public static void reset() {
    clock.set(Instant::now);
  }
}
