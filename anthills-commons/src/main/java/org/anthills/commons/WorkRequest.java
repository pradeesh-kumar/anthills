package org.anthills.commons;

import java.time.Instant;

public record WorkRequest<T>(
  String id,
  T command,
  Status status,
  String details,
  Instant createdTs,
  Instant updatedTs,
  Instant timeStarted,
  Instant timeCompleted
) {

  public enum Status {
    New(false),
    InProgress(false),
    Paused(false),
    Cancelled(true),
    Failed(true),
    Succeeded(true);

    private final boolean isTerminal;

    Status(boolean isTerminal) {
      this.isTerminal = isTerminal;
    }

    public boolean isTerminal() {
      return isTerminal;
    }
  }
}
