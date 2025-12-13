package org.anthills.core;

import org.anthills.commons.WorkRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RequestWorker<T> implements Worker {

  private static final Logger log = LoggerFactory.getLogger(RequestWorker.class);
  private static final Duration LEASE_PERIOD = Duration.ofMinutes(5);

  private final WorkerConfig config;
  private final Consumer<WorkRequest<T>> wrConsumer;
  private final WorkItemClaimer workItemClaimer;
  private final DefaultWorkRequestService workRequestService;
  private final String identity = UUID.randomUUID().toString();
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final Class<T> payloadType;
  private ScheduledExecutorService poller;
  private ThreadPoolExecutor workerPool;
  private ScheduledExecutorService leaseRenewer;
  private final ConcurrentHashMap<String, InFlightTask> inFlight = new ConcurrentHashMap<>();

  RequestWorker(WorkerConfig config, WorkItemClaimer workItemClaimer, DefaultWorkRequestService workRequestService, Class<T> payloadType, Consumer<WorkRequest<T>> wrConsumer) {
    Objects.requireNonNull(config, "config is required");
    Objects.requireNonNull(workItemClaimer, "workItemClaimer is required");
    Objects.requireNonNull(payloadType, "payloadType is required");
    Objects.requireNonNull(wrConsumer, "wrConsumer is required");
    this.config = config;
    this.wrConsumer = wrConsumer;
    this.workItemClaimer = workItemClaimer;
    this.payloadType = payloadType;
    this.workRequestService = workRequestService;
  }

  public String identity() {
    return this.identity;
  }

  @Override
  public void start() {
    if (!running.compareAndSet(false, true)) {
      throw new IllegalStateException("Worker has already been started!");
    }
    workerPool = new ThreadPoolExecutor(
      config.workers(),
      config.workers(),
      0L,
      TimeUnit.MILLISECONDS,
      new ArrayBlockingQueue<>(0) // no queue
    );
    poller = Executors.newSingleThreadScheduledExecutor();
    leaseRenewer = Executors.newSingleThreadScheduledExecutor();
    poller.scheduleWithFixedDelay(() -> {
      try {
        pollAndDispatchBatch();
      } catch (Exception e) {
        log.error("Poller error", e);
      }
    }, 0, config.workPollPeriod().toMillis(), TimeUnit.MILLISECONDS);
    leaseRenewer.scheduleWithFixedDelay(this::renewLeases, LEASE_PERIOD.getSeconds() / 2, LEASE_PERIOD.getSeconds() / 2, TimeUnit.SECONDS);
  }

  private void renewLeases() {
    Instant now = Instant.now();
    for (InFlightTask task : inFlight.values()) {
      Duration remaining = Duration.between(now, task.leaseUntil());
      // TODO Check if lease expired
      if (remaining.compareTo(config.getRenewThreshold()) <= 0) {
        boolean renewed = workRequestService.extendLease(task.workRequestId, identity, LEASE_PERIOD);
        if (renewed) {
          inFlight.computeIfPresent(
            task.workRequestId(),
            (id, old) -> new InFlightTask(
              id,
              now.plus(LEASE_PERIOD),
              now
            )
          );
        } else {
          log.warn("Failed to renew lease for {}", task.workRequestId());
        }
      }
    }
  }

  private int freeSlots() {
    return workerPool.getMaximumPoolSize() - workerPool.getActiveCount();
  }

  private void pollAndDispatchBatch() {
    int free = freeSlots();
    if (free <= 0) {
      log.debug("All workers are busy, skipping poll");
      return;
    }
    int batchSize = Math.min(free, config.maxBatchSize());
    WorkItemClaimer.ClaimRequest<T> claimRequest = WorkItemClaimer.ClaimRequest.<T>builder()
      .batchSize(batchSize)
      .leasePeriod(LEASE_PERIOD)
      .owner(identity)
      .payloadType(payloadType)
      .statuses(WorkRequest.Status.nonTerminalStatuses())
      .build();
    List<WorkRequest<T>> claimedWrs = workItemClaimer.claim(claimRequest);
    claimedWrs.forEach(this::dispatch);
  }

  private void dispatch(WorkRequest<T> wr) {
    workerPool.execute(() -> {
      inFlight.put(
        wr.id(),
        new InFlightTask(wr.id(), wr.leaseUntil(), Instant.now())
      );

      try {
        wrConsumer.accept(wr);
        if (config.enableAutoAck()) {
          workRequestService.markSucceeded(wr);
        }
      } catch (Exception e) {
        workRequestService.markFailedOrRetry(wr);
      } finally {
        inFlight.remove(wr.id());
      }
    });
  }

  @Override
  public void stop() {
    if (!running.compareAndSet(true, false)) {
      throw new IllegalStateException("Worker has already been stopped!");
    }
    poller.shutdownNow();
    leaseRenewer.shutdownNow();
    workerPool.shutdown();
  }

  @Override
  public void awaitTermination() {
    try {
      if (running.get()) {
        stop();
      }
      this.workerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      log.error("[{}] Interrupted while waiting for task to complete", identity, e);
    }
  }

  private record InFlightTask(
    String workRequestId,
    Instant leaseUntil,
    Instant lastRenewedAt
  ) {
  }
}
