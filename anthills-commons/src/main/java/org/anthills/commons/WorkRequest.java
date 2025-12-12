package org.anthills.commons;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

public record WorkRequest<T>(
  String id,
  String payloadClass,
  T payload,
  Status status,
  String details,
  int maxRetries,
  String owner,
  Instant leaseUntil,
  Instant createdTs,
  Instant updatedTs,
  Instant startedTs,
  Instant completedTs
) {

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public enum Status {
    New(false),
    InProgress(false),
    Paused(false),
    Cancelled(true),
    Failed(true),
    Succeeded(true);

    private final boolean isTerminal;

    Status(boolean isTerminal) {
      this.isTerminal = isTerminal;
    }

    public boolean isTerminal() {
      return isTerminal;
    }

    public Set<Status> nonTerminalStatuses() {
      EnumSet<Status> statuses = EnumSet.noneOf(Status.class);
      for (Status status : Status.values()) {
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
    private Status status;
    private String details;
    private int maxRetries;
    private String owner;
    private Instant leaseUntil;
    private Instant createdTs;
    private Instant updatedTs;
    private Instant startedTs;
    private Instant completedTs;

    public Builder<T> setId(String id) {
      this.id = id;
      return this;
    }

    public Builder<T> setPayload(T payload) {
      this.payload = payload;
      return this;
    }

    public Builder<T> setStatus(Status status) {
      this.status = status;
      return this;
    }

    public Builder<T> setDetails(String details) {
      this.details = details;
      return this;
    }

    public Builder<T> setCreatedTs(Instant createdTs) {
      this.createdTs = createdTs;
      return this;
    }

    public Builder<T> setUpdatedTs(Instant updatedTs) {
      this.updatedTs = updatedTs;
      return this;
    }

    public Builder<T> setStartedTs(Instant startedTs) {
      this.startedTs = startedTs;
      return this;
    }

    public Builder<T> setCompletedTs(Instant completedTs) {
      this.completedTs = completedTs;
      return this;
    }

    public Builder<T> setMaxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder<T> setOwner(String owner) {
      this.owner = owner;
      return this;
    }

    public Builder<T> setLeaseUntil(Instant leaseUntil) {
      this.leaseUntil = leaseUntil;
      return this;
    }

    public Builder<T> setPayloadClass(Class<T> payloadClass) {
      this.payloadClass = payloadClass.getName();
      return this;
    }

    public WorkRequest<T> build() {
      return new WorkRequest<T>(id, payloadClass, payload, status, details, maxRetries, owner, leaseUntil, createdTs, updatedTs, startedTs, completedTs);
    }
  }
}
