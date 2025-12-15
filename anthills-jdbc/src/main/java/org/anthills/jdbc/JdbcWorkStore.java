package org.anthills.jdbc;

import org.anthills.api.PayloadCodec;
import org.anthills.api.SubmissionOptions;
import org.anthills.api.WorkRequest;
import org.anthills.api.WorkStore;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;

public final class JdbcWorkStore implements WorkStore {

  private final DataSource dataSource;
  private final PayloadCodec codec;

  private JdbcWorkStore(DataSource dataSource, PayloadCodec codec) {
    this.dataSource = dataSource;
    this.codec = codec;
  }

  public static JdbcWorkStore create(DataSource dataSource) {
    return new JdbcWorkStore(dataSource, JsonPayloadCodec.defaultInstance());
  }

  public static JdbcWorkStore create(DataSource dataSource, PayloadCodec codec) {
    return new JdbcWorkStore(dataSource, codec);
  }

  @Override
  public <T> List<WorkRequest<T>> claimWork(String workerId, int batchSize, Duration leaseDuration, Class<T> payloadType) {
    return List.of();
  }

  @Override
  public boolean renewLease(String workRequestId, String workerId, Duration leaseDuration) {
    return false;
  }

  @Override
  public void markSucceeded(String workRequestId, String workerId) {

  }

  @Override
  public void markFailed(String workRequestId, String workerId, Throwable error) {

  }

  @Override
  public String submit(Object payload, SubmissionOptions options) {
    return "";
  }

  @Override
  public boolean tryAcquireSchedule(String jobName, String workerId, Duration leaseDuration) {
    return false;
  }

  @Override
  public void renewScheduleLease(String jobName, String workerId, Duration leaseDuration) {

  }

  @Override
  public void releaseSchedule(String jobName, String workerId) {

  }
}
