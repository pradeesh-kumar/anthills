package org.anthills.core.work;

import org.anthills.api.codec.PayloadCodec;
import org.anthills.api.work.WorkHandler;
import org.anthills.api.work.ProcessorConfig;
import org.anthills.api.work.WorkRecord;
import org.anthills.api.work.WorkRequest;
import org.anthills.api.work.WorkRequestProcessor;
import org.anthills.api.work.WorkStore;
import org.anthills.core.concurrent.LeaseBoundExecutor;
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

/**
 * Default {@link WorkRequestProcessor} that polls a {@link WorkStore}, claims work items,
 * decodes payloads with a {@link PayloadCodec}, and dispatches them to registered {@link WorkHandler}s.
 *
 * Behavior
 * - Polls for available work respecting configured concurrency and backoff.
 * - Uses a {@link org.anthills.core.concurrent.LeaseBoundExecutor} to renew leases while handlers run.
 * - Marks work succeeded/failed or reschedules with backoff according to outcomes.
 *
 * Thread-safety: designed for multi-threaded processing with an internal fixed worker pool
 * and a single-threaded poller. Start/stop are idempotent.
 */
public class DefaultWorkRequestProcessor implements WorkRequestProcessor {

  private final String workType;
  private final WorkStore store;
  private final PayloadCodec codec;
  private final ProcessorConfig config;

  private final ExecutorService workerPool;
  private final ScheduledExecutorService poller;
  private final LeaseBoundExecutor leaseExecutor;

  private final Map<String, WorkHandler<?>> handlers = new ConcurrentHashMap<>();

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final String ownerId = UUID.randomUUID().toString();
  private final Backoff backoff;

  private final Duration minPoll;
  private final Duration maxPoll;
  private volatile Duration currentPoll;

  /**
   * Creates a processor for a single {@code workType}.
   *
   * @param workType routing key this processor is responsible for
   * @param store persistence used to claim, renew, and mark work
   * @param codec codec used to decode stored payloads
   * @param config tuning parameters for threads, leasing, and polling
   * @throws NullPointerException if any argument is null
   */
  public DefaultWorkRequestProcessor(String workType, WorkStore store, PayloadCodec codec, ProcessorConfig config) {
    this.workType = Objects.requireNonNull(workType);
    this.store = Objects.requireNonNull(store);
    this.codec = Objects.requireNonNull(codec);
    this.config = Objects.requireNonNull(config);

    this.workerPool = Executors.newFixedThreadPool(config.workerThreads(), new NamedThreadFactory("work-" + workType));
    this.poller = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("poller-" + workType));
    this.leaseExecutor = new LeaseBoundExecutor(config.leaseRenewInterval(), "work-" + workType);

    this.backoff = Backoff.exponential(Duration.ofSeconds(1), Duration.ofMinutes(5), true); // TODO these params should be in config

    this.minPoll = config.pollInterval();
    this.maxPoll = config.pollInterval().multipliedBy(10);
    this.currentPoll = minPoll;
  }

  @Override
  /**
   * Starts polling and dispatching if not already running. Idempotent.
   */
  public void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }
    scheduleNextPoll();
  }

  /**
   * Schedules the next polling tick using the current backoff-aware delay.
   * Always re-schedules itself while the processor remains running.
   */
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
  /**
   * Requests a graceful stop, shutting down the poller and worker pool, and
   * stopping the lease renewer. Idempotent.
   */
  public void stop() {
    running.set(false);
    poller.shutdown();
    workerPool.shutdown();
    try {
      leaseExecutor.shutdown(config.shutdownTimeout());
    } catch (InterruptedException _) {
    }
  }

  @Override
  /**
   * Blocks until the worker pool terminates or the configured shutdown timeout elapses.
   *
   * @throws InterruptedException if the waiting thread is interrupted
   */
  public void awaitTermination() throws InterruptedException {
    workerPool.awaitTermination(config.shutdownTimeout().getSeconds(), TimeUnit.SECONDS);
  }

  /**
   * Polls the store for available work up to the number of free worker slots and
   * dispatches claimed records to the worker pool. Applies backoff when no work is found.
   */
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

  /**
   * Processes a single claimed record by decoding it and invoking the registered handler
   * for its payload type. Failures are handled according to {@link #handleFailure(WorkRecord, Exception)}.
   */
  @SuppressWarnings("unchecked")
  private void process(WorkRecord record) {
    if (!codec.name().equalsIgnoreCase(record.codec())) {
      store.markFailed(record.id(), ownerId, "Payload with codec " + record.codec() + " is not supported by the processor.");
      return;
    }
    WorkRequest<Object> workRequest = record.toWorkRequest(codec, Object.class);
    WorkHandler<Object> handler = (WorkHandler<Object>) handlers.get(record.payloadType());
    if (handler == null) {
      store.markFailed(record.id(), ownerId, "No handler registered registered for payload type");
      return;
    }
    leaseExecutor.execute(() -> {
      try {
        handler.handle(workRequest);
        store.markSucceeded(record.id(), ownerId);
      } catch (Exception e) {
        handleFailure(record, e);
      }
    }, () -> store.renewWorkerLease(record.id(), ownerId, config.leaseDuration()), workerPool);
  }

  /**
   * Handles handler failures by either marking the record failed if the attempt limit
   * has been reached, or rescheduling it after a backoff interval.
   *
   * @param record the failed work record
   * @param error the exception raised by the handler
   */
  private void handleFailure(WorkRecord record, Exception error) {
    int attempts = record.attemptCount();
    int maxRetries = effectiveMaxRetries(record);
    if (attempts >= maxRetries) {
      store.markFailed(record.id(), ownerId, error.getMessage());
      return;
    }
    Duration backOffDelay = backoff.nextDelay(attempts);
    store.reschedule(record.id(), backOffDelay);
  }

  /**
   * Computes the effective retry cap as the minimum of the request-specific limit (if any)
   * and the processor's configured maximum.
   *
   * @param record the work record
   * @return maximum number of attempts allowed
   */
  private int effectiveMaxRetries(WorkRecord record) {
    int requestMax = record.maxRetries() != null ? record.maxRetries() : config.defaultMaxRetries();
    return Math.min(requestMax, config.maxAllowedRetries());
  }

  @Override
  /**
   * Registers a handler for the given payload type name within this processor's work type.
   * Replaces any existing handler for the same payload type.
   *
   * @param workType must match this processor's {@code workType}
   * @param payloadType expected payload class for the handler
   * @param handler business logic to execute
   * @param <T> payload type
   * @throws IllegalArgumentException if {@code workType} does not match this processor
   * @throws NullPointerException if any argument is null
   */
  public <T> void registerHandler(String workType, Class<T> payloadType, WorkHandler<T> handler) {
    Objects.requireNonNull(handler);
    Objects.requireNonNull(workType);
    Objects.requireNonNull(payloadType);

    if (!this.workType.equalsIgnoreCase(workType)) {
      throw new IllegalArgumentException("This processor handles only workType=" + this.workType);
    }
    handlers.put(payloadType.getName(), handler);
  }
}
