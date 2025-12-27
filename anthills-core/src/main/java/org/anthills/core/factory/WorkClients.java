package org.anthills.core.factory;

import org.anthills.api.codec.PayloadCodec;
import org.anthills.api.work.WorkClient;
import org.anthills.api.work.WorkStore;
import org.anthills.core.JsonPayloadCodec;
import org.anthills.core.work.DefaultWorkClient;

/**
 * Factory utilities for creating {@link WorkClient}
 */
public final class WorkClients {

  private WorkClients() {}

  /**
   * Creates a {@link WorkClient} that persists work to the given {@link WorkStore} and
   * encodes payloads using the supplied {@link PayloadCodec}.
   *
   * @param store persistence for creating and querying work
   * @param codec payload serializer/deserializer
   * @return a client bound to the provided store and codec
   * @throws NullPointerException if any argument is null
   */
  public static WorkClient create(WorkStore store, PayloadCodec codec) {
    return new DefaultWorkClient(store, codec);
  }

  /**
   * Creates a {@link WorkClient} using the default JSON codec.
   *
   * @param store persistence for creating and querying work
   * @return a client using {@link org.anthills.core.JsonPayloadCodec#defaultInstance()}
   */
  public static WorkClient create(WorkStore store) {
    return create(store, JsonPayloadCodec.defaultInstance());
  }
}
