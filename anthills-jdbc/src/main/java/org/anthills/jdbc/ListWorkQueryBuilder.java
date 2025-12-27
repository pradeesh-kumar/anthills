package org.anthills.jdbc;

import org.anthills.api.work.WorkQuery;
import org.anthills.api.work.WorkRequest;

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
  private final DbInfo.Dialect dialect;
  private final StringBuilder sql = new StringBuilder("SELECT * FROM work_request WHERE 1=1");
  private final List<Object> params = new ArrayList<>();

  /**
   * Creates a builder bound to a specific {@link WorkQuery} and database dialect.
   *
   * @param query high-level filtering and paging criteria
   * @param dialect target SQL dialect used to render LIMIT/OFFSET variants
   * @throws NullPointerException if any argument is null
   */
  public ListWorkQueryBuilder(WorkQuery query, DbInfo.Dialect dialect) {
    this.query = Objects.requireNonNull(query, "query is required");
    this.dialect = Objects.requireNonNull(dialect, "dialect is required");
  }

  /**
   * Builds and returns the SQL string with JDBC placeholders.
   * Call {@link #params()} afterwards to get the ordered parameter list.
   *
   * @return SQL string suitable for a {@link java.sql.PreparedStatement}
   */
  public String buildSql() {
    build();
    return sql.toString();
  }

  /**
   * Returns the parameters in the same order as the placeholders in the SQL
   * produced by {@link #buildSql()}.
   *
   * @return ordered parameter values
   */
  public List<Object> params() {
    return params;
  }

  /**
   * Internal builder that appends WHERE clauses based on {@link WorkQuery}, applies
   * ordering, and renders paging syntax depending on {@link DbInfo.Dialect}.
   */
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

    switch (dialect) {
      case MSSQL -> {
        // SQL Server uses OFFSET then FETCH NEXT
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);
      }
      case Oracle, DB2 -> {
        // Modern Oracle (12c+) and DB2 support OFFSET/FETCH
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);
      }
      default -> {
        // Postgres, MySQL, H2, SQLite
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
      }
    }
  }
}
