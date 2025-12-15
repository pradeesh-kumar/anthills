package org.anthills.jdbc;

import com.google.gson.Gson;
import org.anthills.api.WorkRequest;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WorkRequestMapper {

  private static final Gson gson = new Gson();

  public static <T> WorkRequest<T> map(ResultSet rs, Class<T> payloadType) throws SQLException {

  }
}
