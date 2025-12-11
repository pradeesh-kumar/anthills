package org.anthills.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.anthills.commons.WorkRequest;
import org.anthills.core.utils.JdbcSchemaProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Consumer;

public class AnthillsEngine {

  private static final Logger log = LoggerFactory.getLogger(AnthillsEngine.class);

  private final String dataSourceIdentity;
  private final TransactionManager txManager;
  private final LeaseRepository leaseRepository;
  private final LeaseService leaseService;
  private final WorkRequestRepository workRequestRepository;
  private final WorkRequestService workRequestService;

  private AnthillsEngine(DataSource dataSource) {
    JdbcSchemaProvider.initializeSchema(dataSource);
    this.dataSourceIdentity = deriveIdentityFromDataSource(dataSource);
    this.txManager = new JdbcTransactionManager(dataSource);
    this.leaseRepository = new LeaseJdbcRepository();
    this.workRequestRepository = new WorkRequestJdbcRepository(dataSource);
    this.leaseService = TransactionalProxy.create(new LeaseService(leaseRepository), txManager);
    this.workRequestService = null; // TODO instantiate work request service
  }

  private static String deriveIdentityFromDataSource(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection()) {
      return conn.getMetaData().getURL(); // Safe as key if one DataSource per URL
    } catch (SQLException e) {
      throw new RuntimeException("Unable to derive datasource key", e);
    }
  }

  public static AnthillsEngine fromJdbcDataSource(DataSource dataSource) {
    Objects.requireNonNull(dataSource, "DataSource must not be null");
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
    throw new IllegalStateException("not implemented");
  }
}
