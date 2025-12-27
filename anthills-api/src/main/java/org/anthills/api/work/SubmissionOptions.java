package org.anthills.api.work;

public record SubmissionOptions(
  int payloadVersion,
  String codec,
  Integer maxAttempts
) {

  public static SubmissionOptions defaults() {
    return new SubmissionOptions(1, "json", null);
  }
}
