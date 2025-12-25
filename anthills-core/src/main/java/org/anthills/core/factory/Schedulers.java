package org.anthills.core.factory;

import org.anthills.api.LeasedScheduler;
import org.anthills.api.SchedulerConfig;
import org.anthills.api.WorkStore;
import org.anthills.core.scheduler.DefaultLeasedScheduler;

public final class Schedulers {

  private Schedulers(){}

  public static LeasedScheduler createLeasedScheduler(SchedulerConfig config, WorkStore store) {
    return new DefaultLeasedScheduler(config, store);
  }
}
