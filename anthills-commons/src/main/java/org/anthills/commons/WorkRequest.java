package org.anthills.commons;

import java.time.Instant;

public record WorkRequest<T>(
  String id,
  T payload,
  Status status,
  String details,
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
  }

  public static class Builder<T> {
    private String id;
    private T payload;
    private Status status;
    String details;
    Instant createdTs;
    Instant updatedTs;
    Instant startedTs;
    Instant completedTs;

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

    public WorkRequest<T> build() {
      return new WorkRequest<T>(id, payload, status, details, createdTs, updatedTs, startedTs, completedTs);
    }
  }
}
