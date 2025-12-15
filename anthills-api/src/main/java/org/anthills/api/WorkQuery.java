package org.anthills.api;

import java.time.Instant;
import java.util.Set;

public record WorkQuery(
    String workType,
    Set<WorkRequest.Status> statuses,
    Instant createdAfter,
    Instant createdBefore,
    int limit
) {
    public static WorkQuery defaults() {
        return new WorkQuery(null, null, null, null, 100);
    }
}
