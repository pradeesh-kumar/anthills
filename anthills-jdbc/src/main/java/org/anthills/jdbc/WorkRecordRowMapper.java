package org.anthills.jdbc;

import org.anthills.api.work.WorkRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class WorkRecordRowMapper {

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

  public static List<WorkRecord> retrieveWorkRecords(ResultSet rs) throws SQLException {
    List<WorkRecord> results = new ArrayList<>();
    while (rs.next()) {
      results.add(WorkRecordRowMapper.map(rs));
    }
    return results;
  }

  private static Instant getInstantSafely(ResultSet rs, String column) throws SQLException {
    Timestamp ts = rs.getTimestamp(column);
    return ts != null ? ts.toInstant() : null;
  }
}
