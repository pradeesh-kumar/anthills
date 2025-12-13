package org.anthills.core;

import com.google.gson.Gson;
import org.anthills.commons.WorkRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.anthills.core.utils.Utils.getInstantSafely;

public class WorkRequestJdbcRepository implements WorkRequestRepository {

  private final Gson gson = new Gson();

  @Override
  public <T> WorkRequest<T> create(WorkRequest<T> wr) {
    String sql = """
      INSERT INTO work_request
      (id, payload_class, payload, status, details, max_retries, attempts,
       owner, lease_until, created_ts, updated_ts)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;
    Connection con = TransactionContext.get();
    try (var stmt = con.prepareStatement(sql)) {
      int id = 1;
      stmt.setString(id++, wr.id());
      stmt.setString(id++, wr.payloadClass());
      stmt.setString(id++, gson.toJson(wr.payload()));
      stmt.setString(id++, wr.status().name());
      stmt.setString(id++, wr.details());
      stmt.setInt(id++, wr.maxRetries());
      stmt.setInt(id++, wr.attempts());
      stmt.setString(id++, wr.owner());
      stmt.setTimestamp(id++, getInstantSafely(wr.leaseUntil()));
      stmt.setTimestamp(id++, getInstantSafely(wr.createdTs()));
      stmt.setTimestamp(id++, getInstantSafely(wr.updatedTs()));
      stmt.executeUpdate();

      return (WorkRequest<T>) findById(wr.id(), wr.payload().getClass())
        .orElseThrow(() -> new SQLException("Created WorkRequest not found: " + wr.id()));
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create WorkRequest", e);
    }
  }

  @Override
  public boolean updateStatus(String id, WorkRequest.Status status) {
    String sql = """
      UPDATE work_request
      SET status=?, updated_ts=NOW(), completed_ts=NOW(), lease_until=NULL, owner=NULL
      WHERE id=? AND status != ?
      """;
    Connection con = TransactionContext.get();
    try (var stmt = con.prepareStatement(sql)) {
      stmt.setString(1, status.name());
      stmt.setString(2, id);
      stmt.setString(3, status.name());
      return stmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to update WorkRequest status", e);
    }
  }

  @Override
  public <T> Optional<WorkRequest<T>> findById(String id, Class<T> payloadClass) {
    String sql = "SELECT * FROM work_request WHERE id=?";
    Connection con = TransactionContext.get();
    try (var stmt = con.prepareStatement(sql)) {
      stmt.setString(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(WorkRequestMapper.map(rs, payloadClass));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find WorkRequest with id: " + id, e);
    }
    return Optional.empty();
  }

  @Override
  public <T> List<WorkRequest<T>> findAllNonTerminal(Class<T> clazz, Page page) {
    String sql = "SELECT * FROM work_request WHERE status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED') " +
      "ORDER BY createdTs LIMIT ? OFFSET ?";
    List<WorkRequest<T>> results = new ArrayList<>();
    Connection con = TransactionContext.get();
    try (var stmt = con.prepareStatement(sql)) {
      stmt.setInt(1, page.limit());
      stmt.setInt(2, page.offset());
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          results.add(WorkRequestMapper.map(rs, clazz));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to fetch non-terminal WorkRequests", e);
    }
    return results;
  }

  @Override
  public boolean exists(String id) {
    String sql = "SELECT 1 FROM work_request WHERE id=?";
    Connection con = TransactionContext.get();
    try (var stmt = con.prepareStatement(sql)) {
      stmt.setString(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to fetch WorkRequest with id: " + id, e);
    }
  }

  @Override
  public boolean incrementAttempt(String id) {
    String sql = """
      UPDATE work_request
      SET attempts = attempts + 1, updated_ts=NOW, lease_until=NULL, owner=NULL
      WHERE id=? AND attempts < max_retries""";
    Connection con = TransactionContext.get();
    try (var stmt = con.prepareStatement(sql)) {
      stmt.setString(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to increment attempt for WorkRequest " + id, e);
    }
  }

  @Override
  public boolean extendLease(String wrId, String owner, Duration leasePeriod) {
    String sql = """
      UPDATE work_request
      SET lease_until = now() + interval '30 seconds'
      WHERE id = ?
        AND owner_id = ?
        AND status = 'InProgress';
      """;
    Connection con = TransactionContext.get();
    try (var stmt = con.prepareStatement(sql)) {
      stmt.setString(1, wrId);
      stmt.setString(2, owner);
      return stmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to increment attempt for WorkRequest " + wrId, e);
    }
  }
}
