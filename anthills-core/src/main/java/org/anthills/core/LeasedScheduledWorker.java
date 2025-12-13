package org.anthills.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public non-sealed class LeasedScheduledWorker extends ScheduledWorker {

  private static final Logger log = LoggerFactory.getLogger(LeasedScheduledWorker.class);

  private final LeaseService leaseService;
  private final Duration leasePeriod;
  private final Watch leaseRenewalWatch;
  private final String leaseObject;
  private final Lock leaseMonitorLock = new ReentrantLock();

  LeasedScheduledWorker(SchedulerConfig config, Runnable task, LeaseService leaseService) {
    super(config, task);
    this.leaseObject = config.jobName();
    this.leaseService = leaseService;
    this.leasePeriod = config.period().plusMillis(100);
    this.leaseRenewalWatch = new Watch(identity(), this::monitorAndExtendLease, config.period());
  }

  @Override
  protected void runTask() {
    if (running.get()) {
      log.debug("[{}] Existing task is still running.", identity());
      return;
    }
    boolean leaseAcquired = false;
    try {
      leaseAcquired = leaseService.acquire(identity(), leaseObject, leasePeriod);
      if (!leaseAcquired) {
        log.info("Entity {} Could not acquire lease on object {}", identity(), leaseObject);
        return;
      }
      leaseRenewalWatch.start();
      super.runTask();
    } catch (Exception e) {
      log.error("[{}] Error running LeasedScheduledWorker task", identity(), e);
      this.stop();
    } finally {
      leaseRenewalWatch.stop();
      if (leaseAcquired) {
        leaseService.release(identity(), leaseObject);
      }
    }
  }

  private void monitorAndExtendLease() {
    if (!leaseMonitorLock.tryLock()) {
      return;
    }
    try {
      if (!running.get()) {
        leaseRenewalWatch.stop();
        return;
      }
      if (!leaseService.extend(identity(), leaseObject, leasePeriod)) {
        log.warn("Entity {} could not extend lease on object {}", identity(), leaseObject);
        leaseRenewalWatch.stop();
        super.interruptCurrentTask();
      }
    } catch (Exception e) {
      log.error("[{}] Failed to extend lease", identity(), e);
      if (!doesLeaseStillExists()) {
        leaseRenewalWatch.stop();
        super.interruptCurrentTask();
      }
    } finally {
      leaseMonitorLock.unlock();
    }
  }

  private boolean doesLeaseStillExists() {
    try {
      return leaseService.exists(identity(), leaseObject);
    } catch (Exception e) {
      log.warn("[{}] Failed to check if the lease exists", identity(), e);
      return false;
    }
  }
}
