package org.anthills.core;

import javax.sql.DataSource;
import java.sql.Connection;

public class JdbcTransactionManager implements TransactionManager {

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
    } catch (Throwable e) {
      throw new RuntimeException("Transaction failed", e);
    }
  }
}
