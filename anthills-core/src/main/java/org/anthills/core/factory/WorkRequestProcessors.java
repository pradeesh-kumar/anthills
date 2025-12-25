package org.anthills.core.factory;

import org.anthills.api.PayloadCodec;
import org.anthills.api.ProcessorConfig;
import org.anthills.api.WorkRequestProcessor;
import org.anthills.api.WorkStore;
import org.anthills.core.work.processor.DefaultWorkRequestProcessor;

public final class WorkRequestProcessors {

  private WorkRequestProcessors() {}

  public static WorkRequestProcessor create(String workType, WorkStore store, PayloadCodec codec, ProcessorConfig config) {
    return new DefaultWorkRequestProcessor(workType, store, codec, config);
  }
}
