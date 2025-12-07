package org.anthills.core;

import org.anthills.commons.WorkRequest;

import java.util.UUID;
import java.util.function.Consumer;

public sealed class RequestWorker<T> permits LeasedRequestWorker {

  private final WorkerConfig config;
  private Consumer<WorkRequest<T>> wrConsumer;
  private final String identity;

  public RequestWorker(WorkerConfig config, Consumer<WorkRequest<T>> wrConsumer) {
    this.config = config;
    this.wrConsumer = wrConsumer;
    this.identity = UUID.randomUUID().toString();
  }

  public RequestWorker(Consumer<WorkRequest<T>> process) {
    this(WorkerConfig.defaultConfig(), process);
  }

  public String identity() {
    return this.identity;
  }
}
