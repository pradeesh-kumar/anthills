package org.anthills.api;

import java.time.Instant;

public record ScheduledJob(
  String id,
  String name,

  Schedule schedule,
  JobStatus status,

  Instant nextExecutionTs,
  Instant lastExecutionTs,
  ExecutionStatus lastExecutionStatus,
  String lastFailureError,
  String ownerId,
  Instant leaseUntil,

  Instant createdTs,
  Instant updatedTs
) {
  public enum JobStatus {
    ACTIVE,
    INACTIVE
  }

  public enum ExecutionStatus {
    SUCCEEDED,
    FAILED,
    CANCELLED
  }
}
