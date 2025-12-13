package org.anthills.core;

import org.anthills.commons.WorkRequest;
import org.anthills.core.contract.WorkRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RequestWorker<T> implements Worker {

  private static final Logger log = LoggerFactory.getLogger(RequestWorker.class);

  private static final int BATCH_RELOAD_THRESHOLD = 5;

  private final WorkerConfig config;
  private final Consumer<WorkRequest<T>> wrConsumer;
  private final WorkItemClaimer workItemClaimer;
  private final WorkRequestService workRequestService;
  private final String identity = UUID.randomUUID().toString();
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final Class<T> payloadType;
  private ScheduledExecutorService poller;
  private ExecutorService workerPool;
  private Watch leaseRenewWatch;

  RequestWorker(WorkerConfig config, WorkItemClaimer workItemClaimer, WorkRequestService workRequestService, Class<T> payloadType, Consumer<WorkRequest<T>> wrConsumer) {
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
    workerPool = Executors.newFixedThreadPool(config.workers());
    poller = Executors.newSingleThreadScheduledExecutor();
    leaseRenewWatch = new Watch("RequestWorker", this::monitorAndRenewLease, Duration.ofMinutes(1));
    leaseRenewWatch.start();
    poller.scheduleWithFixedDelay(() -> {
      try {
        claimAndDispatchBatch();
      } catch (Exception e) {
        log.error("Poller error", e);
      }
    }, 0, config.workPollPeriod().toMillis(), TimeUnit.MILLISECONDS);
  }

  private void monitorAndRenewLease() {

  }

  private void claimAndDispatchBatch() {
    WorkItemClaimer.ClaimRequest<T> claimRequest = WorkItemClaimer.ClaimRequest.<T>builder()
      .limit(config.workRequestFetchBatchSize())
      .leasePeriod(Duration.ofMinutes(1))
      .owner(identity)
      .payloadType(payloadType)
      .statuses(WorkRequest.Status.nonTerminalStatuses())
      .build();
    List<WorkRequest<T>> claimedWrs = workItemClaimer.claim(claimRequest);
    claimedWrs.forEach(wr -> workerPool.submit(() -> processTask(wr)));
  }

  private void processTask(WorkRequest<T> wr) {
    try {
      wrConsumer.accept(wr);
      workRequestService.markSucceeded(wr);
    } catch (Exception e) {
      log.error("failed processing {}", wr.id(), e);
      workRequestService.markFailedOrRetry(wr);
    }
  }

  @Override
  public void stop() {
    running.set(false);
    poller.shutdown();
    workerPool.shutdown();
    leaseRenewWatch.stop();
  }

  @Override
  public void awaitTermination() {
    throw new UnsupportedOperationException("start() is not implemented yet");
  }
}
