package org.anthills.api.work;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * Typed view of a unit of work submitted to the system.
 *
 * @param <T> decoded payload type
 *
 * Components:
 * - id: unique identifier
 * - workType: routing key that selects a handler
 * - payload: decoded payload of type T
 * - payloadVersion: semantic schema version used by the codec
 * - codec: name of the {@code PayloadCodec} used
 * - status: lifecycle state
 * - maxRetries: optional cap on attempts; if null, processor defaults apply
 * - attemptCount: number of attempts so far
 * - ownerId: logical owner/worker currently holding the lease
 * - leaseUntil: timestamp until which the lease is valid
 * - failureReason: brief description/truncated stack trace for failures
 * - createdTs/updatedTs/startedTs/completedTs: lifecycle timestamps
 */
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

  /**
   * Lifecycle states for a work request.
   * Non-terminal states may transition to other states; terminal states are final.
   */
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

    /**
     * Returns true if this status is terminal (no further processing occurs).
     */
    public boolean isTerminal() {
      return isTerminal;
    }

    /**
     * Convenience helper returning the set of non-terminal statuses.
     *
     * @return non-terminal statuses
     */
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

  /**
   * Creates a new builder for a typed {@link WorkRequest}.
   *
   * @param <T> payload type
   * @return builder instance
   */
  public static <T> WorkRequest.Builder<T> builder() {
    return new WorkRequest.Builder<>();
  }

  /**
   * Fluent builder for {@link WorkRequest} instances.
   * Setters mirror the record components and return {@code this} for chaining.
   *
   * @param <T> payload type
   */
  public static final class Builder<T> {
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

    /**
     * Builds an immutable {@link WorkRequest} instance from the configured values.
     *
     * @return a new WorkRequest
     */
    public WorkRequest<T> build() {
      return new WorkRequest<>(id, workType, payload, payloadVersion, codec, status, maxRetries, attemptCount, ownerId, leaseUntil, failureReason, createdTs, updatedTs, startedTs, completedTs);
    }
  }
}
