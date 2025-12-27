package org.anthills.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

final class JdbcSchemaProviderTest {

  private HikariDataSource ds;

  @AfterEach
  void tearDown() {
    TestJdbc.closeQuietly(ds);
    // ensure other tests can re-initialize schema on fresh DBs without interference
    JdbcSchemaProvider.schemaInitializedDataSources.clear();
  }

  @Test
  void initializeSchema_creates_required_tables_once_for_h2() throws Exception {
    ds = TestJdbc.newH2DataSource();
    DbInfo info = DbInfo.detect(ds);

    assertEquals(DbInfo.Dialect.H2, info.dialect());

    // first time should create schema
    JdbcSchemaProvider.initializeSchema(ds, info);

    try (Connection c = ds.getConnection()) {
      assertTrue(TestJdbc.tableExists(c, "work_request"), "work_request should exist");
      assertTrue(TestJdbc.tableExists(c, "scheduler_lease"), "scheduler_lease should exist");
    }

    // second time should be a no-op and not throw
    JdbcSchemaProvider.initializeSchema(ds, info);

    // identity should be recorded to avoid duplicate initialization
    assertTrue(JdbcSchemaProvider.schemaInitializedDataSources.contains(info.identity()));
  }
}
