package org.anthills.api.work;

/**
 * Business logic handler for a specific work type.
 *
 * @param <T> typed payload associated with the {@link WorkRequest}
 */
@FunctionalInterface
public interface WorkHandler<T> {

  /**
   *
   * Processes a single work request.
   * <p>
   * Implementations may throw to signal failure; the processor decides
   * whether to retry based on configured policies and {@link WorkRequest#maxRetries()}.
   *
   * @param request the request to process, including decoded payload and metadata
   * @throws Exception if processing fails
   */
  void handle(WorkRequest<T> request) throws Exception;
}
