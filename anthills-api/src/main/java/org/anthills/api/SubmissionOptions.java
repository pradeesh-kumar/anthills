package org.anthills.api;

public record SubmissionOptions(
  String workType,
  int maxAttempts
) {

  public static SubmissionOptions defaults(String workType) {
    return new SubmissionOptions(workType, 3);
  }
}
