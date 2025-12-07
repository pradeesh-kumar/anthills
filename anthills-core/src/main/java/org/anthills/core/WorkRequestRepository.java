package org.anthills.core;

import org.anthills.commons.WorkRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface WorkRequestRepository {
  <T> WorkRequest<T> create(WorkRequest<T> worKRequest);
  <T> WorkRequest<T> update(WorkRequest<T> worKRequest);
  <T> void updateStatus(WorkRequest<T> worKRequest, WorkRequest.Status status);
  <T> Optional<WorkRequest<T>> findById(String id, Class<T> payloadClass);
  <T> List<WorkRequest<T>> findAllNonTerminal(Class<T> clazz, Page page);
}
