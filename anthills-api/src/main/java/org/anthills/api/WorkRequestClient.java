package org.anthills.api;

import java.util.List;
import java.util.Optional;

public interface WorkRequestClient {
  Optional<WorkRequest<?>> get(String workRequestId);
  List<WorkRequest<?>> list(WorkQuery query);
  WorkRequest<?> cancel(String workRequestId);
}
