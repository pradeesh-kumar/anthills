package org.anthills.jdbc;

import org.anthills.api.work.WorkQuery;
import org.anthills.api.work.WorkRequest;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ListWorkQueryBuilderTest {

  @Test
  void builds_sql_with_all_filters_for_h2_dialect() {
    Instant after = Instant.now().minusSeconds(3600);
    Instant before = Instant.now().plusSeconds(3600);

    WorkQuery q = new WorkQuery(
      null,
      "email",
      EnumSet.of(WorkRequest.Status.NEW, WorkRequest.Status.IN_PROGRESS),
      after,
      before,
      WorkQuery.Page.of(25, 50)
    );

    ListWorkQueryBuilder b = new ListWorkQueryBuilder(q, DbInfo.Dialect.H2);
    String sql = b.buildSql();
    List<Object> params = b.params();

    assertTrue(sql.startsWith("SELECT * FROM work_request WHERE 1=1"));
    assertTrue(sql.contains("AND work_type = ?"));
    assertTrue(sql.contains("AND status IN (?, ?)"));
    assertTrue(sql.contains("AND created_ts > ?"));
    assertTrue(sql.contains("AND created_ts < ?"));
    assertTrue(sql.endsWith(" LIMIT ? OFFSET ?"));

    // Order: work_type, status1, status2, created_after, created_before, limit, offset
    assertEquals(7, params.size());
    assertEquals("email", params.get(0));
    assertEquals("NEW", params.get(1));
    assertEquals("IN_PROGRESS", params.get(2));
    assertInstanceOf(Timestamp.class, params.get(3));
    assertInstanceOf(Timestamp.class, params.get(4));
    assertEquals(25, params.get(5));
    assertEquals(50, params.get(6));
  }

  @Test
  void empty_statuses_forces_no_results() {
    WorkQuery q = new WorkQuery(
      null,
      "x",
      EnumSet.noneOf(WorkRequest.Status.class),
      null,
      null,
      WorkQuery.Page.of(10, 0)
    );

    ListWorkQueryBuilder b = new ListWorkQueryBuilder(q, DbInfo.Dialect.H2);
    String sql = b.buildSql();
    List<Object> params = b.params();

    assertTrue(sql.contains("AND 1=0"));
    assertTrue(sql.contains("LIMIT ? OFFSET ?"));
    assertEquals(List.of("x", 10, 0), params);
  }

  @Test
  void uses_mssql_pagination_syntax_when_dialect_mssql() {
    WorkQuery q = new WorkQuery(
      null,
      null,
      null,
      null,
      null,
      WorkQuery.Page.of(5, 15)
    );

    ListWorkQueryBuilder b = new ListWorkQueryBuilder(q, DbInfo.Dialect.MSSQL);
    String sql = b.buildSql();
    List<Object> params = b.params();

    assertTrue(sql.contains(" ORDER BY created_ts DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY"));
    assertEquals(List.of(15, 5), params);
  }
}
