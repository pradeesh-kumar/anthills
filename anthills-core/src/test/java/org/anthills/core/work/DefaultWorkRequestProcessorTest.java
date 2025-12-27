package org.anthills.core.work;

import org.anthills.api.codec.PayloadCodec;
import org.anthills.api.work.ProcessorConfig;
import org.anthills.api.work.WorkHandler;
import org.anthills.api.work.WorkRecord;
import org.anthills.api.work.WorkRequest;
import org.anthills.api.work.WorkStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultWorkRequestProcessorTest {

  @Mock
  WorkStore store;

  @Mock
  PayloadCodec codec;

  private ProcessorConfig cfg() {
    // Small intervals to keep tests fast; validations must pass:
    // - workerThreads > 0
    // - defaultMaxRetries <= maxAllowedRetries
    // - leaseRenewInterval < leaseDuration
    return new ProcessorConfig(
      1,                       // workerThreads
      2,                       // defaultMaxRetries
      5,                       // maxAllowedRetries
      Duration.ofMillis(10),   // pollInterval
      Duration.ofMillis(200),  // leaseDuration
      Duration.ofMillis(50),   // leaseRenewInterval
      Duration.ofSeconds(1)    // shutdownTimeout
    );
  }

  private WorkRecord record(String id, String workType, String codecName, int attemptCount) {
    return WorkRecord.builder()
      .id(id)
      .workType(workType)
      .payload(new byte[]{1, 2, 3})
      // Use Object.class to satisfy WorkRecord.toWorkRequest(codec, Object.class) assignability check
      .payloadType(Object.class.getName())
      .payloadVersion(1)
      .codec(codecName)
      .status(WorkRequest.Status.NEW)
      .attemptCount(attemptCount)
      .createdTs(Instant.now())
      .build();
  }

  @Test
  @Timeout(5)
  void registerHandler_workTypeMismatchThrows() {
    DefaultWorkRequestProcessor p = new DefaultWorkRequestProcessor("email", store, codec, cfg());
    assertThrows(IllegalArgumentException.class, () -> p.registerHandler("sms", Object.class, r -> {}));
    // Not started but safe to stop for cleanup
    p.stop();
  }

  @Test
  @Timeout(10)
  void processesRecord_success_marksSucceeded() throws Exception {
    when(codec.name()).thenReturn("json");
    WorkRecord r = record("r1", "email", "json", 0);

    // First poll returns a single record, subsequent polls empty
    when(store.claimWork(eq("email"), anyString(), anyInt(), any()))
      .thenReturn(List.of(r))
      .thenReturn(List.of());

    // toWorkRequest(Object.class) will request decode(Object.class, 1)
    when(codec.decode(r.payload(), Object.class, 1)).thenReturn(new Object());

    DefaultWorkRequestProcessor p = new DefaultWorkRequestProcessor("email", store, codec, cfg());

    CountDownLatch handled = new CountDownLatch(1);
    WorkHandler<Object> handler = req -> handled.countDown();
    p.registerHandler("email", Object.class, handler);

    p.start();

    assertTrue(handled.await(2, TimeUnit.SECONDS), "Handler was not invoked");

    // Capture ownerId used by the processor
    ArgumentCaptor<String> ownerCaptor = ArgumentCaptor.forClass(String.class);
    verify(store, timeout(1000).atLeastOnce()).claimWork(eq("email"), ownerCaptor.capture(), anyInt(), any());
    String ownerId = ownerCaptor.getValue();

    verify(store, timeout(1000)).markSucceeded(eq("r1"), eq(ownerId));

    p.stop();
    p.awaitTermination();
  }

  @Test
  @Timeout(10)
  void codecMismatch_marksFailed_andSkipsHandler() throws Exception {
    when(codec.name()).thenReturn("json");
    WorkRecord r = record("r2", "email", "bin", 0);

    when(store.claimWork(eq("email"), anyString(), anyInt(), any()))
      .thenReturn(List.of(r))
      .thenReturn(List.of());

    DefaultWorkRequestProcessor p = new DefaultWorkRequestProcessor("email", store, codec, cfg());
    p.start();

    ArgumentCaptor<String> ownerCaptor = ArgumentCaptor.forClass(String.class);
    verify(store, timeout(1000).atLeastOnce()).claimWork(eq("email"), ownerCaptor.capture(), anyInt(), any());
    String ownerId = ownerCaptor.getValue();

    verify(store, timeout(1000)).markFailed(eq("r2"), eq(ownerId), argThat(msg -> msg != null && msg.contains("not supported")));
    verify(codec, timeout(1000).times(0)).decode(any(), any(), anyInt());

    p.stop();
    p.awaitTermination();
  }

  @Test
  @Timeout(10)
  void missingHandler_marksFailed() throws Exception {
    when(codec.name()).thenReturn("json");
    WorkRecord r = record("r3", "email", "json", 0);

    when(store.claimWork(eq("email"), anyString(), anyInt(), any()))
      .thenReturn(List.of(r))
      .thenReturn(List.of());

    when(codec.decode(r.payload(), Object.class, 1)).thenReturn(new Object());

    DefaultWorkRequestProcessor p = new DefaultWorkRequestProcessor("email", store, codec, cfg());
    // Do NOT register any handler -> should markFailed with 'No handler registered'
    p.start();

    ArgumentCaptor<String> ownerCaptor = ArgumentCaptor.forClass(String.class);
    verify(store, timeout(1000).atLeastOnce()).claimWork(eq("email"), ownerCaptor.capture(), anyInt(), any());
    String ownerId = ownerCaptor.getValue();

    verify(store, timeout(1000)).markFailed(eq("r3"), eq(ownerId), argThat(msg -> msg != null && msg.contains("No handler registered")));
    p.stop();
    p.awaitTermination();
  }

  @Test
  @Timeout(10)
  void handlerThrows_belowMaxRetries_reschedules() throws Exception {
    when(codec.name()).thenReturn("json");
    // attemptCount=1 ensures Backoff.nextDelay(1) is valid and reschedule path is taken (defaultMaxRetries=2)
    WorkRecord r = record("r4", "email", "json", 1);

    when(store.claimWork(eq("email"), anyString(), anyInt(), any()))
      .thenReturn(List.of(r))
      .thenReturn(List.of());

    when(codec.decode(r.payload(), Object.class, 1)).thenReturn(new Object());

    DefaultWorkRequestProcessor p = new DefaultWorkRequestProcessor("email", store, codec, cfg());
    p.registerHandler("email", Object.class, req -> { throw new RuntimeException("boom"); });

    p.start();

    // Should attempt a reschedule, not immediate failure
    verify(store, timeout(1500)).reschedule(eq("r4"), any(Duration.class));
    verify(store, never()).markSucceeded(eq("r4"), anyString());
    verify(store, never()).markFailed(eq("r4"), anyString(), anyString());

    p.stop();
    p.awaitTermination();
  }

  @Test
  @Timeout(10)
  void handlerThrows_atOrBeyondMaxRetries_marksFailed() throws Exception {
    when(codec.name()).thenReturn("json");
    // attemptCount=2 equals defaultMaxRetries=2 -> should markFailed
    WorkRecord r = record("r5", "email", "json", 2);

    when(store.claimWork(eq("email"), anyString(), anyInt(), any()))
      .thenReturn(List.of(r))
      .thenReturn(List.of());

    when(codec.decode(r.payload(), Object.class, 1)).thenReturn(new Object());

    DefaultWorkRequestProcessor p = new DefaultWorkRequestProcessor("email", store, codec, cfg());
    p.registerHandler("email", Object.class, req -> { throw new RuntimeException("fatal"); });

    p.start();

    ArgumentCaptor<String> ownerCaptor = ArgumentCaptor.forClass(String.class);
    verify(store, timeout(1000).atLeastOnce()).claimWork(eq("email"), ownerCaptor.capture(), anyInt(), any());
    String ownerId = ownerCaptor.getValue();

    verify(store, timeout(1500)).markFailed(eq("r5"), eq(ownerId), contains("fatal"));
    verify(store, never()).reschedule(eq("r5"), any(Duration.class));

    p.stop();
    p.awaitTermination();
  }
}
