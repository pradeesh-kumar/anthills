package org.anthills.core;

import java.time.Duration;

public non-sealed class LeasedScheduledWorker extends ScheduledWorker {

  private final LeaseManager leaseManager;
  private final String leaseObject = this.getClass().getName();
  private final Duration leasePeriod;
  private final Watch watch;

  public LeasedScheduledWorker(Runnable task) {
    this(SchedulerConfig.defaultConfig(), task);
  }

  public LeasedScheduledWorker(SchedulerConfig config, Runnable task) {
    super(config, task);
    this.leaseManager = new JdbcLeaseManager();
    this.leasePeriod = config.period().plusMillis(100);
    this.watch = new Watch(this::monitorAndExtendLease, config.period());
  }

  @Override
  protected void runTask() {
    if (!leaseManager.acquire(identity(), leaseObject, leasePeriod)) {
      System.out.println("Could not acquire lease for " + identity()); // TODO add logger
      return;
    }
    watch.start();
    super.runTask();
    watch.stop();
    leaseManager.release(identity(), leaseObject);
  }

  private void monitorAndExtendLease() {
    if (!running.get()) {
      watch.stop();
    }
    leaseManager.extend(identity(), leaseObject, leasePeriod);
  }
}
