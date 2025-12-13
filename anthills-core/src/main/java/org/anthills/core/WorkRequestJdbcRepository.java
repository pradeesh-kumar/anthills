package org.anthills.core;

import com.google.gson.Gson;
import org.anthills.commons.WorkRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkRequestJdbcRepository implements WorkRequestRepository {

  private final Gson gson = new Gson();

  @Override
  public <T> WorkRequest<T> create(WorkRequest<T> request) {
    String sql = "INSERT INTO work_request(id, payload, status, details, createdTs) VALUES (?, ?, ?, ?, ?)";
    Connection con = TransactionContext.get();
    try (var stmt = con.prepareStatement(sql)) {
      stmt.setString(1, request.id());
      stmt.setString(2, gson.toJson(request.payload()));
      stmt.setString(3, request.status().name());
      stmt.setString(4, request.details());
      stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
      stmt.executeUpdate();

      return (WorkRequest<T>) findById(request.id(), request.payload().getClass())
        .orElseThrow(() -> new SQLException("Created WorkRequest not found: " + request.id()));
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
}
