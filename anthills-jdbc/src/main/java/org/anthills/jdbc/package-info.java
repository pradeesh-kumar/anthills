/**
 * JDBC persistence for Anthills, providing a cross-vendor {@link org.anthills.api.work.WorkStore}
 * implementation and supporting utilities.
 *
 * <h2>Key types</h2>
 * <ul>
 *   <li>{@link org.anthills.jdbc.JdbcWorkStore} — JDBC-backed implementation of {@link org.anthills.api.work.WorkStore}</li>
 *   <li>{@link org.anthills.jdbc.JdbcSchemaProvider} — initializes/verifies vendor-specific schema DDL</li>
 *   <li>{@link org.anthills.jdbc.ListWorkQueryBuilder} — renders parametrized SQL for {@link org.anthills.api.work.WorkQuery}</li>
 *   <li>{@link org.anthills.jdbc.WorkRecordRowMapper} — maps {@code ResultSet} rows to {@link org.anthills.api.work.WorkRecord}</li>
 *   <li>{@link org.anthills.jdbc.DbInfo} — dialect/version/identity of the connected DB</li>
 *   <li>{@link org.anthills.jdbc.JdbcSettings} — builder for data source/pool settings</li>
 * </ul>
 *
 * <h2>Schema</h2>
 * Vendor-specific DDL lives under {@code /sqldb/schema-*.sql}. At startup,
 * {@link org.anthills.jdbc.JdbcSchemaProvider} detects the dialect and applies the matching DDL
 * if required tables are missing, falling back to a default script when not found.
 *
 * <h2>Dialects</h2>
 * Supported vendors are enumerated in {@link org.anthills.jdbc.DbInfo.Dialect}, and affect LIMIT/OFFSET
 * rendering, locking hints for claim operations, and benign error detection during schema setup.
 *
 * @see org.anthills.jdbc.util for utilities such as ID generation
 */
package org.anthills.jdbc;
