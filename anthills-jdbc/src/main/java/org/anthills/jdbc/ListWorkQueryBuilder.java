package org.anthills.jdbc;

import org.anthills.api.WorkQuery;
import org.anthills.api.WorkRequest;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a parametrized SQL query for listing work items based on WorkQuery.
 * Exposes the SQL string with placeholders and the ordered parameter list.
 */
public final class ListWorkQueryBuilder {
  private final WorkQuery query;
  private final StringBuilder sql = new StringBuilder("SELECT * FROM work_request WHERE 1=1");
  private final List<Object> params = new ArrayList<>();

  public ListWorkQueryBuilder(WorkQuery query) {
    this.query = Objects.requireNonNull(query, "query is required");
  }

  /**
   * Build and return the SQL string with placeholders.
   */
  public String buildSql() {
    build();
    return sql.toString();
  }

  /**
   * Return the parameters in the same order as the placeholders in the SQL.
   */
  public List<Object> params() {
    return params;
  }

  private void build() {
    // If explicitly provided an empty status set, force empty results.
    if (query.statuses() != null && query.statuses().isEmpty()) {
      sql.append(" AND 1=0");
    }

    if (query.workType() != null) {
      sql.append(" AND work_type = ?");
      params.add(query.workType());
    }

    if (query.statuses() != null && !query.statuses().isEmpty()) {
      sql.append(" AND status IN (");
      int i = 0;
      for (WorkRequest.Status s : query.statuses()) {
        if (i++ > 0) sql.append(", ");
        sql.append("?");
        params.add(s.name());
      }
      sql.append(")");
    }

    if (query.createdAfter() != null) {
      sql.append(" AND created_ts > ?");
      params.add(Timestamp.from(query.createdAfter()));
    }

    if (query.createdBefore() != null) {
      sql.append(" AND created_ts < ?");
      params.add(Timestamp.from(query.createdBefore()));
    }
    sql.append(" ORDER BY created_ts DESC");

    int limit = query.page() != null ? query.page().limit() : 100;
    int offset = query.page() != null ? query.page().offset() : 0;

    sql.append(" LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
  }
}
