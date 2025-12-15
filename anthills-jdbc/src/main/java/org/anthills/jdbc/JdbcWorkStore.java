package org.anthills.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.anthills.api.PayloadCodec;
import org.anthills.api.WorkQuery;
import org.anthills.api.WorkRequest;
import org.anthills.api.WorkStore;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcWorkStore implements WorkStore {

  private final DataSource dataSource;
  private final PayloadCodec codec;

  private JdbcWorkStore(DataSource dataSource, PayloadCodec codec) {
    DbInfo dbInfo = DbInfo.detect(dataSource);
    JdbcSchemaProvider.initializeSchema(dataSource, dbInfo);
    this.dataSource = dataSource;
    this.codec = codec;
  }

  public static JdbcWorkStore create(DataSource dataSource) {
    return new JdbcWorkStore(dataSource, JsonPayloadCodec.defaultInstance());
  }

  public static JdbcWorkStore create(DataSource dataSource, PayloadCodec codec) {
    return new JdbcWorkStore(dataSource, codec);
  }

  public static JdbcWorkStore create(JdbcSettings jdbcSettings) {
    return create(jdbcSettings, null);
  }

  public static JdbcWorkStore create(JdbcSettings jdbcSettings, PayloadCodec codec) {
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
    return create(new HikariDataSource(hikariConfig), codec);
  }

  @Override
  public <T> WorkRequest<T> createWork(String workType, byte[] payload, int payloadVersion, String codec, Integer maxRetries) {
    return null;
  }

  @Override
  public Optional<WorkRequest<?>> getWork(String workId) {
    return Optional.empty();
  }

  @Override
  public List<WorkRequest<?>> listWork(WorkQuery query) {
    return List.of();
  }

  @Override
  public <T> List<WorkRequest<T>> claimWork(String workerId, int batchSize, Duration leaseDuration, Class<T> payloadType) {
    return List.of();
  }

  @Override
  public List<WorkRequest<?>> claimWork(String workType, String ownerId, int limit, Duration leaseDuration) {
    return List.of();
  }

  @Override
  public boolean renewLease(String workId, String ownerId, Duration leaseDuration) {
    return false;
  }

  @Override
  public void markSucceeded(String workId, String ownerId) {

  }

  @Override
  public void markFailed(String workId, String ownerId, String failureReason) {

  }

  @Override
  public void markCancelled(String workId) {

  }

  @Override
  public void markFailed(String workRequestId, String workerId, Throwable error) {

  }

  @Override
  public boolean tryAcquireSchedulerLease(String jobName, String ownerId, Duration leaseDuration) {
    return false;
  }

  @Override
  public boolean renewSchedulerLease(String jobName, String ownerId, Duration leaseDuration) {
    return false;
  }

  @Override
  public void releaseSchedulerLease(String jobName, String ownerId) {

  }
}
