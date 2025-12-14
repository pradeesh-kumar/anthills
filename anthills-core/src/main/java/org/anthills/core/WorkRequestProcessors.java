package org.anthills.core;

import org.anthills.api.WorkRequestHandler;
import org.anthills.api.WorkRequestProcessor;
import org.anthills.api.config.WorkProcessorConfig;
import org.anthills.api.WorkStore;

public final class WorkRequestProcessors {

  private WorkRequestProcessors() {}

  public static <T> WorkRequestProcessor<T> create(
    Class<T> payloadType,
    WorkProcessorConfig config,
    WorkStore store,
    WorkRequestHandler<T> handler
  ) {
    return new DefaultWorkRequestProcessor<>();
  }
}
