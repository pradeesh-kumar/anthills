package org.anthills.api.work;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Persistence abstraction for storing, querying and leasing work requests and scheduler leases.
 * Implementations must ensure atomicity of claim/renew/mark operations to support distributed workers.
 */
public interface WorkStore {

  // ------------------------------------------------------------------
  // WorkRequest Operations
  // ------------------------------------------------------------------
  /**
   * Persists a new unit of work.
   *
   * @param workType logical routing key
   * @param payload serialized payload bytes
   * @param payloadType class name of payload
   * @param payloadVersion semantic schema version for the payload
   * @param codec name of the codec used to serialize the payload
   * @param maxRetries optional cap on retry attempts; {@code null} to use processor defaults
   * @return the stored {@link WorkRecord}
   */
  WorkRecord createWork(String workType, byte[] payload, String payloadType, int payloadVersion, String codec, Integer maxRetries);

  /**
   * Fetches a single work item by id.
   *
   * @param id identifier of the work item
   * @return present if found, otherwise empty
   */
  Optional<WorkRecord> getWork(String id);

  /**
   * Lists work items matching the supplied {@link WorkQuery}.
   *
   * @param query filter and paging parameters
   * @return matching work records (may be empty)
   */
  List<WorkRecord> listWork(WorkQuery query);

  /**
   * Atomically claims up to {@code limit} items of type {@code workType} for the owner,
   * assigning a lease with the given duration. Implementations must ensure that items are
   * not simultaneously claimed by multiple owners.
   *
   * @param workType routing key
   * @param ownerId logical owner/worker identifier
   * @param limit maximum number of items to claim
   * @param leaseDuration lease length for each claimed item
   * @return claimed work records (size ≤ limit)
   */
  List<WorkRecord> claimWork(String workType, String ownerId, int limit, Duration leaseDuration);

  /**
   * Renews the worker lease for a claimed work item if owned by {@code ownerId}.
   *
   * @param id work id
   * @param ownerId expected owner
   * @param leaseDuration new lease duration from now
   * @return true if renewed, false if not owned/does not exist/expired
   */
  boolean renewWorkerLease(String id, String ownerId, Duration leaseDuration);

  /**
   * Reschedules a non-terminal work item to be retried after the given delay.
   *
   * @param id work id
   * @param delay delay before it becomes claimable again
   */
  void reschedule(String id, Duration delay);

  /**
   * Marks a claimed work item as succeeded.
   *
   * @param id work id
   * @param ownerId expected owner
   */
  void markSucceeded(String id, String ownerId);

  /**
   * Marks a claimed work item as failed with a human-readable reason.
   *
   * @param id work id
   * @param ownerId expected owner
   * @param failureReason brief description and/or truncated stack trace
   */
  void markFailed(String id, String ownerId, String failureReason);

  /**
   * Best-effort cancellation; if already terminal, this is a no-op.
   *
   * @param id work id
   */
  void markCancelled(String id);


  // ============================================================
  // Scheduler lease Operations (LeasedScheduler)
  // ============================================================
  /**
   * Try to acquire a scheduler lease for a logical job name.
   */
  boolean tryAcquireSchedulerLease(String jobName, String ownerId, Duration leaseDuration);

  /**
   * Renew an existing scheduler lease.
   */
  boolean renewSchedulerLease(String jobName, String ownerId, Duration leaseDuration);

  /**
   * Release scheduler lease early (best-effort).
   */
  void releaseSchedulerLease(String jobName, String ownerId);
}
