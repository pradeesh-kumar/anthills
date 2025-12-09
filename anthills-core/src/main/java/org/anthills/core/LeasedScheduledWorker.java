package org.anthills.core;

import java.time.Duration;

public non-sealed class LeasedScheduledWorker extends ScheduledWorker {

  private final LeaseService leaseService;
  private final Duration leasePeriod;
  private final Watch watch;
  private final String leaseObject = this.getClass().getName();

  LeasedScheduledWorker(SchedulerConfig config, Runnable task, LeaseService leaseService) {
    super(config, task);
    this.leaseService = leaseService;
    this.leasePeriod = config.period().plusMillis(100);
    this.watch = new Watch(this::monitorAndExtendLease, config.period());
  }

  @Override
  protected void runTask() {
    if (!leaseService.acquire(identity(), leaseObject, leasePeriod)) {
      System.out.println("Could not acquire lease for " + identity()); // TODO add logger
      return;
    }
    watch.start();
    super.runTask();
    watch.stop();
    leaseService.release(identity(), leaseObject);
  }

  private void monitorAndExtendLease() {
    if (!running.get()) {
      watch.stop();
    }
    if (!leaseService.extend(identity(), leaseObject, leasePeriod)) {
      System.out.println("Failed to extend lease for " + identity());
      watch.stop();
      // TODO stop the running task when release extension fails
    }
  }
}
