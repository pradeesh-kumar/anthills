package org.anthills.core.work;

import org.anthills.api.codec.PayloadCodec;
import org.anthills.api.work.SubmissionOptions;
import org.anthills.api.work.WorkClient;
import org.anthills.api.work.WorkQuery;
import org.anthills.api.work.WorkRecord;
import org.anthills.api.work.WorkRequest;
import org.anthills.api.work.WorkStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default {@link WorkClient} implementation that persists requests via a {@link WorkStore}
 * and encodes/decodes payloads using a {@link org.anthills.api.codec.PayloadCodec}.
 */
public class DefaultWorkClient implements WorkClient {

  private final WorkStore store;
  private final PayloadCodec codec;

  /**
   * Creates a client that uses the given store and codec.
   *
   * @param store persistence used to create and query work
   * @param codec codec used to serialize and deserialize payloads
   * @throws NullPointerException if any argument is null
   */
  public DefaultWorkClient(WorkStore store, PayloadCodec codec) {
    this.store = Objects.requireNonNull(store, "store is required");
    this.codec = Objects.requireNonNull(codec, "codec is required");
  }

  /**
   * Submits a new work request using {@link SubmissionOptions#defaults()}.
   *
   * @param workType routing key that determines which processor/handler will handle it
   * @param payload typed payload to be serialized and stored
   * @param <T> payload type
   * @return created request with decoded payload
   * @throws IllegalArgumentException if encoding fails
   * @throws NullPointerException if any argument is null
   */
  @Override
  public <T> WorkRequest<T> submit(String workType, T payload) {
    return submit(workType, payload, SubmissionOptions.defaults());
  }

  /**
   * Submits a new work request using the supplied options.
   *
   * @param workType routing key for dispatch
   * @param payload typed payload to be serialized and stored
   * @param options submission parameters (codec name, version, retry cap)
   * @param <T> payload type
   * @return created request with decoded payload
   * @throws IllegalArgumentException if encoding fails
   * @throws NullPointerException if any argument is null
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> WorkRequest<T> submit(String workType, T payload, SubmissionOptions options) {
    Objects.requireNonNull(workType, "workType");
    Objects.requireNonNull(payload, "payload");
    Objects.requireNonNull(options, "options");
    byte[] encodedPayload;
    try {
      encodedPayload = codec.encode(payload, options.payloadVersion());
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to encode payload of type " + payload.getClass().getName(), e);
    }
    WorkRecord record = store.createWork(workType, encodedPayload, options.payloadVersion(), options.codec(), options.maxAttempts());
    return (WorkRequest<T>) record.toWorkRequest(codec);
  }

  /**
   * Fetches a single work request by id and decodes its payload into the requested type.
   *
   * @param id work identifier
   * @param payloadType target class for payload
   * @param <T> payload type
   * @return present if found, otherwise empty
   * @throws IllegalArgumentException if decoding to the requested type fails
   * @throws NullPointerException if any argument is null
   */
  @Override
  public <T> Optional<WorkRequest<T>> get(String id, Class<T> payloadType) {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(payloadType, "payloadType is required");
    return store.getWork(id).map(workRecord -> workRecord.toWorkRequest(codec, payloadType));
  }

  /**
   * Lists work requests matching the query and decodes payloads as {@code Object}.
   *
   * @param query filter and paging parameters
   * @return matching requests (may be empty)
   * @throws NullPointerException if {@code query} is null
   */
  @Override
  public List<WorkRequest<?>> list(WorkQuery query) {
    Objects.requireNonNull(query, "query is required");
    return store.listWork(query)
      .stream()
      .map(record -> record.toWorkRequest(codec, Object.class))
      .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * Requests best-effort cancellation of a work request.
   *
   * @param workRequestId identifier of the request to cancel
   */
  @Override
  public void cancel(String workRequestId) {
    store.markCancelled(workRequestId);
  }
}
