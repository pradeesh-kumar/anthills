package org.anthills.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public non-sealed class LeasedScheduledWorker extends ScheduledWorker {

  private static final Logger log = LoggerFactory.getLogger(LeasedScheduledWorker.class);

  private final LeaseService leaseService;
  private final Duration leasePeriod;
  private final Watch watch;
  private final String leaseObject;

  LeasedScheduledWorker(SchedulerConfig config, Runnable task, LeaseService leaseService) {
    super(config, task);
    this.leaseObject = config.jobName();
    this.leaseService = leaseService;
    this.leasePeriod = config.period().plusMillis(100);
    this.watch = new Watch(identity(), this::monitorAndExtendLease, config.period());
  }

  @Override
  protected void runTask() {
    if (running.get()) {
      log.debug("[{}] Existing task is still running.", identity());
      return;
    }
    try {
      if (!leaseService.acquire(identity(), leaseObject, leasePeriod)) {
        log.info("Entity {} Could not acquire lease on object {}", identity(), leaseObject);
        return;
      }
      watch.start();
      super.runTask();
      watch.stop();
      leaseService.release(identity(), leaseObject);
    } catch (Exception e) {
      log.error("[{}] Error running LeasedScheduledWorker task", identity(), e);
      watch.stop();
      this.stop();
    }
  }

  private void monitorAndExtendLease() {
    if (!running.get()) {
      watch.stop();
    }
    if (!leaseService.extend(identity(), leaseObject, leasePeriod)) {
      log.warn("Entity {} could not extend lease on object {}", identity(), leaseObject);
      watch.stop();
      super.interruptCurrentTask();
    }
  }
}
