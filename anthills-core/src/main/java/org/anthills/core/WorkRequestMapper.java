package org.anthills.core;

import com.google.gson.Gson;
import org.anthills.commons.WorkRequest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.anthills.core.utils.Utils.getInstantSafely;

public class WorkRequestMapper {

  private static final Gson gson = new Gson();

  public static <T> WorkRequest<T> map(ResultSet rs, Class<T> payloadType) throws SQLException {
    return WorkRequest.<T>builder()
      .setId(rs.getString("id"))
      .setPayloadClass(payloadType.getSimpleName())
      .setPayload(gson.fromJson(rs.getString("payload"), payloadType))
      .setStatus(WorkRequest.Status.valueOf(rs.getString("status")))
      .setDetails(rs.getString("details"))
      .setMaxRetries(rs.getInt("max_retries"))
      .setAttempts(rs.getInt("attempts"))
      .setOwner(rs.getString("owner"))
      .setLeaseUntil(getInstantSafely(rs, "lease_until"))
      .setCreatedTs(getInstantSafely(rs, "created_ts"))
      .setCompletedTs(getInstantSafely(rs, "completed_ts"))
      .setStartedTs(getInstantSafely(rs, "started_ts"))
      .setUpdatedTs(getInstantSafely(rs, "updated_ts"))
      .build();
  }
}
