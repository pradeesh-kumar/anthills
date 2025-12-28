package org.anthills.jdbc;

import org.anthills.api.work.WorkRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps JDBC {@link ResultSet} rows to {@link org.anthills.api.work.WorkRecord} instances.
 *
 * Column expectations (case-insensitive):
 * id, work_type, payload, payload_type, payload_version, codec, status, max_retries,
 * attempt_count, owner_id, lease_until, failure_reason, created_ts, updated_ts, started_ts, completed_ts.
 */
public final class WorkRecordRowMapper {

  /**
   * Maps the current row of the {@link ResultSet} to a {@link WorkRecord}.
   *
   * @param rs positioned result set
   * @return mapped WorkRecord
   * @throws SQLException if a JDBC access error occurs
   */
  public static WorkRecord map(ResultSet rs) throws SQLException {
    return WorkRecord.builder()
      .id(rs.getString("id"))
      .workType(rs.getString("work_type"))

      .payload(rs.getBytes("payload"))
      .payloadType(rs.getString("payload_type"))
      .payloadVersion(rs.getInt("payload_version"))
      .codec(rs.getString("codec"))

      .status(rs.getString("status"))
      .maxRetries(rs.getObject("max_retries", Integer.class))
      .attemptCount(rs.getInt("attempt_count"))
      .ownerId(rs.getString("owner_id"))
      .leaseUntil(getInstantSafely(rs, "lease_until"))

      .failureReason(rs.getString("failure_reason"))

      .createdTs(getInstantSafely(rs, "created_ts"))
      .updatedTs(getInstantSafely(rs, "updated_ts"))
      .startedTs(getInstantSafely(rs, "started_ts"))
      .completedTs(getInstantSafely(rs, "completed_ts"))
      .build();
  }

  /**
   * Iterates through the {@link ResultSet} and maps all rows to {@link WorkRecord}s.
   *
   * @param rs result set to iterate (will be consumed)
   * @return list of mapped work records (possibly empty)
   * @throws SQLException if a JDBC access error occurs
   */
  public static List<WorkRecord> retrieveWorkRecords(ResultSet rs) throws SQLException {
    List<WorkRecord> results = new ArrayList<>();
    while (rs.next()) {
      results.add(WorkRecordRowMapper.map(rs));
    }
    return results;
  }

  /**
   * Reads a nullable timestamp column as {@link Instant}.
   *
   * @param rs result set
   * @param column column name
   * @return Instant value, or null if the column is SQL NULL
   * @throws SQLException if a JDBC access error occurs
   */
  static Instant getInstantSafely(ResultSet rs, String column) throws SQLException {
    Timestamp ts = rs.getTimestamp(column);
    return ts != null ? ts.toInstant() : null;
  }
}
