package org.anthills.core.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;

public class Utils {

  public static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  public static boolean isEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  public static java.time.Instant getInstantSafely(ResultSet rs, String column) throws SQLException {
    Timestamp ts = rs.getTimestamp(column);
    return ts != null ? ts.toInstant() : null;
  }

  public static Timestamp getInstantSafely(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }
}
