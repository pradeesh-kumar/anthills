package org.anthills.api;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

public record WorkRequest<T>(
  String id,
  String payloadClass,
  T payload,
  WorkRequest.Status status,
  String details,
  int maxRetries,
  int attempts,
  String owner,
  Instant leaseUntil,
  Instant createdTs,
  Instant updatedTs,
  Instant startedTs,
  Instant completedTs
) {
  public static <T> WorkRequest.Builder<T> builder() {
    return new WorkRequest.Builder<>();
  }

  public WorkRequest.Builder<T> toBuilder() {
    return new WorkRequest.Builder<T>()
      .setId(this.id)
      .setPayloadClass(this.payloadClass)
      .setPayload(this.payload)
      .setStatus(this.status)
      .setDetails(this.details)
      .setMaxRetries(this.maxRetries)
      .setAttempts(this.attempts)
      .setOwner(this.owner)
      .setLeaseUntil(this.leaseUntil)
      .setCreatedTs(this.createdTs)
      .setUpdatedTs(this.updatedTs)
      .setStartedTs(this.startedTs)
      .setCompletedTs(this.completedTs);
  }

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

  public static class Builder<T> {
    private String id;
    private String payloadClass;
    private T payload;
    private WorkRequest.Status status;
    private String details;
    private int maxRetries;
    private int attempts;
    private String owner;
    private Instant leaseUntil;
    private Instant createdTs;
    private Instant updatedTs;
    private Instant startedTs;
    private Instant completedTs;

    public WorkRequest.Builder<T> setId(String id) {
      this.id = id;
      return this;
    }

    public WorkRequest.Builder<T> setPayload(T payload) {
      this.payload = payload;
      return this;
    }

    public WorkRequest.Builder<T> setStatus(WorkRequest.Status status) {
      this.status = status;
      return this;
    }

    public WorkRequest.Builder<T> setDetails(String details) {
      this.details = details;
      return this;
    }

    public WorkRequest.Builder<T> setCreatedTs(Instant createdTs) {
      this.createdTs = createdTs;
      return this;
    }

    public WorkRequest.Builder<T> setUpdatedTs(Instant updatedTs) {
      this.updatedTs = updatedTs;
      return this;
    }

    public WorkRequest.Builder<T> setStartedTs(Instant startedTs) {
      this.startedTs = startedTs;
      return this;
    }

    public WorkRequest.Builder<T> setCompletedTs(Instant completedTs) {
      this.completedTs = completedTs;
      return this;
    }

    public WorkRequest.Builder<T> setMaxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public WorkRequest.Builder<T> setAttempts(int attempts) {
      this.attempts = attempts;
      return this;
    }

    public WorkRequest.Builder<T> setOwner(String owner) {
      this.owner = owner;
      return this;
    }

    public WorkRequest.Builder<T> setLeaseUntil(Instant leaseUntil) {
      this.leaseUntil = leaseUntil;
      return this;
    }

    public WorkRequest.Builder<T> setPayloadClass(String payloadClass) {
      this.payloadClass = payloadClass;
      return this;
    }

    public WorkRequest<T> build() {
      return new WorkRequest<>(id, payloadClass, payload, status, details, maxRetries, attempts, owner, leaseUntil, createdTs, updatedTs, startedTs, completedTs);
    }
  }
}
