package org.anthills.core;

import org.anthills.commons.WorkRequest;

import java.util.function.Consumer;

public non-sealed class LeasedRequestWorker<T> extends RequestWorker<T> {
  public LeasedRequestWorker(WorkerConfig config, Consumer<WorkRequest<T>> wrConsumer) {
    super(config, wrConsumer);
  }
}
