package org.anthills.api;

public class CodecException extends RuntimeException {
  public CodecException(String message) {
    super(message);
  }

  public CodecException(String message, Throwable cause) {}
}
