package org.anthills.core.work.processor;

import org.anthills.api.PayloadCodec;
import org.anthills.api.WorkHandler;
import org.anthills.api.ProcessorConfig;
import org.anthills.api.WorkRecord;
import org.anthills.api.WorkRequest;
import org.anthills.api.WorkRequestProcessor;
import org.anthills.api.WorkStore;
import org.anthills.core.concurrent.NamedThreadFactory;
import org.anthills.core.util.Backoff;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.anthills.core.util.Utils.min;

public class DefaultWorkRequestProcessor implements WorkRequestProcessor {

  private final String workType;
  private final WorkStore store;
  private final PayloadCodec codec;
  private final ProcessorConfig config;

  private final ExecutorService workerPool;
  private final ScheduledExecutorService poller;

  private final Map<Class<?>, WorkHandler<?>> handlers = new ConcurrentHashMap<>();

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final String ownerId = UUID.randomUUID().toString();
  private final Backoff backoff;

  private final Duration minPoll;
  private final Duration maxPoll;
  private volatile Duration currentPoll;

  public DefaultWorkRequestProcessor(String workType, WorkStore store, PayloadCodec codec, ProcessorConfig config) {
    this.workType = Objects.requireNonNull(workType);
    this.store = Objects.requireNonNull(store);
    this.codec = Objects.requireNonNull(codec);
    this.config = Objects.requireNonNull(config);

    this.workerPool = Executors.newFixedThreadPool(config.workerThreads(), new NamedThreadFactory("work-" + workType));
    this.poller = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("poller-" + workType));
    this.backoff = Backoff.exponential(Duration.ofSeconds(1), Duration.ofMinutes(5), true); // TODO these params should be in config

    this.minPoll = config.pollInterval();
    this.maxPoll = config.pollInterval().multipliedBy(10);
    this.currentPoll = minPoll;
  }

  @Override
  public void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }
    scheduleNextPoll();
  }

  private void scheduleNextPoll() {
    poller.schedule(
      () -> {
        try {
          pollAndDispatch();
        } finally {
          if (running.get()) {
            scheduleNextPoll();
          }
        }
      },
      currentPoll.toMillis(),
      TimeUnit.MILLISECONDS
    );
  }

  @Override
  public void stop() {
    running.set(false);
    poller.shutdown();
    workerPool.shutdown();
  }

  @Override
  public void awaitTermination() throws InterruptedException {
    workerPool.awaitTermination(config.shutdownTimeout().getSeconds(), TimeUnit.SECONDS);
  }

  private void pollAndDispatch() {
    if (!running.get()) {
      return;
    }
    int freeSlots = config.workerThreads() - ((ThreadPoolExecutor) workerPool).getActiveCount();
    if (freeSlots <= 0) {
      return;
    }
    List<WorkRecord> records = store.claimWork(workType, ownerId, freeSlots, config.leaseDuration());
    if (records.isEmpty()) {
      currentPoll = min(currentPoll.multipliedBy(5), maxPoll);
      return;
    }
    currentPoll = minPoll;
    records.forEach(record -> workerPool.submit(() -> process(record)));
  }

  @SuppressWarnings("unchecked")
  private <T> void process(WorkRecord record) {
    WorkHandler<T> handler = null;
    Class<T> payloadType = null;

    for (var entry : handlers.entrySet()) {
      if (entry.getKey().getName().equals(record.codec())) {
        payloadType = (Class<T>) entry.getKey();
        handler = (WorkHandler<T>) entry.getValue();
        break;
      }
    }
    if (handler == null) {
      store.markFailed(record.id(), ownerId, "No handler registered");
      return;
    }
    WorkRequest<T> request = record.toWorkRequest(payloadType, codec);
    try {
      handler.handle(request);
      store.markSucceeded(record.id(), ownerId);
    } catch (Exception e) {
      handleFailure(record, e);
    }
  }

  private void handleFailure(WorkRecord record, Exception error) {
    int attempts = record.attemptCount();
    int maxRetries = record.maxRetries();
    if (attempts >= maxRetries) {
      store.markFailed(record.id(), ownerId, error.getMessage());
      return;
    }
    Duration backOffDelay = backoff.nextDelay(attempts);
    store.reschedule(record.id(), backOffDelay);
  }

  private int effectiveMaxRetries(WorkRecord record) {
    int requestMax = record.maxRetries() != null ? record.maxRetries() : config.defaultMaxRetries();
    return Math.min(requestMax, config.maxAllowedRetries());
  }

  @Override
  public <T> void registerHandler(String workType, Class<T> payloadType, WorkHandler<T> handler) {
    Objects.requireNonNull(handler);
    Objects.requireNonNull(workType);
    Objects.requireNonNull(payloadType);

    if (!this.workType.equalsIgnoreCase(workType)) {
      throw new IllegalArgumentException("Processor handles workType=" + this.workType);
    }
    handlers.put(payloadType, handler);
  }

  record HandlerRegistration<T>(
    String workType,
    Class<T> payloadType,
    WorkHandler<T> handler
  ) {
  }
}
