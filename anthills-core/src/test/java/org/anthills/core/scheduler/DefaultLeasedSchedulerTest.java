package org.anthills.core.scheduler;

import org.anthills.api.Job;
import org.anthills.api.Schedule;
import org.anthills.api.SchedulerConfig;
import org.anthills.api.WorkStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DefaultLeasedScheduler.
 *
 * These tests use very small durations to keep runtime short while exercising the
 * scheduling and leasing behavior. Each test ensures the scheduler is shut down
 * to avoid leaking threads.
 */
@ExtendWith(MockitoExtension.class)
public class DefaultLeasedSchedulerTest {

  @Mock
  WorkStore store;

  @Captor
  ArgumentCaptor<String> ownerCaptor;

  @Captor
  ArgumentCaptor<String> renewOwnerCaptor;

  private static SchedulerConfig cfg(Duration lease, Duration renew, Duration shutdown) {
    return new SchedulerConfig(lease, renew, shutdown);
  }

  @Test
  @Timeout(5)
  void scheduleDuplicateNameThrows() {
    var config = cfg(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(2));
    var scheduler = new DefaultLeasedScheduler(config, store);

    Job job = () -> {};
    scheduler.schedule("jobA", new Schedule.FixedRate(Duration.ofMillis(50)), job);

    assertThrows(IllegalArgumentException.class,
      () -> scheduler.schedule("jobA", new Schedule.FixedRate(Duration.ofMillis(50)), job));

    scheduler.close();
  }

  @Test
  @Timeout(5)
  void scheduleAfterStartThrows() {
    var config = cfg(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(2));
    var scheduler = new DefaultLeasedScheduler(config, store);

    scheduler.schedule("jobA", new Schedule.FixedRate(Duration.ofMillis(100)), () -> {});
    scheduler.start();
    assertThrows(IllegalStateException.class,
      () -> scheduler.schedule("jobB", new Schedule.FixedRate(Duration.ofMillis(100)), () -> {}));
    scheduler.close();
  }

  @Test
  @Timeout(10)
  void startsAndRunsJobWhenLeaseAcquired_andRenewsLease() throws Exception {
    // Use small intervals so the renewer runs while the job is executing
    var config = cfg(Duration.ofSeconds(1), Duration.ofMillis(10), Duration.ofSeconds(2));
    var scheduler = new DefaultLeasedScheduler(config, store);

    // Make acquiring the lease succeed and allow renewal to continue
    when(store.tryAcquireSchedulerLease(eq("job1"), anyString(), any())).thenReturn(true);
    when(store.renewSchedulerLease(eq("job1"), anyString(), any())).thenReturn(true);

    CountDownLatch jobStarted = new CountDownLatch(1);
    CountDownLatch allowFinish = new CountDownLatch(1);

    Job longRunningJob = () -> {
      jobStarted.countDown();
      // Keep the job alive long enough for at least one renew tick
      allowFinish.await(200, TimeUnit.MILLISECONDS);
    };

    scheduler.schedule("job1", new Schedule.FixedRate(Duration.ofMillis(10)), longRunningJob);
    scheduler.start();

    // Wait for job to actually start
    assertTrue(jobStarted.await(1, TimeUnit.SECONDS), "Job did not start in time");

    // Capture ownerId used for acquisition
    verify(store, atLeastOnce()).tryAcquireSchedulerLease(eq("job1"), ownerCaptor.capture(), any());
    String ownerId = ownerCaptor.getValue();
    assertNotNull(ownerId);

    // Verify at least one renewal occurs with the same ownerId
    verify(store, timeout(500).atLeastOnce()).renewSchedulerLease(eq("job1"), renewOwnerCaptor.capture(), any());
    // All renewals must use same ownerId
    assertTrue(renewOwnerCaptor.getAllValues().stream().allMatch(ownerId::equals),
      "Renew calls used a different ownerId than acquire");

    // Let the job complete and shutdown
    allowFinish.countDown();
    scheduler.close();
  }

  @Test
  @Timeout(5)
  void doesNotRunJobWhenLeaseNotAcquired() throws Exception {
    var config = cfg(Duration.ofSeconds(1), Duration.ofMillis(50), Duration.ofSeconds(2));
    var scheduler = new DefaultLeasedScheduler(config, store);

    // Always fail to acquire the lease
    when(store.tryAcquireSchedulerLease(eq("jobX"), anyString(), any())).thenReturn(false);

    AtomicInteger executions = new AtomicInteger();
    Job countingJob = executions::incrementAndGet;

    scheduler.schedule("jobX", new Schedule.FixedRate(Duration.ofMillis(20)), countingJob);
    scheduler.start();

    // Give some time for a few triggers
    Thread.sleep(200);

    assertEquals(0, executions.get(), "Job should not run if lease is not acquired");
    scheduler.close();
  }

  @Test
  @Timeout(5)
  void startIsIdempotent() throws Exception {
    var config = cfg(Duration.ofSeconds(1), Duration.ofMillis(50), Duration.ofSeconds(2));
    var scheduler = new DefaultLeasedScheduler(config, store);

    when(store.tryAcquireSchedulerLease(eq("jobIdem"), anyString(), any())).thenReturn(true);

    CountDownLatch ran = new CountDownLatch(1);
    scheduler.schedule("jobIdem", new Schedule.FixedRate(Duration.ofMillis(10)), () -> ran.countDown());

    // Calling start twice should not throw and should not break execution
    scheduler.start();
    scheduler.start();

    assertTrue(ran.await(1, TimeUnit.SECONDS), "Job did not run even after start() was called twice");
    scheduler.close();
  }
}
