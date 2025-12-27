package org.anthills.api.work;

/**
 * Coordinates the processing of submitted {@link WorkRequest}s by dispatching them
 * to registered {@link WorkHandler handlers} based on the request's {@code workType}.
 * Implementations are typically long-running {@link Worker}s that poll a {@link WorkStore}.
 */
public interface WorkRequestProcessor extends Worker {

  /**
   * Registers or replaces a handler for a specific {@code workType}.
   *
   * @param workType routing key identifying the kind of work
   * @param payloadType concrete class of the payload the handler expects
   * @param handler business logic that processes the work request
   * @param <T> payload type
   * @throws IllegalArgumentException if any argument is invalid
   */
  <T> void registerHandler(String workType, Class<T> payloadType, WorkHandler<T> handler);
}
