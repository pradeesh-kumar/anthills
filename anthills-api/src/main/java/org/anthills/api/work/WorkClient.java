package org.anthills.api.work;

import java.util.List;
import java.util.Optional;

/**
 * Client API for creating and quering {@link WorkRequest}.
 */
public interface WorkClient {

  /**
   * Creates a new work request with default {@link SubmissionOptions#defaults()}.
   *
   * @param workType logical routing key that identifies the kind of work/handler
   * @param payload typed payload to be processed
   * @param <T> payload type
   * @return the created {@link WorkRequest}
   * @throws IllegalArgumentException if inputs are invalid
   */
  <T> WorkRequest<T> submit(String workType, T payload);

  /**
   * Creates a new work request with explicit submission options.
   *
   * @param workType logical routing key that identifies the kind of work/handler
   * @param payload typed payload to be processed
   * @param options serialization/codec and retry options
   * @param <T> payload type
   * @return the created {@link WorkRequest}
   * @throws IllegalArgumentException if inputs are invalid
   */
  <T> WorkRequest<T> submit(String workType, T payload, SubmissionOptions options);

  /**
   * Fetches a work request by id and decodes its payload into the requested type.
   *
   * @param id work request identifier
   * @param payloadType target class for payload decoding
   * @param <T> payload type
   * @return present if found, otherwise empty
   * @throws IllegalArgumentException if the stored payload cannot be decoded to the requested type
   */
  <T> Optional<WorkRequest<T>> get(String id, Class<T> payloadType);

  /**
   * Lists work requests matching the supplied query.
   *
   * @param query filter and paging parameters
   * @return matching work requests (may be empty)
   */
  List<WorkRequest<?>> list(WorkQuery query);

  /**
   * Best-effort cancellation of a work request.
   * If the request is already completed or cancelled, this is a no-op.
   *
   * @param workRequestId identifier of the work request to cancel
   */
  void cancel(String workRequestId);
}
