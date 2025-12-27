package org.anthills.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.anthills.api.work.WorkQuery;
import org.anthills.api.work.WorkRecord;
import org.anthills.api.work.WorkRequest;
import org.anthills.api.work.WorkStore;
import org.anthills.jdbc.util.IdGenerator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class JdbcWorkStore implements WorkStore {

  private final DataSource dataSource;
  private final DbInfo dbInfo;

  private JdbcWorkStore(DataSource dataSource) {
    DbInfo dbInfo = DbInfo.detect(dataSource);
    JdbcSchemaProvider.initializeSchema(dataSource, dbInfo);
    this.dbInfo = dbInfo;
    this.dataSource = dataSource;
  }

  public static JdbcWorkStore create(DataSource dataSource) {
    return new JdbcWorkStore(dataSource);
  }

  public static JdbcWorkStore create(JdbcSettings jdbcSettings) {
    Objects.requireNonNull(jdbcSettings, "jdbcSettings must not be null");
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(jdbcSettings.jdbcUrl());
    hikariConfig.setUsername(jdbcSettings.username());
    hikariConfig.setPassword(jdbcSettings.password());
    hikariConfig.setMaximumPoolSize(jdbcSettings.maxPoolSize());
    hikariConfig.setMinimumIdle(jdbcSettings.minIdleConnections());
    hikariConfig.setAutoCommit(false);
    hikariConfig.setConnectionTimeout(jdbcSettings.connectionTimeoutMs());
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "10");
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
    return create(new HikariDataSource(hikariConfig));
  }

  @Override
  public WorkRecord createWork(String workType, byte[] payload, int payloadVersion, String codec, Integer maxRetries) {
    String id = IdGenerator.generateRandomId();
    Instant now = now();

    String sql = """
      INSERT INTO work_request (
          id, work_type, payload, payload_version, codec,
          status, attempt_count, max_retries,
          created_ts, updated_ts
      )
      VALUES (?, ?, ?, ?, ?, 'NEW', 0, ?, ?, ?)
      """;

    try (Connection c = getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, id);
      ps.setString(2, workType);
      ps.setBytes(3, payload);
      ps.setInt(4, payloadVersion);
      ps.setString(5, codec);
      ps.setObject(6, maxRetries);
      ps.setTimestamp(7, Timestamp.from(now));
      ps.setTimestamp(8, Timestamp.from(now));
      ps.executeUpdate();
      c.commit();
      return getWork(id).orElseThrow();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create work", e);
    }
  }

  @Override
  public Optional<WorkRecord> getWork(String workId) {
    String sql = "SELECT * FROM work_request WHERE id = ?";

    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, workId);
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) return Optional.empty();
      return Optional.of(WorkRecordRowMapper.map(rs));
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private List<WorkRecord> getWorkByIds(Connection c, List<String> ids) {
    List<WorkRecord> ordered = new ArrayList<>();
    if (ids.isEmpty()) return ordered;

    StringBuilder sb = new StringBuilder("SELECT * FROM work_request WHERE id IN (");
    for (int i = 0; i < ids.size(); i++) {
      if (i > 0) sb.append(", ");
      sb.append("?");
    }
    sb.append(")");

    try (PreparedStatement ps = c.prepareStatement(sb.toString())) {
      for (int i = 0; i < ids.size(); i++) {
        ps.setString(i + 1, ids.get(i));
      }
      List<WorkRecord> rows = WorkRecordRowMapper.retrieveWorkRecords(ps.executeQuery());
      Map<String, WorkRecord> byId = new HashMap<>();
      for (WorkRecord r : rows) {
        byId.put(r.id(), r);
      }
      for (String id : ids) {
        WorkRecord r = byId.get(id);
        if (r != null) ordered.add(r);
      }
      return ordered;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<WorkRecord> listWork(WorkQuery query) {
    Objects.requireNonNull(query, "query is required");

    // If ids are provided, fetch by ids and return immediately (ignore other filters)
    if (query.ids() != null && !query.ids().isEmpty()) {
      try (Connection c = dataSource.getConnection()) {
        return getWorkByIds(c, new ArrayList<>(query.ids()));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to list work by ids", e);
      }
    }

    ListWorkQueryBuilder b = new ListWorkQueryBuilder(query, dbInfo.dialect());
    String sql = b.buildSql();
    List<Object> params = b.params();

    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

      int idx = 1;
      for (Object p : params) {
        if (p instanceof Timestamp ts) {
          ps.setTimestamp(idx++, ts);
        } else if (p instanceof Integer i) {
          ps.setInt(idx++, i);
        } else if (p instanceof String s) {
          ps.setString(idx++, s);
        } else {
          ps.setObject(idx++, p);
        }
      }
      return WorkRecordRowMapper.retrieveWorkRecords(ps.executeQuery());
    } catch (SQLException e) {
      throw new RuntimeException("Failed to list work", e);
    }
  }

  @Override
  public List<WorkRecord> claimWork(String workType, String ownerId, int limit, Duration leaseDuration) {
    Instant now = now();
    Instant leaseUntil = now.plus(leaseDuration);

    String selectSql = buildSelectIdsForClaimSql();
    String updateSql = """
      UPDATE work_request
      SET status = 'IN_PROGRESS',
          owner_id = ?,
          lease_until = ?,
          attempt_count = attempt_count + 1,
          started_ts = COALESCE(started_ts, ?),
          updated_ts = ?
      WHERE id = ?
        AND status = 'NEW'
        AND (owner_id IS NULL OR lease_until < ?)
      """;

    List<String> claimedIds = new ArrayList<>();

    try (Connection c = getConnection();
         PreparedStatement select = c.prepareStatement(selectSql);
         PreparedStatement update = c.prepareStatement(updateSql)) {

      int sIdx = 1;
      select.setString(sIdx++, workType);
      select.setTimestamp(sIdx++, Timestamp.from(now));
      // limit param position depends on dialect but buildSelectIdsForClaimSql always places it as last placeholder
      select.setInt(sIdx, limit);

      ResultSet rs = select.executeQuery();

      List<String> selectedIds = new ArrayList<>();
      while (rs.next()) {
        String id = rs.getString(1);
        selectedIds.add(id);

        int uIdx = 1;
        update.setString(uIdx++, ownerId);
        update.setTimestamp(uIdx++, Timestamp.from(leaseUntil));
        update.setTimestamp(uIdx++, Timestamp.from(now));
        update.setTimestamp(uIdx++, Timestamp.from(now));
        update.setString(uIdx++, id);
        update.setTimestamp(uIdx, Timestamp.from(now));

        update.addBatch();
      }

      int[] counts = update.executeBatch();
      for (int i = 0; i < counts.length; i++) {
        int count = counts[i];
        if (count == 1 || count == java.sql.Statement.SUCCESS_NO_INFO) {
          claimedIds.add(selectedIds.get(i));
        }
      }

      List<WorkRecord> claimed = getWorkByIds(c, claimedIds);
      c.commit();
      return claimed;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work", e);
    }
  }

  @Override
  public boolean renewWorkerLease(String workId, String ownerId, Duration leaseDuration) {
    String sql = """
      UPDATE work_request
      SET lease_until = ?, updated_ts = ?
      WHERE id = ?
        AND owner_id = ?
        AND status = 'IN_PROGRESS'
      """;

    Instant now = now();
    try (Connection c = getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setTimestamp(1, Timestamp.from(now.plus(leaseDuration)));
      ps.setTimestamp(2, Timestamp.from(now));
      ps.setString(3, workId);
      ps.setString(4, ownerId);

      int updated = ps.executeUpdate();
      c.commit();
      return updated == 1;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void reschedule(String id, Duration delay) {
    Instant now = now();
    String sql = """
      UPDATE work_request
      SET status = 'NEW',
          owner_id = NULL,
          lease_until = ?,
          updated_ts = ?
      WHERE id = ?
      """;
    try (Connection c = getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setTimestamp(1, Timestamp.from(now.plus(delay)));
      ps.setTimestamp(2, Timestamp.from(now));
      ps.setString(3, id);
      ps.executeUpdate();
      c.commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void markSucceeded(String workId, String ownerId) {
    updateTerminal(workId, ownerId, WorkRequest.Status.SUCCEEDED, null);
  }

  @Override
  public void markFailed(String workId, String ownerId, String reason) {
    updateTerminal(workId, ownerId, WorkRequest.Status.FAILED, reason);
  }

  private void updateTerminal(String id, String ownerId, WorkRequest.Status status, String reason) {
    String sql = """
      UPDATE work_request
      SET status = ?, failure_reason = ?, lease_until = NULL,
          completed_ts = ?, updated_ts = ?
      WHERE id = ? AND owner_id = ?
      """;

    Instant now = now();

    try (Connection c = getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setString(1, status.name());
      ps.setString(2, reason);
      ps.setTimestamp(3, Timestamp.from(now));
      ps.setTimestamp(4, Timestamp.from(now));
      ps.setString(5, id);
      ps.setString(6, ownerId);

      ps.executeUpdate();
      c.commit();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void markCancelled(String id) {
    String sql = """
      UPDATE work_request
      SET status = ?, lease_until = NULL,
          completed_ts = ?, updated_ts = ?
      WHERE id = ?
      """;
    Instant now = now();
    try (Connection c = getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, WorkRequest.Status.CANCELLED.name());
      ps.setTimestamp(2, Timestamp.from(now));
      ps.setTimestamp(3, Timestamp.from(now));
      ps.setString(4, id);
      ps.executeUpdate();
      c.commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean tryAcquireSchedulerLease(String jobName, String ownerId, Duration leaseDuration) {
    Instant now = now();

    String updateSql = """
      UPDATE scheduler_lease
      SET owner_id = ?, lease_until = ?
      WHERE job_name = ? AND lease_until < ?
      """;

    String insertSql = """
      INSERT INTO scheduler_lease (job_name, owner_id, lease_until)
      VALUES (?, ?, ?)
      """;

    try (Connection c = getConnection()) {

      try (PreparedStatement up = c.prepareStatement(updateSql)) {
        up.setString(1, ownerId);
        up.setTimestamp(2, Timestamp.from(now.plus(leaseDuration)));
        up.setString(3, jobName);
        up.setTimestamp(4, Timestamp.from(now));
        int updated = up.executeUpdate();
        if (updated == 1) {
          c.commit();
          return true;
        }
      }

      try (PreparedStatement ins = c.prepareStatement(insertSql)) {
        ins.setString(1, jobName);
        ins.setString(2, ownerId);
        ins.setTimestamp(3, Timestamp.from(now.plus(leaseDuration)));
        ins.executeUpdate();
        c.commit();
        return true;
      } catch (SQLException se) {
        if (isDuplicateKey(se)) {
          c.rollback();
          return false; // someone else holds the lease
        }
        c.rollback();
        throw se;
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean renewSchedulerLease(String jobName, String ownerId, Duration leaseDuration) {
    String sql = """
      UPDATE scheduler_lease
      SET lease_until = ?
      WHERE job_name = ? AND owner_id = ?
      """;

    try (Connection c = getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setTimestamp(1, Timestamp.from(now().plus(leaseDuration)));
      ps.setString(2, jobName);
      ps.setString(3, ownerId);

      int updated = ps.executeUpdate();
      c.commit();
      return updated == 1;

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void releaseSchedulerLease(String jobName, String ownerId) {
    String sql = """
      DELETE FROM scheduler_lease
      WHERE job_name = ? AND owner_id = ?
      """;

    try (Connection c = getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setString(1, jobName);
      ps.setString(2, ownerId);
      ps.executeUpdate();
      c.commit();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private String buildSelectIdsForClaimSql() {
    // Build a SELECT of candidate ids that avoids waiting on locked rows where supported.
    // Parameter order is always: work_type, now_ts, limit (last placeholder).
    DbInfo.Dialect d = dbInfo.dialect();

    if (d == DbInfo.Dialect.PostgresSQL) {
      // Lock and skip locked rows to avoid waiting; keep limit last
      return """
        SELECT id FROM work_request
        WHERE work_type = ?
          AND status = 'NEW'
          AND (lease_until IS NULL OR lease_until < ?)
        ORDER BY created_ts
        LIMIT ? FOR UPDATE SKIP LOCKED
        """;
    } else if (d == DbInfo.Dialect.MySQL) {
      // MySQL 8+: SKIP LOCKED supported with FOR UPDATE
      return """
        SELECT id FROM work_request
        WHERE work_type = ?
          AND status = 'NEW'
          AND (lease_until IS NULL OR lease_until < ?)
        ORDER BY created_ts
        LIMIT ? FOR UPDATE SKIP LOCKED
        """;
    } else if (d == DbInfo.Dialect.MSSQL) {
      // SQL Server: READPAST skips locked rows; UPDLOCK/ROWLOCK to take update locks during selection
      return """
        SELECT id FROM work_request WITH (READPAST, UPDLOCK, ROWLOCK)
        WHERE work_type = ?
          AND status = 'NEW'
          AND (lease_until IS NULL OR lease_until < ?)
        ORDER BY created_ts
        OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY
        """;
    } else if (d == DbInfo.Dialect.Oracle) {
      // Oracle: use inline view for ORDER BY + ROWNUM limiting; lock and skip locked
      return """
        SELECT id FROM (
          SELECT id FROM work_request
          WHERE work_type = ?
            AND status = 'NEW'
            AND (lease_until IS NULL OR lease_until < ?)
          ORDER BY created_ts
        )
        WHERE ROWNUM <= ? FOR UPDATE SKIP LOCKED
        """;
    } else {
      // DB2/Sqlite/H2/Unknown: simple limit/fetch without locking hints
      String base = """
        SELECT id FROM work_request
        WHERE work_type = ?
          AND status = 'NEW'
          AND (lease_until IS NULL OR lease_until < ?)
        ORDER BY created_ts
        """;
      if (d == DbInfo.Dialect.DB2) {
        return base + " FETCH FIRST ? ROWS ONLY";
      } else {
        // H2, Sqlite and others
        return base + " LIMIT ?";
      }
    }
  }

  private boolean isDuplicateKey(SQLException e) {
    String state = e.getSQLState();
    int code = e.getErrorCode();
    String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

    // Postgres unique_violation
    if ("23505".equals(state)) return true;
    // MySQL duplicate entry
    if ("23000".equals(state) && code == 1062) return true;
    // SQLite constraint violation typically 19 or SQLState "23000"
    if ("23000".equals(state) || code == 19) return msg.contains("constraint");
    // SQL Server 2627 (unique constraint), 2601 (duplicate key)
    if (code == 2627 || code == 2601) return true;
    // Oracle ORA-00001
    if (code == 1) return true;
    // DB2 23505
    if ("23505".equals(state)) return true;

    return msg.contains("duplicate") || msg.contains("unique constraint") || msg.contains("already exists");
  }

  private Connection getConnection() throws SQLException {
    Connection c = dataSource.getConnection();
    c.setAutoCommit(false);
    return c;
  }

  private Instant now() {
    return Instant.now();
  }
}
