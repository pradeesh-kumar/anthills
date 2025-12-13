package org.anthills.core;

import org.anthills.commons.WorkRequest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.anthills.core.utils.Utils.isEmpty;

public interface WorkItemClaimer {

  <T> List<WorkRequest<T>> claim(ClaimRequest<T> request);

  record ClaimRequest<T>(
    Class<T> payloadType,
    Set<WorkRequest.Status> statuses,
    String owner,
    Duration leasePeriod,
    int limit) {

    public static <T> Builder<T> builder() {
      return new Builder<>();
    }

    public static class Builder<T> {
      private Class<T> payloadType;
      private Set<WorkRequest.Status> statuses;
      private String owner;
      private Duration leasePeriod;
      private int limit = 1;

      public Builder<T> payloadType(Class<T> payloadType) {
        this.payloadType = payloadType;
        return this;
      }

      public Builder<T> statuses(Set<WorkRequest.Status> statuses) {
        this.statuses = statuses;
        return this;
      }

      public Builder<T> owner(String owner) {
        this.owner = owner;
        return this;
      }

      public Builder<T> leasePeriod(Duration leasePeriod) {
        this.leasePeriod = leasePeriod;
        return this;
      }

      public Builder<T> limit(int limit) {
        this.limit = limit;
        return this;
      }

      public ClaimRequest<T> build() {
        validate();
        return new ClaimRequest<>(payloadType, statuses, owner, leasePeriod, limit);
      }

      private void validate() {
        Objects.requireNonNull(payloadType, "payloadType is required");
        Objects.requireNonNull(statuses, "statuses is required");
        Objects.requireNonNull(owner, "owner is required");
        Objects.requireNonNull(leasePeriod, "leasePeriod is required");
        if (limit <= 0) {
          throw new IllegalArgumentException("limit must be greater than 0");
        }
      }
    }
  }
}

class WorkItemClaimerFactory {
  public static WorkItemClaimer getClaimerFor(DbInfo db) {
    return switch (db.dialect()) {
      case PostgresSQL -> new PostgresClaimer();
      case Oracle -> new OracleClaimer();
      case MySQL -> new MySqlClaimer();
      case H2 -> new H2Claimer();
      case MSSQL -> new MsSqlClaimer();
      case DB2 -> new Db2Claimer();
      case Sqlite -> new SqliteClaimer();
      default -> throw new IllegalArgumentException("Cannot create claimer for Unknown dialect: " + db.dialect());
    };
  }
}

class ClaimQuerySupport {

  public static String buildWhereClause(WorkItemClaimer.ClaimRequest<?> req, String nowExpression) {
    StringBuilder sb = new StringBuilder();
    sb.append(" WHERE payload_class=? ");
    if (isEmpty(req.statuses())) {
      sb.append(" AND status IN (");
      String joined = req.statuses().stream().map(s -> "?").collect(Collectors.joining(", "));
      sb.append(joined);
    }
    sb.append(" AND (owner IS NULL OR lease_until < ").append(nowExpression).append(") ");
    return sb.toString();
  }

  public static <T> List<WorkRequest<T>> retrieveResults(PreparedStatement ps, WorkItemClaimer.ClaimRequest<T> req) throws SQLException {
    try (var rs = ps.executeQuery()) {
      return retrieveResults(rs, req);
    }
  }

  public static <T> List<WorkRequest<T>> retrieveResults(ResultSet rs, WorkItemClaimer.ClaimRequest<T> req) throws SQLException {
    List<WorkRequest<T>> results = new ArrayList<>();
    while (rs.next()) {
      results.add(
        WorkRequestMapper.map(rs, req.payloadType())
      );
    }
    return results;
  }
}

class PostgresClaimer implements WorkItemClaimer {

  @Override
  public <T> List<WorkRequest<T>> claim(ClaimRequest<T> req) {
    String where = ClaimQuerySupport.buildWhereClause(req, "now()");
    int statusCount = req.statuses() == null ? 0 : req.statuses().size();

    String sql = """
      WITH candidate AS (
          SELECT ctid
          FROM work_request
      """ + where + """
          FOR UPDATE SKIP LOCKED
          LIMIT ?
      )
      UPDATE work_request wr
      SET owner = ?,
          lease_until = now() + (? || ' seconds')::interval
      FROM candidate c
      WHERE wr.ctid = c.ctid
      RETURNING wr.*;
      """;

    var con = TransactionContext.get();
    try (var ps = con.prepareStatement(sql)) {
      int idx = 1;
      // 1. payload type (payload_class)
      ps.setString(idx++, req.payloadType().getName());

      // 2. statuses
      if (statusCount > 0) {
        for (var st : req.statuses()) {
          ps.setString(idx++, st.name());
        }
      }

      // 3. limit
      ps.setInt(idx++, req.limit());

      // 4. owner
      ps.setString(idx++, req.owner());

      // 5. lease seconds
      ps.setLong(idx++, req.leasePeriod().getSeconds());

      return ClaimQuerySupport.retrieveResults(ps, req);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }
  }
}

class MySqlClaimer implements WorkItemClaimer {

  private static final String SELECT_IDS = """
    SELECT id
    FROM work_request
    %s
    FOR UPDATE SKIP LOCKED
    LIMIT ?
    """;

  // UPDATE requires explicit column list in RETURNING for MySQL
  private static final String UPDATE_TEMPLATE = """
    UPDATE work_request
    SET owner = ?,
        lease_until = DATE_ADD(NOW(), INTERVAL ? SECOND)
    WHERE id IN (%s)
    RETURNING id, payload_class, payload, status, details, max_retries, owner, lease_until, created_ts, updated_ts, started_ts, completed_ts
    """;

  @Override
  public <T> List<WorkRequest<T>> claim(ClaimRequest<T> req) {
    String where = ClaimQuerySupport.buildWhereClause(req, "NOW()");
    String selectSql = String.format(SELECT_IDS, where);

    List<String> ids = new ArrayList<>();

    var con = TransactionContext.get();
    try (var ps = con.prepareStatement(selectSql)) {

      int idx = 1;
      ps.setString(idx++, req.payloadType().getName());
      if (req.statuses() != null && !req.statuses().isEmpty()) {
        for (var s : req.statuses()) ps.setString(idx++, s.name());
      }
      ps.setInt(idx, req.limit());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) ids.add(rs.getString(1));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }

    if (ids.isEmpty()) return List.of();

    String inClause = ids.stream().map(i -> "?").collect(Collectors.joining(","));
    String updateSql = String.format(UPDATE_TEMPLATE, inClause);

    try (PreparedStatement ps = con.prepareStatement(updateSql)) {
      int idx = 1;
      ps.setString(idx++, req.owner());
      ps.setLong(idx++, req.leasePeriod().getSeconds());
      for (String id : ids) ps.setString(idx++, id);
      return ClaimQuerySupport.retrieveResults(ps, req);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }
  }
}

class OracleClaimer implements WorkItemClaimer {

  private static final String SELECT_IDS = """
    SELECT id
    FROM work_request
    %s
    FOR UPDATE SKIP LOCKED
    FETCH FIRST ? ROWS ONLY
    """;

  private static final String UPDATE_TEMPLATE = """
    UPDATE work_request
    SET owner = ?,
        lease_until = SYSTIMESTAMP + NUMTODSINTERVAL(?, 'SECOND')
    WHERE id IN (%s)
    """;

  private static final String RETURN_TEMPLATE = """
    SELECT id, payload_class, payload, status, details, max_retries, owner, lease_until, created_ts, updated_ts, started_ts, completed_ts
    FROM work_request
    WHERE id IN (%s)
    """;

  @Override
  public <T> List<WorkRequest<T>> claim(ClaimRequest<T> req) {
    String where = ClaimQuerySupport.buildWhereClause(req, "SYSTIMESTAMP");
    String selectSql = String.format(SELECT_IDS, where);

    List<Long> ids = new ArrayList<>();
    var con = TransactionContext.get();
    try (var ps = con.prepareStatement(selectSql)) {
      int idx = 1;
      ps.setString(idx++, req.payloadType().getName());
      if (req.statuses() != null && !req.statuses().isEmpty()) {
        for (var s : req.statuses()) ps.setString(idx++, s.name());
      }
      ps.setInt(idx, req.limit());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) ids.add(rs.getLong(1));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }

    if (ids.isEmpty()) return List.of();

    String inClause = ids.stream().map(x -> "?").collect(Collectors.joining(","));
    String updateSql = String.format(UPDATE_TEMPLATE, inClause);
    try (var ps = con.prepareStatement(updateSql)) {
      int idx = 1;
      ps.setString(idx++, req.owner());
      ps.setLong(idx++, req.leasePeriod().getSeconds());
      for (Long id : ids) ps.setLong(idx++, id);

      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }

    String returnSql = String.format(RETURN_TEMPLATE, inClause);
    try (var ps = con.prepareStatement(returnSql)) {
      int idx = 1;
      for (Long id : ids) ps.setLong(idx++, id);

      return ClaimQuerySupport.retrieveResults(ps, req);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }
  }
}

class MsSqlClaimer implements WorkItemClaimer {

  private static final String SQL = """
    WITH candidate AS (
        SELECT TOP (?) *
        FROM work_request WITH (ROWLOCK, UPDLOCK, READPAST)
        %s
        ORDER BY id
    )
    UPDATE candidate
    SET owner = ?,
        lease_until = DATEADD(SECOND, ?, SYSDATETIME())
    OUTPUT inserted.id, inserted.payload_class, inserted.payload, inserted.status, inserted.details, inserted.max_retries, inserted.owner, inserted.lease_until, inserted.created_ts, inserted.updated_ts, inserted.started_ts, inserted.completed_ts;
    """;

  @Override
  public <T> List<WorkRequest<T>> claim(ClaimRequest<T> req) {
    String where = ClaimQuerySupport.buildWhereClause(req, "SYSDATETIME()");
    String sql = String.format(SQL, where);

    var con = TransactionContext.get();
    try (var ps = con.prepareStatement(sql)) {
      int idx = 1;
      ps.setInt(idx++, req.limit());

      // payload_class
      ps.setString(idx++, req.payloadType().getName());
      if (req.statuses() != null && !req.statuses().isEmpty()) {
        for (var s : req.statuses()) ps.setString(idx++, s.name());
      }

      // now we've filled candidate's placeholders, now owner + lease
      ps.setString(idx++, req.owner());
      ps.setLong(idx++, req.leasePeriod().getSeconds());

      return ClaimQuerySupport.retrieveResults(ps, req);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }
  }
}

class Db2Claimer implements WorkItemClaimer {

  private static final String SQL = """
    WITH candidate AS (
        SELECT id
        FROM work_request
        %s
        FOR UPDATE WITH RS SKIP LOCKED
        FETCH FIRST ? ROWS ONLY
    )
    SELECT *
    FROM FINAL TABLE (
        UPDATE work_request
        SET owner = ?,
            lease_until = CURRENT TIMESTAMP + ? SECONDS
        WHERE id IN (SELECT id FROM candidate)
    );
    """;

  @Override
  public <T> List<WorkRequest<T>> claim(ClaimRequest<T> req) {
    String where = ClaimQuerySupport.buildWhereClause(req, "CURRENT TIMESTAMP");
    String sql = String.format(SQL, where);

    var con = TransactionContext.get();
    try (var ps = con.prepareStatement(sql)) {
      int idx = 1;
      // candidate: payload_class + statuses...
      ps.setString(idx++, req.payloadType().getName());
      if (req.statuses() != null && !req.statuses().isEmpty()) {
        for (var s : req.statuses()) ps.setString(idx++, s.name());
      }

      // candidate FETCH FIRST ? ROWS ONLY
      ps.setInt(idx++, req.limit());

      // update params
      ps.setString(idx++, req.owner());
      ps.setLong(idx++, req.leasePeriod().getSeconds());

      return ClaimQuerySupport.retrieveResults(ps, req);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }
  }
}

class H2Claimer implements WorkItemClaimer {

  private static final String SQL = """
    WITH candidate AS (
        SELECT ctid
        FROM work_request
        %s
        FOR UPDATE SKIP LOCKED
        LIMIT ?
    )
    UPDATE work_request wr
    SET owner = ?,
        lease_until = DATEADD('SECOND', ?, NOW())
    FROM candidate c
    WHERE wr.ctid = c.ctid
    RETURNING wr.*;
    """;

  @Override
  public <T> List<WorkRequest<T>> claim(ClaimRequest<T> req) {
    String where = ClaimQuerySupport.buildWhereClause(req, "NOW()");
    String sql = String.format(SQL, where);

    var con = TransactionContext.get();
    try (var ps = con.prepareStatement(sql)) {
      int idx = 1;
      ps.setString(idx++, req.payloadType().getName());
      if (req.statuses() != null && !req.statuses().isEmpty()) {
        for (var s : req.statuses()) ps.setString(idx++, s.name());
      }

      ps.setInt(idx++, req.limit());
      ps.setString(idx++, req.owner());
      ps.setLong(idx++, req.leasePeriod().getSeconds());

      return ClaimQuerySupport.retrieveResults(ps, req);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }
  }
}

class SqliteClaimer implements WorkItemClaimer {

  private static final String SELECT_IDS = """
    SELECT id
    FROM work_request
    %s
    ORDER BY id
    LIMIT ?
    """;

  private static final String UPDATE_TEMPLATE = """
    UPDATE work_request
    SET owner = ?,
        lease_until = DATETIME('now', ? || ' seconds')
    WHERE id IN (%s)
      AND (owner IS NULL OR lease_until < CURRENT_TIMESTAMP)
    """;

  private static final String RETURN_TEMPLATE = """
    SELECT id, payload_class, payload, status, details, max_retries, owner, lease_until, created_ts, updated_ts, started_ts, completed_ts
    FROM work_request
    WHERE id IN (%s)
    """;

  @Override
  public <T> List<WorkRequest<T>> claim(ClaimRequest<T> req) {
    String where = ClaimQuerySupport.buildWhereClause(req, "CURRENT_TIMESTAMP");
    String selectSql = String.format(SELECT_IDS, where);

    List<String> ids = new ArrayList<>();
    var con = TransactionContext.get();
    try (var ps = con.prepareStatement(selectSql)) {
      int idx = 1;
      ps.setString(idx++, req.payloadType().getName());
      if (req.statuses() != null && !req.statuses().isEmpty()) {
        for (var s : req.statuses()) ps.setString(idx++, s.name());
      }
      ps.setInt(idx, req.limit());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) ids.add(rs.getString(1));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }

    if (ids.isEmpty()) return List.of();

    String inClause = ids.stream().map(i -> "?").collect(Collectors.joining(","));
    String updateSql = String.format(UPDATE_TEMPLATE, inClause);

    int updated;
    try (var ps = con.prepareStatement(updateSql)) {
      int idx = 1;
      ps.setString(idx++, req.owner());
      ps.setLong(idx++, req.leasePeriod().getSeconds());
      for (String id : ids) ps.setString(idx++, id);
      updated = ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }

    if (updated == 0) return List.of();

    String returnSql = String.format(RETURN_TEMPLATE, inClause);
    try (var ps = con.prepareStatement(returnSql)) {

      int idx = 1;
      for (String id : ids) ps.setString(idx++, id);
      return ClaimQuerySupport.retrieveResults(ps, req);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim work requests", e);
    }
  }
}
