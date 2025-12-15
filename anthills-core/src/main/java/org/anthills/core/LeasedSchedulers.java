package org.anthills.core;

import org.anthills.api.WorkStore;

public final class LeasedSchedulers {

  private LeasedSchedulers() {}

  public static LeasedScheduler create(SchedulerConfig config, WorkStore store) {
    return new DefaultLeasedScheduler(config, store);
  }
}
