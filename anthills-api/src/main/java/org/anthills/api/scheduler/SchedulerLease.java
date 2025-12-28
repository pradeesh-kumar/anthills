package org.anthills.api.scheduler;

import java.time.Instant;

public record SchedulerLease(
    String jobName,
    String ownerId,
    Instant leaseUntil
) {}
