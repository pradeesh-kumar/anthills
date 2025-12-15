package org.anthills.core.factory;

public final class LeasedSchedulers {

  private LeasedSchedulers(){}

  public static LeasedScheduler create(SchedulerConfig config, WorkStore store) {
    return new DefaultLeasedScheduler(config, store);
  }
}
