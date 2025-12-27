package org.anthills.core.factory;

import org.anthills.api.codec.PayloadCodec;
import org.anthills.api.work.ProcessorConfig;
import org.anthills.api.work.WorkRequestProcessor;
import org.anthills.api.work.WorkStore;
import org.anthills.core.work.DefaultWorkRequestProcessor;

public final class WorkRequestProcessors {

  private WorkRequestProcessors() {}

  public static WorkRequestProcessor create(String workType, WorkStore store, PayloadCodec codec, ProcessorConfig config) {
    return new DefaultWorkRequestProcessor(workType, store, codec, config);
  }
}
