package org.anthills.core.contract;

import java.util.Optional;

public interface WorkRequestService {
  <T> WorkRequest<T> create(T payload, int maxRetries);
  void markSucceeded(WorkRequest<?> wr);
  void markFailedOrRetry(WorkRequest<?> wr);
  boolean exists(String id);
  <T> Optional<WorkRequest<T>> findById(String id, Class<T> payloadClass);
}
