package org.anthills.core;

import org.anthills.commons.WorkRequest;

import java.util.Optional;
import java.util.stream.Stream;

public interface WorkRequestRepository {
  <T> WorkRequest<T> create(WorkRequest<T> worKRequest);
  <T> WorkRequest<T> update(WorkRequest<T> worKRequest);
  <T> Optional<WorkRequest<T>> findById(String id);
  <T> Stream<WorkRequest<T>> findAllNonTerminal(Class<T> clazz);
}
