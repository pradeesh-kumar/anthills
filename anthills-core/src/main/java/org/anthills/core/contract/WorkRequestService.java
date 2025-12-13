package org.anthills.core.contract;

import org.anthills.commons.WorkRequest;

import java.util.Optional;

public interface WorkRequestService {
  WorkRequest<?> create(WorkRequest<?> wr);
  WorkRequest<?> update(WorkRequest<?> wr);
  boolean exists(String id);
  Optional<WorkRequest<?>> findById(String id, Class<?> payloadClass);
}
