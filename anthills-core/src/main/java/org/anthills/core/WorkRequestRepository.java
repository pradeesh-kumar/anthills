package org.anthills.core;

import org.anthills.commons.WorkRequest;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface WorkRequestRepository {
  <T> WorkRequest<T> create(WorkRequest<T> worKRequest);
  boolean updateStatus(String id, WorkRequest.Status status);
  <T> Optional<WorkRequest<T>> findById(String id, Class<T> payloadClass);
  <T> List<WorkRequest<T>> findAllNonTerminal(Class<T> clazz, Page page);
  boolean exists(String id);
  boolean incrementAttempt(String id);
  boolean extendLease(String wrId, String owner, Duration leasePeriod);
}
