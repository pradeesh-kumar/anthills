package org.anthills.api.work;

/**
 * Options that influence how a work request is submitted and processed.
 *
 * @param payloadVersion semantic version of the payload schema used by the {@code codec}
 * @param codec name of the {@link org.anthills.api.codec.PayloadCodec} to use (e.g., "json")
 * @param maxAttempts optional cap on how many times the request may be attempted;
 *                    if {@code null}, the processor's default/max policy is applied
 */
public record SubmissionOptions(
  int payloadVersion,
  String codec,
  Integer maxAttempts
) {

  /**
   * Returns a sensible default submission configuration:
   * payloadVersion=1, codec="json", maxAttempts unbounded (delegate to processor defaults).
   *
   * @return default submission options
   */
  public static SubmissionOptions defaults() {
    return new SubmissionOptions(1, "json", null);
  }
}
