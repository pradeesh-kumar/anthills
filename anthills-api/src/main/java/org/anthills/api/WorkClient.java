package org.anthills.api;

import java.util.List;
import java.util.Optional;

public interface WorkClient {
  <T> WorkRequest<T> submit(String workType, T payload);
  <T> WorkRequest<T> submit(String workType, T payload, SubmissionOptions options);
  <T> Optional<WorkRequest<T>> get(String id, Class<T> payloadType);
  List<WorkRequest<?>> list(WorkQuery query);
  void cancel(String workRequestId);
}
