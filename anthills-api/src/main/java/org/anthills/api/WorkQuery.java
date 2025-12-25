package org.anthills.api;

import java.time.Instant;
import java.util.Set;

public record WorkQuery(
  String workType,
  Set<WorkRequest.Status> statuses,
  Instant createdAfter,
  Instant createdBefore,
  Page page
) {

  public record Page(int limit, int offset) {
    public static Page of(int limit, int offset) {
      return new Page(limit, offset);
    }
  }

  public static WorkQuery defaults() {
    return new WorkQuery(null, null, null, null, Page.of(1, 0));
  }
}
