package org.anthills.api;

import java.time.Instant;

public record WorkRecord(
  String id,

  // Routing
  String workType,

  // Payload
  byte[] payload,
  String payloadType,
  int payloadVersion,
  String codec,

  // Execution
  WorkRequest.Status status,
  // total allowed executions. null -> use processor default value. non-null -> min(request.maxRetries, processor.maxAllowedRetries)
  Integer maxRetries,
  int attemptCount, // number of executions so far
  String ownerId,
  Instant leaseUntil,

  // Short human-readable error; may include truncated stack trace
  String failureReason, // optional, stack trace

  // Timestamps
  Instant createdTs,
  Instant updatedTs,
  Instant startedTs,
  Instant completedTs) {

  public <T> WorkRequest<T> toWorkRequest(
    Class<T> payloadType,
    PayloadCodec codec
  ) {
    T decoded;
    try {
      decoded = codec.decode(this.payload(), payloadType, this.payloadVersion);
    } catch (Exception e) {
      throw new IllegalStateException(
        "Failed to decode payload for workId=" + id(),
        e
      );
    }

    return new WorkRequest<>(
      id(),
      workType(),
      decoded,
      payloadVersion(),
      this.codec(),
      status(),
      attemptCount(),
      maxRetries(),
      ownerId(),
      leaseUntil(),
      failureReason(),
      createdTs(),
      updatedTs(),
      startedTs(),
      completedTs()
    );
  }
}
