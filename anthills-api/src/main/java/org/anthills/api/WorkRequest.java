package org.anthills.api;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

public record WorkRequest<T>(
  String id,

  // Routing
  String workType,

  // Payload
  T payload,
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
  Instant completedTs
) {

  public enum Status {
    NEW(false),
    IN_PROGRESS(false),
    PAUSED(false),
    CANCELLED(true),
    FAILED(true),
    SUCCEEDED(true);

    private final boolean isTerminal;

    Status(boolean isTerminal) {
      this.isTerminal = isTerminal;
    }

    public boolean isTerminal() {
      return isTerminal;
    }

    public static Set<WorkRequest.Status> nonTerminalStatuses() {
      EnumSet<WorkRequest.Status> statuses = EnumSet.noneOf(WorkRequest.Status.class);
      for (WorkRequest.Status status : WorkRequest.Status.values()) {
        if (!status.isTerminal()) {
          statuses.add(status);
        }
      }
      return statuses;
    }
  }

  public static <T> WorkRequest.Builder<T> builder() {
    return new WorkRequest.Builder<>();
  }

  public static class Builder<T> {
    private String id;
    private String workType;
    private T payload;
    private int payloadVersion;
    private String codec;
    private WorkRequest.Status status;
    private Integer maxRetries;
    private int attemptCount; // number of executions so far
    private String ownerId;
    private Instant leaseUntil;
    private String failureReason;
    private Instant createdTs;
    private Instant updatedTs;
    private Instant startedTs;
    private Instant completedTs;

    public Builder<T> id(String id) {
      this.id = id;
      return this;
    }

    public Builder<T> workType(String workType) {
      this.workType = workType;
      return this;
    }

    public Builder<T> payload(T payload) {
      this.payload = payload;
      return this;
    }

    public Builder<T> payloadVersion(int payloadVersion) {
      this.payloadVersion = payloadVersion;
      return this;
    }

    public Builder<T> codec(String codec) {
      this.codec = codec;
      return this;
    }

    public Builder<T> status(WorkRequest.Status status) {
      this.status = status;
      return this;
    }

    public Builder<T> status(String status) {
      this.status = WorkRequest.Status.valueOf(status);
      return this;
    }

    public Builder<T> maxRetries(Integer maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder<T> attemptCount(int attemptCount) {
      this.attemptCount = attemptCount;
      return this;
    }

    public Builder<T> ownerId(String ownerId) {
      this.ownerId = ownerId;
      return this;
    }

    public Builder<T> leaseUntil(Instant leaseUntil) {
      this.leaseUntil = leaseUntil;
      return this;
    }

    public Builder<T> failureReason(String failureReason) {
      this.failureReason = failureReason;
      return this;
    }

    public Builder<T> createdTs(Instant createdTs) {
      this.createdTs = createdTs;
      return this;
    }

    public Builder<T> updatedTs(Instant updatedTs) {
      this.updatedTs = updatedTs;
      return this;
    }

    public Builder<T> startedTs(Instant startedTs) {
      this.startedTs = startedTs;
      return this;
    }

    public Builder<T> completedTs(Instant completedTs) {
      this.completedTs = completedTs;
      return this;
    }

    public WorkRequest<T> build() {
      return new WorkRequest<>(id, workType, payload, payloadVersion, codec, status, maxRetries, attemptCount, ownerId, leaseUntil, failureReason, createdTs, updatedTs, startedTs, completedTs);
    }
  }
}
