package org.anthills.core;

import org.anthills.commons.WorkRequest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

public class AnthillsEngine {

  public static AnthillsEngine fromJdbcDataSource(DataSource dataSource) throws SQLException {
    throw new IllegalStateException("not implemented");
  }

  public static ScheduledWorker createScheduledWorker(SchedulerConfig config, Runnable task) {
    throw new IllegalStateException("not implemented");
  }

  public static LeasedScheduledWorker createLeasedScheduledWorker(SchedulerConfig config, Runnable task) {
    throw new IllegalStateException("not implemented");
  }

  public static <T> RequestWorker<T> createRequestWorker(WorkerConfig config, Callable<WorkRequest<T>> callable) {
    throw new IllegalStateException("not implemented");
  }

  private String detectDialect(DataSource dataSource) throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
      String dbName = conn.getMetaData().getDatabaseProductName().toLowerCase();
      return switch (dbName) {
        case "postgresql" -> "org.hibernate.dialect.PostgreSQLDialect";
        case "mysql" -> "org.hibernate.dialect.MySQL8Dialect";
        case "mariadb" -> "org.hibernate.dialect.MariaDBDialect";
        case "oracle" -> "org.hibernate.dialect.Oracle12cDialect";
        case "microsoft sql server" -> "org.hibernate.dialect.SQLServerDialect";
        case "h2" -> "org.hibernate.dialect.H2Dialect";
        default -> throw new IllegalArgumentException("Unsupported DB: " + dbName);
      };
    }
  }
}
