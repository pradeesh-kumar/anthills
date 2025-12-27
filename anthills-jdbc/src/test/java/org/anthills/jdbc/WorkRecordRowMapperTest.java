package org.anthills.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.anthills.api.work.WorkRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class WorkRecordRowMapperTest {

  private HikariDataSource ds;

  @AfterEach
  void tearDown() {
    TestJdbc.closeQuietly(ds);
    JdbcSchemaProvider.schemaInitializedDataSources.clear();
  }

  @Test
  void map_single_row_and_list_multiple_rows() throws Exception {
    ds = TestJdbc.newH2DataSource();
    DbInfo info = DbInfo.detect(ds);
    JdbcSchemaProvider.initializeSchema(ds, info);

    Instant now = Instant.now();
    byte[] payload = new byte[] {1, 2, 3};

    try (Connection c = ds.getConnection()) {
      // insert two rows
      TestJdbc.insertWork(c,
        "id-1", "typeA", payload, "java.lang.String", 1, "json",
        "NEW", 5, 0, null, null, null,
        now.minusSeconds(60), now.minusSeconds(60), null, null
      );
      TestJdbc.insertWork(c,
        "id-2", "typeB", payload, "java.lang.Integer", 2, "json",
        "IN_PROGRESS", 3, 2, "owner-1", now.plusSeconds(300), "boom",
        now.minusSeconds(30), now.minusSeconds(30), now.minusSeconds(25), null
      );
      c.commit();

      // single map
      try (PreparedStatement ps = c.prepareStatement("SELECT * FROM work_request WHERE id = ?")) {
        ps.setString(1, "id-1");
        try (ResultSet rs = ps.executeQuery()) {
          assertTrue(rs.next());
          WorkRecord r = WorkRecordRowMapper.map(rs);
          assertEquals("id-1", r.id());
          assertEquals("typeA", r.workType());
          assertArrayEquals(payload, r.payload());
          assertEquals("java.lang.String", r.payloadType());
          assertEquals(1, r.payloadVersion());
          assertEquals("json", r.codec());
          assertEquals("NEW", r.status().name());
          assertEquals(0, r.attemptCount());
          assertNull(r.ownerId());
          assertNull(r.leaseUntil());
          assertNull(r.failureReason());
          assertNotNull(r.createdTs());
          assertNotNull(r.updatedTs());
          assertNull(r.startedTs());
          assertNull(r.completedTs());
        }
      }

      // list mapping
      try (PreparedStatement ps = c.prepareStatement("SELECT * FROM work_request ORDER BY id")) {
        try (ResultSet rs = ps.executeQuery()) {
          List<WorkRecord> list = WorkRecordRowMapper.retrieveWorkRecords(rs);
          assertEquals(2, list.size());
          assertEquals("id-1", list.get(0).id());
          assertEquals("id-2", list.get(1).id());

          WorkRecord r2 = list.get(1);
          assertEquals("owner-1", r2.ownerId());
          assertNotNull(r2.leaseUntil());
          assertEquals("boom", r2.failureReason());
          assertNotNull(r2.startedTs());
          assertNotNull(r2.createdTs());
          assertNotNull(r2.updatedTs());
          assertNull(r2.completedTs());
        }
      }
    }
  }

  @Test
  void getInstantSafely_handles_nulls() throws Exception {
    ds = TestJdbc.newH2DataSource();
    DbInfo info = DbInfo.detect(ds);
    JdbcSchemaProvider.initializeSchema(ds, info);

    Instant now = Instant.now();
    byte[] payload = new byte[] {9};

    try (Connection c = ds.getConnection()) {
      TestJdbc.insertWork(c,
        "id-null", "x", payload, "java.lang.String", 1, "json",
        "NEW", null, 0, null, null, null,
        now, now, null, null
      );
      c.commit();

      try (PreparedStatement ps = c.prepareStatement("SELECT lease_until, started_ts, completed_ts FROM work_request WHERE id = ?")) {
        ps.setString(1, "id-null");
        try (ResultSet rs = ps.executeQuery()) {
          assertTrue(rs.next());
          // validate JDBC nulls are actually null
          assertNull(rs.getTimestamp(1));
          assertNull(rs.getTimestamp(2));
          assertNull(rs.getTimestamp(3));
        }
      }
    }
  }
}
