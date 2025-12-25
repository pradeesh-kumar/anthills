package org.anthills.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.anthills.api.WorkQuery;
import org.anthills.api.WorkRecord;
import org.anthills.api.WorkRequest;
import org.anthills.api.WorkStore;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcWorkStore implements WorkStore {

  private final DataSource dataSource;

  private JdbcWorkStore(DataSource dataSource) {
    DbInfo dbInfo = DbInfo.detect(dataSource);
    JdbcSchemaProvider.initializeSchema(dataSource, dbInfo);
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
      return Optional.of(mapRow(rs));
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<WorkRecord> listWork(WorkQuery query) {
    Objects.requireNonNull(query, "query is required");

    ListWorkQueryBuilder b = new ListWorkQueryBuilder(query);
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

      ResultSet rs = ps.executeQuery();
      List<WorkRecord> results = new ArrayList<>();
      while (rs.next()) {
        results.add(mapRow(rs));
      }
      return results;

    } catch (SQLException e) {
      throw new RuntimeException("Failed to list work", e);
    }
  }

  @Override
  public List<WorkRecord> claimWork(String workType, String ownerId, int limit, Duration leaseDuration) {
    Instant now = now();
    Instant leaseUntil = now.plus(leaseDuration);

    String selectSql = """
        SELECT id FROM work_request
        WHERE work_type = ?
          AND status = 'NEW'
          AND (lease_until IS NULL OR lease_until < ?)
        ORDER BY created_ts
        LIMIT ?
        FOR UPDATE SKIP LOCKED
        """;

    String updateSql = """
        UPDATE work_request
        SET status = 'IN_PROGRESS',
            owner_id = ?,
            lease_until = ?,
            attempt_count = attempt_count + 1,
            started_ts = COALESCE(started_ts, ?),
            updated_ts = ?
        WHERE id = ?
        """;

    List<WorkRecord> claimed = new ArrayList<>();

    try (Connection c = getConnection();
         PreparedStatement select = c.prepareStatement(selectSql);
         PreparedStatement update = c.prepareStatement(updateSql)) {

      select.setString(1, workType);
      select.setTimestamp(2, Timestamp.from(now));
      select.setInt(3, limit);

      ResultSet rs = select.executeQuery();

      while (rs.next()) {
        String id = rs.getString(1);

        update.setString(1, ownerId);
        update.setTimestamp(2, Timestamp.from(leaseUntil));
        update.setTimestamp(3, Timestamp.from(now));
        update.setTimestamp(4, Timestamp.from(now));
        update.setString(5, id);

        update.executeUpdate();
        claimed.add(getWork(id).orElseThrow());
      }

      c.commit();
      return claimed;

    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work", e);
    }
  }


  @Override
  public boolean renewLease(String workId, String ownerId, Duration leaseDuration) {
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
    String sql = """
        INSERT INTO scheduler_lease (job_name, owner_id, lease_until)
        VALUES (?, ?, ?)
        ON CONFLICT (job_name)
        DO UPDATE SET owner_id = ?, lease_until = ?
        WHERE scheduler_lease.lease_until < ?
        """;

    try (Connection c = getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setString(1, jobName);
      ps.setString(2, ownerId);
      ps.setTimestamp(3, Timestamp.from(now.plus(leaseDuration)));
      ps.setString(4, ownerId);
      ps.setTimestamp(5, Timestamp.from(now.plus(leaseDuration)));
      ps.setTimestamp(6, Timestamp.from(now));

      int updated = ps.executeUpdate();
      c.commit();
      return updated == 1;

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

  private Connection getConnection() throws SQLException {
    Connection c = dataSource.getConnection();
    c.setAutoCommit(false);
    return c;
  }

  private Instant now() {
    return Instant.now();
  }

  private WorkRecord mapRow(ResultSet rs) throws SQLException {
    return WorkRecord.builder()
      .id(rs.getString("id"))
      .workType(rs.getString("work_type"))

      .payload(rs.getBytes("payload"))
      .payloadType(rs.getString("payload_type"))
      .payloadVersion(rs.getInt("payload_version"))
      .codec(rs.getString("codec"))

      .status(rs.getString("status"))
      .maxRetries(rs.getObject("max_retries", Integer.class))
      .attemptCount(rs.getInt("attempt_count"))
      .ownerId(rs.getString("owner_id"))
      .leaseUntil(getInstantSafely(rs, "lease_until"))

      .failureReason(rs.getString("failure_reason"))

      .createdTs(getInstantSafely(rs, "created_ts"))
      .updatedTs(getInstantSafely(rs, "updated_ts"))
      .startedTs(getInstantSafely(rs, "started_ts"))
      .completedTs(getInstantSafely(rs, "completed_ts"))
      .build();
  }

  private static Instant getInstantSafely(ResultSet rs, String column) throws SQLException {
    Timestamp ts = rs.getTimestamp(column);
    return ts != null ? ts.toInstant() : null;
  }
}
