package org.anthills.core.factory;

import org.anthills.api.codec.PayloadCodec;
import org.anthills.api.work.WorkClient;
import org.anthills.api.work.WorkStore;
import org.anthills.core.work.DefaultWorkClient;

public final class WorkClients {

  private WorkClients() {}

  public static WorkClient create(WorkStore store, PayloadCodec codec) {
    return new DefaultWorkClient(store, codec);
  }
}
