package org.anthills.api.codec;

/**
 * Unchecked exception thrown when a payload cannot be encoded or decoded
 * by a {@link org.anthills.api.codec.PayloadCodec}.
 * Typical causes include schema/version mismatches, unsupported data types,
 * malformed input, or mapping failures.
 */
public class CodecException extends RuntimeException {

  /**
   * Creates an exception with a descriptive message.
   *
   * @param message detail explaining the encoding/decoding failure
   */
  public CodecException(String message) {
    super(message);
  }

  /**
   * Creates an exception with a descriptive message and the underlying cause.
   *
   * @param message detail explaining the encoding/decoding failure
   * @param cause the original exception that triggered this failure
   */
  public CodecException(String message, Throwable cause) {
    super(message, cause);
  }
}
