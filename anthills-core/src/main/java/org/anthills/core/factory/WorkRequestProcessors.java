package org.anthills.core.factory;

import org.anthills.api.codec.PayloadCodec;
import org.anthills.api.work.ProcessorConfig;
import org.anthills.api.work.WorkRequestProcessor;
import org.anthills.api.work.WorkStore;
import org.anthills.core.JsonPayloadCodec;
import org.anthills.core.work.DefaultWorkRequestProcessor;

/**
 * Factory utilities for creating {@link WorkRequestProcessor} instances.
 *
 * Use this when you need a ready-to-run processor for a single {@code workType}
 * that will poll a {@link WorkStore}, renew leases, and dispatch to handlers.
 */
public final class WorkRequestProcessors {

  private WorkRequestProcessors() {}

  /**
   * Creates a {@link WorkRequestProcessor} for the given {@code workType}.
   *
   * @param workType routing key the processor will handle
   * @param store persistence used to claim/renew/mark work
   * @param codec codec used to decode stored payloads
   * @param config tuning parameters (threads, polling, retries, leases)
   * @return a processor instance ready to {@link WorkRequestProcessor#start()}
   * @throws NullPointerException if any argument is null
   */
  public static WorkRequestProcessor create(String workType, WorkStore store, PayloadCodec codec, ProcessorConfig config) {
    return new DefaultWorkRequestProcessor(workType, store, codec, config);
  }

  /**
   * Creates a {@link WorkRequestProcessor} for the given {@code workType}. Uses JsonPayloadCodec as the default codec
   *
   * @param workType routing key the processor will handle
   * @param store persistence used to claim/renew/mark work
   * @param config tuning parameters (threads, polling, retries, leases)
   * @return a processor instance ready to {@link WorkRequestProcessor#start()}
   * @throws NullPointerException if any argument is null
   */
  public static WorkRequestProcessor create(String workType, WorkStore store, ProcessorConfig config) {
    return create(workType, store, JsonPayloadCodec.defaultInstance(), config);
  }
}
