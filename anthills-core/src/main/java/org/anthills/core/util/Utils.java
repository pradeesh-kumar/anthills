package org.anthills.core.util;

import java.time.Duration;
import java.util.Collection;

public class Utils {

  public static boolean isEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  public static Duration min(Duration a, Duration b) {
    return a.compareTo(b) <= 0 ? a : b;
  }
}
