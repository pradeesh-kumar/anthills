package org.anthills.core.factory;

import org.anthills.api.scheduler.LeasedScheduler;
import org.anthills.api.scheduler.SchedulerConfig;
import org.anthills.api.work.WorkStore;
import org.anthills.core.scheduler.DefaultLeasedScheduler;

/**
 * Factory utilities for creating scheduler implementations.
 */
public final class Schedulers {

  private Schedulers(){}

  /**
   * Creates a {@link LeasedScheduler}
   *
   * @param config runtime configuration controlling leases and shutdown behavior
   * @param store persistence used to acquire/renew scheduler leases
   * @return a {@link DefaultLeasedScheduler} instance
   * @throws NullPointerException if any argument is null
   */
  public static LeasedScheduler createLeasedScheduler(SchedulerConfig config, WorkStore store) {
    return new DefaultLeasedScheduler(config, store);
  }
}
