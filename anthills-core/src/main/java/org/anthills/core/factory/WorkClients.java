package org.anthills.core.factory;

import org.anthills.api.PayloadCodec;
import org.anthills.api.WorkClient;
import org.anthills.api.WorkStore;
import org.anthills.core.work.DefaultWorkClient;

public final class WorkClients {

  private WorkClients() {}

  public static WorkClient create(WorkStore store, PayloadCodec codec) {
    return new DefaultWorkClient(store, codec);
  }
}
