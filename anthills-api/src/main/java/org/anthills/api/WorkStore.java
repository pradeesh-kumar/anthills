package org.anthills.api;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface WorkStore {

  // ------------------------------------------------------------------
  // WorkRequest Operations
  // ------------------------------------------------------------------
  WorkRecord createWork(String workType, byte[] payload, int payloadVersion, String codec, Integer maxRetries);
  Optional<WorkRecord> getWork(String id);
  List<WorkRecord> listWork(WorkQuery query);
  List<WorkRecord> claimWork(String workType, String ownerId, int limit, Duration leaseDuration);
  boolean renewLease(String id, String ownerId, Duration leaseDuration);
  void reschedule(String id, Duration delay);
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
