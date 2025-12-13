package org.anthills.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public class LeaseJdbcRepository implements LeaseRepository {

  private static final Logger log =  LoggerFactory.getLogger(LeaseJdbcRepository.class);

  private static final String SQL_FIND_BY_OBJECT = "SELECT object, owner, expires_at FROM lease WHERE object = ?";
  @Override
  public Optional<Lease> findByObject(String object) {
    log.debug("Executing the query {} ", SQL_FIND_BY_OBJECT);
    Connection con = TransactionContext.get();
    try (PreparedStatement stmt = con.prepareStatement(SQL_FIND_BY_OBJECT)) {
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

  private static final String SQL_INSERT_LEASE = "INSERT INTO lease (object, owner, expires_at) VALUES (?, ?, ?)";
  @Override
  public boolean insertIfAbsent(Lease lease) {
    log.debug("Executing the query {} ", SQL_INSERT_LEASE);
    Connection con = TransactionContext.get();
    try (PreparedStatement stmt = con.prepareStatement(SQL_INSERT_LEASE)) {
      stmt.setString(1, lease.object());
      stmt.setString(2, lease.owner());
      stmt.setTimestamp(3, Timestamp.from(lease.expiresAt()));
      stmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      if (e.getSQLState().startsWith("23")) {
        return false;
      }
      throw new RuntimeException("Failed to insert lease", e);
    }
  }

  private static final String SQL_UPDATE_IF_EXPIRED = "UPDATE lease SET expires_at = ? WHERE object = ? AND expires_at < ?";
  @Override
  public boolean updateIfExpired(Lease lease) {
    log.debug("Executing the query {} ", SQL_UPDATE_IF_EXPIRED);
    Connection con = TransactionContext.get();
    try (PreparedStatement stmt = con.prepareStatement(SQL_UPDATE_IF_EXPIRED)) {
      stmt.setTimestamp(1, Timestamp.from(lease.expiresAt()));
      stmt.setString(2, lease.object());
      stmt.setTimestamp(3, Timestamp.from(Instant.now()));
      return stmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to update expired lease", e);
    }
  }

  private static final String SQL_UPDATE_IF_OWNED = "UPDATE lease SET expires_at = ? WHERE object = ? AND owner = ?";
  @Override
  public boolean updateIfOwned(Lease lease) {
    log.debug("Executing the query {} ", SQL_UPDATE_IF_OWNED);
    Connection con = TransactionContext.get();
    try (PreparedStatement stmt = con.prepareStatement(SQL_UPDATE_IF_OWNED)) {
      stmt.setTimestamp(1, Timestamp.from(lease.expiresAt()));
      stmt.setString(2, lease.object());
      stmt.setString(3, lease.owner());
      return stmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to extend lease", e);
    }
  }

  private static final String SQL_DELETE_BY_OWNER_AND_OBJECT = "DELETE FROM lease WHERE object = ? AND owner = ?";
  @Override
  public void deleteByOwnerAndObject(String owner, String object) {
    log.debug("Executing the query {} ", SQL_DELETE_BY_OWNER_AND_OBJECT);
    Connection con = TransactionContext.get();
    try (PreparedStatement stmt = con.prepareStatement(SQL_DELETE_BY_OWNER_AND_OBJECT)) {
      stmt.setString(1, object);
      stmt.setString(2, owner);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete lease", e);
    }
  }

  private static final String SQL_EXISTS_BY_OWNER_AND_OBJECT = "SELECT 1 FROM lease WHERE object = ? AND owner = ?";
  @Override
  public boolean existsByOwnerAndObject(String owner, String object) {
    log.debug("Executing the query {} ", SQL_EXISTS_BY_OWNER_AND_OBJECT);
    Connection con = TransactionContext.get();
    try (PreparedStatement stmt = con.prepareStatement(SQL_EXISTS_BY_OWNER_AND_OBJECT)) {
      stmt.setString(1, object);
      stmt.setString(2, owner);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return true;
        }
      }
      return false;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check lease existance", e);
    }
  }
}
