package org.anthills.api;

import java.time.Instant;

public record SubmissionOptions(
  Instant availableAfter,
  int maxAttempts
) {
  public static SubmissionOptions defaults() {
    return new SubmissionOptions(null, 5);
  }
}
