package org.anthills.core.work.processor;

import org.anthills.api.WorkHandler;
import org.anthills.api.WorkProcessorConfig;
import org.anthills.api.WorkRequestProcessor;
import org.anthills.api.WorkStore;

public class DefaultWorkRequestProcessor implements WorkRequestProcessor {

  private final String workType;
  private final WorkProcessorConfig config;
  private final WorkStore workStore;

  public DefaultWorkRequestProcessor(String workType, WorkProcessorConfig config, WorkStore workStore) {
    this.workType = workType;
    this.config = config;
    this.workStore = workStore;
  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {

  }

  @Override
  public void awaitTermination() throws InterruptedException {

  }

  @Override
  public <T> void registerHandler(String workType, Class<T> payloadType, WorkHandler<T> handler) {

  }
}
