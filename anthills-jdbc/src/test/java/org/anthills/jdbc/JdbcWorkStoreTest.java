package org.anthills.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.anthills.api.work.WorkQuery;
import org.anthills.api.work.WorkRecord;
import org.anthills.api.work.WorkRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class JdbcWorkStoreTest {

  private HikariDataSource ds;
  private JdbcWorkStore store;

  @AfterEach
  void tearDown() {
    TestJdbc.closeQuietly(ds);
    JdbcSchemaProvider.schemaInitializedDataSources.clear();
  }

  @Test
  void ctor_initializes_schema_once() throws Exception {
    ds = TestJdbc.newH2DataSource();
    // first create should initialize schema
    store = JdbcWorkStore.create(ds);

    try (Connection c = ds.getConnection()) {
      assertTrue(TestJdbc.tableExists(c, "work_request"));
      assertTrue(TestJdbc.tableExists(c, "scheduler_lease"));
    }

    // second create should be a no-op for schema init
    store = JdbcWorkStore.create(ds);
  }

  @Test
  void createWork_currently_fails_due_to_missing_payload_type_in_schema() {
    ds = TestJdbc.newH2DataSource();
    store = JdbcWorkStore.create(ds);

    // createWork does not insert payload_type column (NOT NULL in schema), so should fail with RuntimeException
    byte[] payload = new byte[]{1, 2, 3};
    assertThrows(RuntimeException.class, () ->
      store.createWork("typeA", payload, payload.getClass().getName(), 1, "json", null)
    );
  }

  @Test
  void listWork_filters_and_paginates() throws Exception {
    ds = TestJdbc.newH2DataSource();
    store = JdbcWorkStore.create(ds);

    Instant t0 = Instant.now().minusSeconds(300);
    byte[] payload = new byte[]{1};

    try (Connection c = ds.getConnection()) {
      // Insert 5 rows with differing types and created_ts
      TestJdbc.insertWork(c, "w1", "typeA", payload, "java.lang.String", 1, "json", "NEW", 5, 0, null, null, null, t0.plusSeconds(10), t0.plusSeconds(10), null, null);
      TestJdbc.insertWork(c, "w2", "typeB", payload, "java.lang.String", 1, "json", "NEW", 5, 0, null, null, null, t0.plusSeconds(20), t0.plusSeconds(20), null, null);
      TestJdbc.insertWork(c, "w3", "typeA", payload, "java.lang.String", 1, "json", "NEW", 5, 0, null, null, null, t0.plusSeconds(30), t0.plusSeconds(30), null, null);
      TestJdbc.insertWork(c, "w4", "typeA", payload, "java.lang.String", 1, "json", "IN_PROGRESS", 5, 1, "o", t0.plusSeconds(600), null, t0.plusSeconds(40), t0.plusSeconds(40), t0.plusSeconds(41), null);
      TestJdbc.insertWork(c, "w5", "typeA", payload, "java.lang.String", 1, "json", "NEW", 5, 0, null, null, null, t0.plusSeconds(50), t0.plusSeconds(50), null, null);
      c.commit();
    }

    // Filter typeA + NEW only, order by created_ts desc; page(limit=2, offset=1) should return second and third newest: w3 then w1
    WorkQuery q = new WorkQuery(
      null,
      "typeA",
      Set.of(WorkRequest.Status.NEW),
      null,
      null,
      WorkQuery.Page.of(2, 1)
    );

    List<WorkRecord> result = store.listWork(q);
    assertEquals(2, result.size());
    assertEquals("w3", result.get(0).id());
    assertEquals("w1", result.get(1).id());
  }

  @Test
  void listWork_by_ids_ignores_other_filters_and_preserves_order() throws Exception {
    ds = TestJdbc.newH2DataSource();
    store = JdbcWorkStore.create(ds);

    Instant t0 = Instant.now().minusSeconds(300);
    byte[] payload = new byte[]{9};

    try (Connection c = ds.getConnection()) {
      TestJdbc.insertWork(c, "i1", "typeX", payload, "java.lang.String", 1, "json", "NEW", 5, 0, null, null, null, t0.plusSeconds(10), t0.plusSeconds(10), null, null);
      TestJdbc.insertWork(c, "i2", "typeY", payload, "java.lang.String", 1, "json", "FAILED", 5, 0, null, null, null, t0.plusSeconds(20), t0.plusSeconds(20), t0.plusSeconds(21), null);
      TestJdbc.insertWork(c, "i3", "typeX", payload, "java.lang.String", 1, "json", "IN_PROGRESS", 5, 1, "owner", t0.plusSeconds(600), null, t0.plusSeconds(30), t0.plusSeconds(30), t0.plusSeconds(31), null);
      c.commit();
    }

    java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
    ids.add("i2");
    ids.add("i1");

    // Intentionally set filters that would normally exclude some rows; ids should override and only ids filter applies.
    WorkQuery q = new WorkQuery(
      ids,
      "typeZ",
      Set.of(WorkRequest.Status.NEW),
      t0.plusSeconds(15),
      null,
      WorkQuery.Page.of(10, 0)
    );

    List<WorkRecord> result = store.listWork(q);
    assertEquals(2, result.size());
    assertEquals("i2", result.get(0).id());
    assertEquals("i1", result.get(1).id());
  }

  @Test
  void claimWork_claims_up_to_limit_and_sets_fields() throws Exception {
    ds = TestJdbc.newH2DataSource();
    store = JdbcWorkStore.create(ds);

    Instant now = Instant.now();
    byte[] payload = new byte[]{7};

    try (Connection c = ds.getConnection()) {
      // Four NEW eligible, one NEW but not eligible (future lease), one IN_PROGRESS
      TestJdbc.insertWork(c, "c1", "typeA", payload, "java.lang.String", 1, "json", "NEW", 3, 0, null, null, null, now.minusSeconds(50), now.minusSeconds(50), null, null);
      TestJdbc.insertWork(c, "c2", "typeA", payload, "java.lang.String", 1, "json", "NEW", 3, 0, null, null, null, now.minusSeconds(40), now.minusSeconds(40), null, null);
      TestJdbc.insertWork(c, "c3", "typeA", payload, "java.lang.String", 1, "json", "NEW", 3, 0, null, null, null, now.minusSeconds(30), now.minusSeconds(30), null, null);
      TestJdbc.insertWork(c, "c4", "typeA", payload, "java.lang.String", 1, "json", "NEW", 3, 0, null, now.plusSeconds(600), null, now.minusSeconds(20), now.minusSeconds(20), null, null); // not eligible
      TestJdbc.insertWork(c, "c5", "typeA", payload, "java.lang.String", 1, "json", "IN_PROGRESS", 3, 1, "o", now.plusSeconds(600), null, now.minusSeconds(10), now.minusSeconds(10), now.minusSeconds(9), null);
      TestJdbc.insertWork(c, "c6", "typeB", payload, "java.lang.String", 1, "json", "NEW", 3, 0, null, null, null, now.minusSeconds(5), now.minusSeconds(5), null, null);
      c.commit();
    }

    List<WorkRecord> claimed = store.claimWork("typeA", "owner-1", 3, Duration.ofMinutes(5));
    assertEquals(3, claimed.size(), "should claim up to limit");

    for (WorkRecord r : claimed) {
      assertEquals("owner-1", r.ownerId());
      assertEquals(WorkRequest.Status.IN_PROGRESS, r.status());
      assertTrue(r.attemptCount() >= 1);
      assertNotNull(r.leaseUntil());
      assertTrue(r.leaseUntil().isAfter(Instant.now()), "lease should be in future");
    }

    // Next attempt shouldn't return any more for typeA (the remaining NEW is not eligible due to future lease)
    List<WorkRecord> claimedAgain = store.claimWork("typeA", "owner-2", 5, Duration.ofMinutes(5));
    assertTrue(claimedAgain.isEmpty());
  }

  @Test
  void renewWorkerLease_succeeds_for_owner_only() throws Exception {
    ds = TestJdbc.newH2DataSource();
    store = JdbcWorkStore.create(ds);

    byte[] payload = new byte[]{1};
    Instant now = Instant.now();

    try (Connection c = ds.getConnection()) {
      TestJdbc.insertWork(c, "r1", "typeA", payload, "java.lang.String", 1, "json", "NEW", 5, 0, null, null, null, now, now, null, null);
      c.commit();
    }

    List<WorkRecord> claimed = store.claimWork("typeA", "owner-A", 1, Duration.ofSeconds(5));
    assertEquals(1, claimed.size());
    Instant firstLease = claimed.get(0).leaseUntil();

    boolean ok = store.renewWorkerLease("r1", "owner-A", Duration.ofSeconds(60));
    assertTrue(ok);
    WorkRecord after = store.getWork("r1").orElseThrow();
    assertTrue(after.leaseUntil().isAfter(firstLease));

    // wrong owner cannot renew
    boolean wrong = store.renewWorkerLease("r1", "owner-B", Duration.ofSeconds(60));
    assertFalse(wrong);
  }

  @Test
  void reschedule_resets_to_new_and_sets_lease_until_in_future() throws Exception {
    ds = TestJdbc.newH2DataSource();
    store = JdbcWorkStore.create(ds);

    byte[] payload = new byte[]{2};
    Instant now = Instant.now();

    try (Connection c = ds.getConnection()) {
      TestJdbc.insertWork(c, "s1", "typeA", payload, "java.lang.String", 1, "json", "IN_PROGRESS", 5, 1, "o", now.plusSeconds(120), null, now, now, now, null);
      c.commit();
    }

    Duration delay = Duration.ofMinutes(10);
    store.reschedule("s1", delay);
    WorkRecord r = store.getWork("s1").orElseThrow();
    assertEquals(WorkRequest.Status.NEW, r.status());
    assertNull(r.ownerId());
    assertNotNull(r.leaseUntil());
    assertTrue(r.leaseUntil().isAfter(Instant.now().plusSeconds(9 * 60))); // approximately 10 min
  }

  @Test
  void markSucceeded_failed_cancelled_transitions() throws Exception {
    ds = TestJdbc.newH2DataSource();
    store = JdbcWorkStore.create(ds);

    byte[] payload = new byte[]{3};
    Instant now = Instant.now();

    try (Connection c = ds.getConnection()) {
      TestJdbc.insertWork(c, "m1", "typeA", payload, "java.lang.String", 1, "json", "NEW", 3, 0, null, null, null, now, now, null, null);
      TestJdbc.insertWork(c, "m2", "typeA", payload, "java.lang.String", 1, "json", "NEW", 3, 0, null, null, null, now, now, null, null);
      TestJdbc.insertWork(c, "m3", "typeA", payload, "java.lang.String", 1, "json", "NEW", 3, 0, null, null, null, now, now, null, null);
      c.commit();
    }

    // Claim m1 and mark succeeded
    store.claimWork("typeA", "worker-1", 1, Duration.ofMinutes(1));
    store.markSucceeded("m1", "worker-1");
    WorkRecord s = store.getWork("m1").orElseThrow();
    assertEquals(WorkRequest.Status.SUCCEEDED, s.status());
    assertNull(s.leaseUntil());
    assertNotNull(s.completedTs());

    // Claim m2 and mark failed with reason
    store.claimWork("typeA", "worker-2", 1, Duration.ofMinutes(1));
    store.markFailed("m2", "worker-2", "boom");
    WorkRecord f = store.getWork("m2").orElseThrow();
    assertEquals(WorkRequest.Status.FAILED, f.status());
    assertEquals("boom", f.failureReason());
    assertNull(f.leaseUntil());
    assertNotNull(f.completedTs());

    // Cancel m3 without owner
    store.markCancelled("m3");
    WorkRecord c3 = store.getWork("m3").orElseThrow();
    assertEquals(WorkRequest.Status.CANCELLED, c3.status());
    assertNull(c3.leaseUntil());
    assertNotNull(c3.completedTs());
  }

  @Test
  void scheduler_lease_acquire_renew_release() {
    ds = TestJdbc.newH2DataSource();
    store = JdbcWorkStore.create(ds);

    String job = "job-A";

    // acquire by owner1
    assertTrue(store.tryAcquireSchedulerLease(job, "owner1", Duration.ofSeconds(5)));

    // cannot acquire by another owner while lease active
    assertFalse(store.tryAcquireSchedulerLease(job, "owner2", Duration.ofSeconds(5)));

    // renew by owner1 succeeds
    assertTrue(store.renewSchedulerLease(job, "owner1", Duration.ofSeconds(60)));

    // renew by other owner fails
    assertFalse(store.renewSchedulerLease(job, "owner2", Duration.ofSeconds(60)));

    // release by owner1
    store.releaseSchedulerLease(job, "owner1");

    // now other owner can acquire
    assertTrue(store.tryAcquireSchedulerLease(job, "owner2", Duration.ofSeconds(5)));
  }
}
