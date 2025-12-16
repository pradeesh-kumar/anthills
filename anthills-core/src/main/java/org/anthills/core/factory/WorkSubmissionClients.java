package org.anthills.core.factory;

import org.anthills.api.WorkStore;
import org.anthills.api.WorkSubmissionClient;
import org.anthills.core.work.client.DefaultWorkSubmissionClient;

public final class WorkSubmissionClients {

  private WorkSubmissionClients() {}

  public static WorkSubmissionClient create(WorkStore store) {
    return new DefaultWorkSubmissionClient(store);
  }
}
