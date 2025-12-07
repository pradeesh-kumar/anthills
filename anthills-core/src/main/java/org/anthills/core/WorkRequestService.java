package org.anthills.core;

import org.anthills.commons.WorkRequest;

public interface WorkRequestService {
  <T> WorkRequest<T> create(WorkRequest<T> workRequest);
}
