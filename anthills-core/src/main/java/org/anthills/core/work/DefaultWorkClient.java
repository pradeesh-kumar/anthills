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

public class DefaultWorkClient implements WorkClient {

  private final WorkStore store;
  private final PayloadCodec codec;

  public DefaultWorkClient(WorkStore store, PayloadCodec codec) {
    this.store = Objects.requireNonNull(store, "store is required");
    this.codec = Objects.requireNonNull(codec, "codec is required");
  }

  @Override
  public <T> WorkRequest<T> submit(String workType, T payload) {
    return submit(workType, payload, SubmissionOptions.defaults());
  }

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

  @Override
  public <T> Optional<WorkRequest<T>> get(String id, Class<T> payloadType) {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(payloadType, "payloadType is required");
    return store.getWork(id).map(workRecord -> workRecord.toWorkRequest(codec, payloadType));
  }

  @Override
  public List<WorkRequest<?>> list(WorkQuery query) {
    Objects.requireNonNull(query, "query is required");
    return store.listWork(query)
      .stream()
      .map(record -> record.toWorkRequest(codec, Object.class))
      .collect(Collectors.toCollection(ArrayList::new));
  }

  @Override
  public void cancel(String workRequestId) {
    store.markCancelled(workRequestId);
  }
}
