package org.anthills.core.util;

import java.time.Duration;
import java.util.Collection;

/**
 * Small collection of utility helpers used across the core module.
 */
public class Utils {

  /**
   * Checks if a collection is {@code null} or has no elements.
   *
   * @param collection the collection to test (may be null)
   * @return true if the collection is null or empty
   */
  public static boolean isEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * Returns the smaller of two durations.
   *
   * @param a first duration
   * @param b second duration
   * @return {@code a} if {@code a <= b}, otherwise {@code b}
   */
  public static Duration min(Duration a, Duration b) {
    return a.compareTo(b) <= 0 ? a : b;
  }
}
