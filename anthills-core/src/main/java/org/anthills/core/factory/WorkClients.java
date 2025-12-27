package org.anthills.core.factory;

import org.anthills.api.codec.PayloadCodec;
import org.anthills.api.work.WorkClient;
import org.anthills.api.work.WorkStore;
import org.anthills.core.JsonPayloadCodec;
import org.anthills.core.work.DefaultWorkClient;

public final class WorkClients {

  private WorkClients() {}

  public static WorkClient create(WorkStore store, PayloadCodec codec) {
    return new DefaultWorkClient(store, codec);
  }

  public static WorkClient create(WorkStore store) {
    return create(store, JsonPayloadCodec.defaultInstance());
  }
}
