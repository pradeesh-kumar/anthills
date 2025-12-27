package org.anthills.core.factory;

import org.anthills.api.scheduler.LeasedScheduler;
import org.anthills.api.scheduler.SchedulerConfig;
import org.anthills.api.work.WorkStore;
import org.anthills.core.scheduler.DefaultLeasedScheduler;

public final class Schedulers {

  private Schedulers(){}

  public static LeasedScheduler createLeasedScheduler(SchedulerConfig config, WorkStore store) {
    return new DefaultLeasedScheduler(config, store);
  }
}
