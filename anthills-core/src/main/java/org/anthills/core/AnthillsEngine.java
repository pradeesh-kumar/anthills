package org.anthills.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.anthills.commons.WorkRequest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Consumer;

public class AnthillsEngine {

  private final DataSource dataSource;
  private final TransactionManager txManager;
  private final LeaseRepository leaseRepository;
  private final LeaseService leaseService;
  private final WorkRequestRepository workRequestRepository;
  private final WorkRequestService workRequestService;

  private AnthillsEngine(DataSource dataSource) {
    this.dataSource = dataSource;
    this.txManager = new JdbcTransactionManager(dataSource);
    this.leaseRepository = new LeaseJdbcRepository(dataSource);
    this.workRequestRepository = new WorkRequestJdbcRepository(dataSource);

    LeaseService originalLeaseService = new LeaseService(leaseRepository, txManager);
    this.leaseService = TransactionalProxy.create(originalLeaseService, txManager);

    this.workRequestService = null; // TODO instantiate work request service
  }

  public static AnthillsEngine fromJdbcDataSource(DataSource dataSource) {
    Objects.requireNonNull(dataSource,  "DataSource must not be null");
    return new AnthillsEngine(dataSource);
  }

  public static AnthillsEngine fromJdbcSettings(JdbcSettings jdbcSettings) {
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
    return fromJdbcDataSource(new HikariDataSource(hikariConfig));
  }

  public ScheduledWorker newScheduledWorker(SchedulerConfig config, Runnable task) {
    return new ScheduledWorker(config, task);
  }

  public LeasedScheduledWorker newLeasedScheduledWorker(SchedulerConfig config, Runnable task) {
    return new LeasedScheduledWorker(config, task, leaseService);
  }

  public <T> RequestWorker<T> newRequestWorker(WorkerConfig config, Consumer<WorkRequest<T>> callable) {
    throw new IllegalStateException("not implemented");
  }

  public void awaitTermination() {

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
