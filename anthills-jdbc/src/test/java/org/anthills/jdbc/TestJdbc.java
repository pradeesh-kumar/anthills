package org.anthills.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.UUID;

final class TestJdbc {

  static HikariDataSource newH2DataSource() {
    String dbName = "test_" + UUID.randomUUID().toString().replace("-", "");
    String url = "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1";
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(url);
    cfg.setUsername("sa");
    cfg.setPassword("");
    cfg.setMaximumPoolSize(10);
    cfg.setMinimumIdle(1);
    cfg.setAutoCommit(false);
    return new HikariDataSource(cfg);
  }

  static void closeQuietly(DataSource ds) {
    if (ds instanceof HikariDataSource hds) {
      try {
        hds.close();
      } catch (Exception ignored) {
      }
    }
  }

  static void exec(Connection conn, String sql) throws SQLException {
    try (Statement st = conn.createStatement()) {
      st.execute(sql);
    }
  }

  static boolean tableExists(Connection conn, String name) throws SQLException {
    DatabaseMetaData meta = conn.getMetaData();
    try (ResultSet rs = meta.getTables(conn.getCatalog(), null, null, new String[] {"TABLE"})) {
      while (rs.next()) {
        String t = rs.getString("TABLE_NAME");
        if (t != null && t.equalsIgnoreCase(name)) return true;
      }
      return false;
    }
  }

  static void insertWork(Connection c,
                         String id,
                         String workType,
                         byte[] payload,
                         String payloadType,
                         int payloadVersion,
                         String codec,
                         String status,
                         Integer maxRetries,
                         int attemptCount,
                         String ownerId,
                         Instant leaseUntil,
                         String failureReason,
                         Instant createdTs,
                         Instant updatedTs,
                         Instant startedTs,
                         Instant completedTs) throws SQLException {
    String sql = """
      INSERT INTO work_request (
        id, work_type, payload, payload_type, payload_version, codec,
        status, attempt_count, max_retries, owner_id, lease_until, failure_reason,
        created_ts, updated_ts, started_ts, completed_ts
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      int i = 1;
      ps.setString(i++, id);
      ps.setString(i++, workType);
      ps.setBytes(i++, payload);
      ps.setString(i++, payloadType);
      ps.setInt(i++, payloadVersion);
      ps.setString(i++, codec);
      ps.setString(i++, status);
      ps.setInt(i++, attemptCount);
      if (maxRetries == null) ps.setNull(i++, Types.INTEGER); else ps.setInt(i++, maxRetries);
      if (ownerId == null) ps.setNull(i++, Types.VARCHAR); else ps.setString(i++, ownerId);
      if (leaseUntil == null) ps.setNull(i++, Types.TIMESTAMP); else ps.setTimestamp(i++, Timestamp.from(leaseUntil));
      if (failureReason == null) ps.setNull(i++, Types.CLOB); else ps.setString(i++, failureReason);
      ps.setTimestamp(i++, Timestamp.from(createdTs));
      ps.setTimestamp(i++, Timestamp.from(updatedTs));
      if (startedTs == null) ps.setNull(i++, Types.TIMESTAMP); else ps.setTimestamp(i++, Timestamp.from(startedTs));
      if (completedTs == null) ps.setNull(i++, Types.TIMESTAMP); else ps.setTimestamp(i++, Timestamp.from(completedTs));
      ps.executeUpdate();
    }
  }

  private TestJdbc() {}
}
