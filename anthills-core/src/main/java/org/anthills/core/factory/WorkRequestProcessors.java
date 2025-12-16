package org.anthills.core.factory;

import org.anthills.api.WorkProcessorConfig;
import org.anthills.api.WorkRequestProcessor;
import org.anthills.api.WorkStore;
import org.anthills.core.work.processor.DefaultWorkRequestProcessor;

public final class WorkRequestProcessors {

  private WorkRequestProcessors() {}

  public static WorkRequestProcessor create(String workType, WorkProcessorConfig config, WorkStore store) {
    return new DefaultWorkRequestProcessor(workType, config, store);
  }
}
