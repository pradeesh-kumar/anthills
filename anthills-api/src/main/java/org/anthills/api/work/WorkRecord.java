package org.anthills.api.work;

import org.anthills.api.codec.PayloadCodec;

import java.time.Instant;

/**
 * Binary representation of a stored work request as persisted in a {@link WorkStore}.
 * It contains serialized payload bytes plus metadata and can be converted to a typed
 * {@link WorkRequest} using a {@link org.anthills.api.codec.PayloadCodec}.
 *
 * Record components:
 * - id: unique identifier
 * - workType: routing key
 * - payload: serialized bytes
 * - payloadType: fully qualified class name of the original payload
 * - payloadVersion: semantic schema version used by the codec
 * - codec: name of the codec used to serialize the payload
 * - status: lifecycle state
 * - maxRetries: optional cap on attempts
 * - attemptCount: number of attempts so far
 * - ownerId: current lease owner
 * - leaseUntil: lease expiration
 * - failureReason: brief description/truncated stack trace
 * - createdTs/updatedTs/startedTs/completedTs: lifecycle timestamps
 *
 * @param id unique identifier
 * @param workType logical routing key
 * @param payload serialized payload bytes
 * @param payloadType fully qualified class name of the payload
 * @param payloadVersion payload schema version
 * @param codec name of the codec used
 * @param status lifecycle status
 * @param maxRetries optional cap on allowed attempts
 * @param attemptCount number of attempts so far
 * @param ownerId logical owner currently holding the lease
 * @param leaseUntil lease expiration timestamp
 * @param failureReason brief description/truncated stack trace for failures
 * @param createdTs creation time
 * @param updatedTs last update time
 * @param startedTs first start time
 * @param completedTs completion time
 */
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

  /**
   * Decodes the stored payload to its declared {@code payloadType} using the provided codec.
   *
   * @param codec payload codec to use for decoding
   * @return a typed {@link WorkRequest} with the decoded payload
   * @throws IllegalStateException if the payload type cannot be loaded or decoding fails
   * @throws IllegalArgumentException if the codec rejects the payload
   */
  public WorkRequest<?> toWorkRequest(PayloadCodec codec) {
    Object decoded;
    try {
      Class<?> actualPayloadType = Class.forName(this.payloadType);
      decoded = codec.decode(this.payload(), actualPayloadType, this.payloadVersion);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Failed to deserialize WorkRequest " + this.id + " Class not found " + this.payloadType);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to decode payload for workId=" + this.id, e);
    }

    return WorkRequest.builder()
      .id(id)

      .workType(workType)

      .payload(decoded)
      .payloadVersion(payloadVersion)
      .codec(this.codec)

      .status(status)

      .attemptCount(attemptCount)
      .maxRetries(maxRetries)
      .ownerId(ownerId)
      .leaseUntil(leaseUntil)

      .failureReason(failureReason)

      .createdTs(createdTs)
      .updatedTs(updatedTs)
      .startedTs(startedTs)
      .completedTs(completedTs)
      .build();
  }

  /**
   * Decodes the stored payload to the requested {@code expectedPayloadType} using the provided codec.
   *
   * @param codec payload codec to use for decoding
   * @param expectedPayloadType target payload class; must be assignable from the stored {@code payloadType}
   * @param <T> typed payload class
   * @return a typed {@link WorkRequest} with the decoded payload
   * @throws IllegalArgumentException if the stored payload type is incompatible with {@code expectedPayloadType}
   * @throws IllegalStateException if the payload type cannot be loaded or decoding fails
   */
  @SuppressWarnings("unchecked")
  public <T> WorkRequest<T> toWorkRequest(
    PayloadCodec codec,
    Class<T> expectedPayloadType
  ) {
    T decoded;
    try {
      Class<?> actualPayloadType = Class.forName(this.payloadType);
      if (!actualPayloadType.isAssignableFrom(expectedPayloadType)) {
        throw new IllegalArgumentException("Cannot deserialize WorkRequest " + this.id + " to " + expectedPayloadType.getName() + ". Actual Type " + this.payloadType);
      }
      decoded = (T) codec.decode(this.payload(), actualPayloadType, this.payloadVersion);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Failed to deserialize WorkRequest " + this.id + " Class not found " + this.payloadType);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to decode payload for workId=" + this.id, e);
    }

    return WorkRequest.<T>builder()
      .id(id)

      .workType(workType)

      .payload(decoded)
      .payloadVersion(payloadVersion)
      .codec(this.codec)

      .status(status)

      .attemptCount(attemptCount)
      .maxRetries(maxRetries)
      .ownerId(ownerId)
      .leaseUntil(leaseUntil)

      .failureReason(failureReason)

      .createdTs(createdTs)
      .updatedTs(updatedTs)
      .startedTs(startedTs)
      .completedTs(completedTs)
      .build();
  }

  /**
   * Creates a new builder for {@link WorkRecord} instances.
   *
   * @return builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder pre-populated from this record.
   *
   * @return a builder initialized with this record's values
   */
  public Builder toBuilder() {
    return new Builder()
      .id(this.id())
      .workType(this.workType())
      .payload(this.payload())
      .payloadType(this.payloadType())
      .payloadVersion(this.payloadVersion())
      .codec(this.codec())
      .status(this.status())
      .maxRetries(this.maxRetries())
      .attemptCount(this.attemptCount())
      .ownerId(this.ownerId())
      .leaseUntil(this.leaseUntil())
      .failureReason(this.failureReason())
      .createdTs(this.createdTs())
      .updatedTs(this.updatedTs())
      .startedTs(this.startedTs())
      .completedTs(this.completedTs());
  }

  /**
   * Fluent builder for {@link WorkRecord} instances.
   *
   * Setters mirror the record components and return {@code this} for chaining.
   */
  public static final class Builder {
    private String id;

    // Routing
    private String workType;

    // Payload
    private byte[] payload;
    private String payloadType;
    private int payloadVersion;
    private String codec;

    // Execution
    private WorkRequest.Status status;
    private Integer maxRetries;
    private int attemptCount;
    private String ownerId;
    private Instant leaseUntil;

    // Error
    private String failureReason;

    // Timestamps
    private Instant createdTs;
    private Instant updatedTs;
    private Instant startedTs;
    private Instant completedTs;

    public Builder() {
    }

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder workType(String workType) {
      this.workType = workType;
      return this;
    }

    public Builder payload(byte[] payload) {
      this.payload = payload;
      return this;
    }

    public Builder payloadType(String payloadType) {
      this.payloadType = payloadType;
      return this;
    }

    public Builder payloadVersion(int payloadVersion) {
      this.payloadVersion = payloadVersion;
      return this;
    }

    public Builder codec(String codec) {
      this.codec = codec;
      return this;
    }

    public Builder status(WorkRequest.Status status) {
      this.status = status;
      return this;
    }

    public Builder status(String status) {
      this.status = WorkRequest.Status.valueOf(status);
      return this;
    }

    public Builder maxRetries(Integer maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder attemptCount(int attemptCount) {
      this.attemptCount = attemptCount;
      return this;
    }

    public Builder ownerId(String ownerId) {
      this.ownerId = ownerId;
      return this;
    }

    public Builder leaseUntil(Instant leaseUntil) {
      this.leaseUntil = leaseUntil;
      return this;
    }

    public Builder failureReason(String failureReason) {
      this.failureReason = failureReason;
      return this;
    }

    public Builder createdTs(Instant createdTs) {
      this.createdTs = createdTs;
      return this;
    }

    public Builder updatedTs(Instant updatedTs) {
      this.updatedTs = updatedTs;
      return this;
    }

    public Builder startedTs(Instant startedTs) {
      this.startedTs = startedTs;
      return this;
    }

    public Builder completedTs(Instant completedTs) {
      this.completedTs = completedTs;
      return this;
    }

    /**
     * Populates this builder with values from an existing {@link WorkRecord}.
     *
     * @param r source record
     * @return this builder
     */
    public Builder from(WorkRecord r) {
      this.id = r.id();
      this.workType = r.workType();
      this.payload = r.payload();
      this.payloadType = r.payloadType();
      this.payloadVersion = r.payloadVersion();
      this.codec = r.codec();
      this.status = r.status();
      this.maxRetries = r.maxRetries();
      this.attemptCount = r.attemptCount();
      this.ownerId = r.ownerId();
      this.leaseUntil = r.leaseUntil();
      this.failureReason = r.failureReason();
      this.createdTs = r.createdTs();
      this.updatedTs = r.updatedTs();
      this.startedTs = r.startedTs();
      this.completedTs = r.completedTs();
      return this;
    }

    /**
     * Builds an immutable {@link WorkRecord} instance from the configured values.
     *
     * @return new WorkRecord
     */
    public WorkRecord build() {
      return new WorkRecord(
        id,
        workType,
        payload,
        payloadType,
        payloadVersion,
        codec,
        status,
        maxRetries,
        attemptCount,
        ownerId,
        leaseUntil,
        failureReason,
        createdTs,
        updatedTs,
        startedTs,
        completedTs
      );
    }
  }
}
