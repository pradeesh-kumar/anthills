package org.anthills.api;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface WorkStore {

  // ------------------------------------------------------------------
  // WorkRequest Operations
  // ------------------------------------------------------------------
  <T> WorkRequest<T> createWork(String workType, byte[] payload, int payloadVersion, String codec, Integer maxRetries);
  Optional<WorkRequest<?>> getWork(String id);
  List<WorkRequest<?>> listWork(WorkQuery query);
  List<WorkRequest<?>> claimWork(String workType, String ownerId, int limit, Duration leaseDuration);
  boolean renewLease(String id, String ownerId, Duration leaseDuration);
  void markSucceeded(String id, String ownerId);
  void markFailed(String id, String ownerId, String failureReason);
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
