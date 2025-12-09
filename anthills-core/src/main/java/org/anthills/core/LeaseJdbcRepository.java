package org.anthills.core;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public class LeaseJdbcRepository implements LeaseRepository {
  private final DataSource dataSource;

  LeaseJdbcRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Optional<Lease> findByObject(String object) {
    String sql = "SELECT object, owner, expires_at FROM leases WHERE object = ?";
    Connection con = TransactionContext.get();
    try (PreparedStatement stmt = con.prepareStatement(sql)) {
      stmt.setString(1, object);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(new Lease(
            rs.getString("object"),
            rs.getString("owner"),
            rs.getTimestamp("expires_at").toInstant()
          ));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to read lease for object: " + object, e);
    }
  }

  @Override
  public boolean insertIfAbsent(Lease lease) {
    String sql = "INSERT INTO leases (object, owner, expires_at) VALUES (?, ?, ?)";
    Connection con = TransactionContext.get();
    try (PreparedStatement stmt = con.prepareStatement(sql)) {
      stmt.setString(1, lease.object());
      stmt.setString(2, lease.owner());
      stmt.setTimestamp(3, Timestamp.from(lease.expiresAt()));
      stmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      if (e.getSQLState().startsWith("23")) { // constraint violation
        return false;
      }
      throw new RuntimeException("Failed to insert lease", e);
    }
  }

  @Override
  public boolean updateIfOwnedAndNotExpired(Lease lease) {
    String sql = """
      UPDATE leases SET expires_at = ?
      WHERE object = ? AND owner = ? AND expires_at > ?
      """;
    Connection con = TransactionContext.get();
    try (PreparedStatement stmt = con.prepareStatement(sql)) {
      stmt.setTimestamp(1, Timestamp.from(lease.expiresAt()));
      stmt.setString(2, lease.object());
      stmt.setString(3, lease.owner());
      stmt.setTimestamp(4, Timestamp.from(Instant.now()));
      return stmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to extend lease", e);
    }
  }

  @Override
  public void deleteByOwnerAndObject(String owner, String object) {
    String sql = "DELETE FROM leases WHERE object = ? AND owner = ?";
    Connection con = TransactionContext.get();
    try (PreparedStatement stmt = con.prepareStatement(sql)) {
      stmt.setString(1, object);
      stmt.setString(2, owner);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to release lease", e);
    }
  }
}
