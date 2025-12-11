package org.anthills.core;

import org.anthills.commons.WorkRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Consumer;

public sealed class RequestWorker<T> implements Worker permits LeasedRequestWorker {

  private static final Logger log =  LoggerFactory.getLogger(RequestWorker.class);

  private final WorkerConfig config;
  private Consumer<WorkRequest<T>> wrConsumer;
  private final String identity;

  RequestWorker(WorkerConfig config, Consumer<WorkRequest<T>> wrConsumer) {
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

  @Override
  public void start() {
    throw new UnsupportedOperationException("start() is not implemented yet");
  }

  @Override
  public void awaitTermination() {
    throw new UnsupportedOperationException("start() is not implemented yet");
  }
}
