package org.anthills.core;

public final class LeasedSchedulers {

  private LeasedSchedulers() {}

  public static LeasedScheduler create(SchedulerConfig config, WorkStore store) {
    return new DefaultLeasedScheduler(config, store);
  }
}
