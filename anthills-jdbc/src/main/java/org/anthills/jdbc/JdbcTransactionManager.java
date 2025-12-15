package org.anthills.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class JdbcTransactionManager implements TransactionManager {

  private static final Logger log = LoggerFactory.getLogger(JdbcTransactionManager.class);

  private final DataSource dataSource;

  public JdbcTransactionManager(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public <T> T execute(TransactionCallback<T> callback) {
    try (Connection con = dataSource.getConnection()) {
      TransactionContext.set(con);
      con.setAutoCommit(false);
      try {
        T result = callback.doInTransaction();
        con.commit();
        return result;
      } catch (Exception e) {
        con.rollback();
        throw new RuntimeException("Transaction failed, rolled back", e);
      } finally {
        TransactionContext.clear();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to get JDBC Connection", e);
    }
  }
}
