package org.anthills.core.work.client;

import org.anthills.api.SubmissionOptions;
import org.anthills.api.WorkRequest;
import org.anthills.api.WorkStore;
import org.anthills.api.WorkSubmissionClient;

public class DefaultWorkSubmissionClient implements WorkSubmissionClient {

  private final WorkStore workStore;

  public DefaultWorkSubmissionClient(WorkStore workStore) {
    this.workStore = workStore;
  }

  @Override
  public <T> WorkRequest<T> submit(T payload) {
    return null;
  }

  @Override
  public <T> WorkRequest<T> submit(T payload, SubmissionOptions options) {
    return null;
  }
}
