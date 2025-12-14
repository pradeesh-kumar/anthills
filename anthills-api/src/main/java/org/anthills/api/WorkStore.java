package org.anthills.api;

import java.time.Duration;
import java.util.List;

public interface WorkStore {

    // ---- RequestWorker operations ----

    <T> List<WorkRequest<T>> claimWork(
            String workerId,
            int batchSize,
            Duration leaseDuration,
            Class<T> payloadType
    );

    boolean renewLease(
            String workRequestId,
            String workerId,
            Duration leaseDuration
    );

    void markSucceeded(String workRequestId, String workerId);

    void markFailed(
            String workRequestId,
            String workerId,
            Throwable error
    );

    String submit(Object payload, SubmissionOptions options);

    // ---- Scheduler operations ----
    boolean tryAcquireSchedule(
            String jobName,
            String workerId,
            Duration leaseDuration
    );

    void renewScheduleLease(
            String jobName,
            String workerId,
            Duration leaseDuration
    );

    void releaseSchedule(
            String jobName,
            String workerId
    );
}
