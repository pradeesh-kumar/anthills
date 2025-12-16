package org.anthills.core.work.client;

import org.anthills.api.WorkQuery;
import org.anthills.api.WorkRequest;
import org.anthills.api.WorkRequestClient;

import java.util.List;
import java.util.Optional;

public class DefaultWorkRequestClient implements WorkRequestClient {

  @Override
  public Optional<WorkRequest<?>> get(String workRequestId) {
    return Optional.empty();
  }

  @Override
  public List<WorkRequest<?>> list(WorkQuery query) {
    return List.of();
  }

  @Override
  public WorkRequest<?> cancel(String workRequestId) {
    return null;
  }
}
