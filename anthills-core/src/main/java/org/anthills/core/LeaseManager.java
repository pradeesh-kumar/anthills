package org.anthills.core;

import java.time.Duration;

public interface LeaseManager {
  boolean acquire(String owner, String object, Duration period);
  boolean extend(String owner, String object, Duration period);
  void release(String owner, String object);
}
