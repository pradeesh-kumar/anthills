package org.anthills.core;

import java.time.Instant;

public record Lease(
  String entityId,
  String owner,
  Instant acquiredTs,
  Instant until) {
}
