package org.anthills.api;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface WorkStore {

  // ------------------------------------------------------------------
  // Work submission
  // ------------------------------------------------------------------

  <T> WorkRequest<T> createWork(
    String workType,
    byte[] payload,
    int payloadVersion,
    String codec,
    Integer maxRetries
  );

  // ------------------------------------------------------------------
  // Querying / inspection
  // ------------------------------------------------------------------

  Optional<WorkRequest<?>> getWork(String workId);

  List<WorkRequest<?>> listWork(
    WorkQuery query
  );

  // ---- RequestWorker operations ----
  <T> List<WorkRequest<T>> claimWork(
    String workerId,
    int batchSize,
    Duration leaseDuration,
    Class<T> payloadType
  );

  // ------------------------------------------------------------------
  // Leasing + claiming (CRITICAL)
  // ------------------------------------------------------------------

  /**
   * Atomically:
   *  - finds work requests of the given workType
   *  - that are eligible to run
   *  - and claims them by acquiring a lease
   *
   * This MUST be safe under concurrent callers.
   */
  List<WorkRequest<?>> claimWork(
    String workType,
    String ownerId,
    int limit,
    Duration leaseDuration
  );

  /**
   * Renew lease for an in-progress work request.
   * Must fail if ownerId does not match.
   */
  boolean renewLease(
    String workId,
    String ownerId,
    Duration leaseDuration
  );

  // ------------------------------------------------------------------
  // Completion & failure
  // ------------------------------------------------------------------

  void markSucceeded(
    String workId,
    String ownerId
  );

  void markFailed(
    String workId,
    String ownerId,
    String failureReason
  );

  void markCancelled(
    String workId
  );

  void markFailed(
    String workRequestId,
    String workerId,
    Throwable error
  );

  // ============================================================
  // Scheduler leases (LeasedScheduler)
  // ============================================================

  /**
   * Try to acquire a scheduler lease for a logical job name.
   */
  boolean tryAcquireSchedulerLease(
    String jobName,
    String ownerId,
    Duration leaseDuration
  );

  /**
   * Renew an existing scheduler lease.
   */
  boolean renewSchedulerLease(
    String jobName,
    String ownerId,
    Duration leaseDuration
  );

  /**
   * Release scheduler lease early (best-effort).
   */
  void releaseSchedulerLease(
    String jobName,
    String ownerId
  );
}
