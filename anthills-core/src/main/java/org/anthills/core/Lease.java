package org.anthills.core;

import java.time.Instant;

public record Lease(
  String object,
  String owner,
  Instant expiresAt) {
}
