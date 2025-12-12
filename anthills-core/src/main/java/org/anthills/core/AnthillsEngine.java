package org.anthills.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.anthills.commons.WorkRequest;
import org.anthills.core.utils.JdbcSchemaProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.function.Consumer;

public class AnthillsEngine {

  private static final Logger log = LoggerFactory.getLogger(AnthillsEngine.class);

  private final TransactionManager txManager;
  private final LeaseRepository leaseRepository;
  private final LeaseService leaseService;
  private final WorkItemClaimer workItemClaimer;

  private AnthillsEngine(DataSource dataSource) {
    DbInfo dbInfo = DbInfo.detect(dataSource);
    JdbcSchemaProvider.initializeSchema(dataSource, dbInfo);
    this.txManager = new JdbcTransactionManager(dataSource);
    this.leaseRepository = new LeaseJdbcRepository();
    this.leaseService = TransactionalProxy.create(new LeaseService(leaseRepository), txManager);
    this.workItemClaimer = TransactionalProxy.create(WorkItemClaimerFactory.getClaimerFor(dbInfo), txManager);
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
