package org.anthills.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Describes a database connection target, including detected SQL dialect, version,
 * and a stable identity string (typically the JDBC URL) used to scope one-time actions
 * such as schema initialization.
 *
 * @param dialect detected vendor/dialect
 * @param version database product version string
 * @param identity stable identifier for the datasource (e.g., JDBC URL)
 */
public record DbInfo(Dialect dialect, String version, String identity) {

  /**
   * Supported database dialects used to adapt SQL and DDL.
   */
  public enum Dialect {
    PostgresSQL,
    MySQL,
    Oracle,
    DB2,
    H2,
    Sqlite,
    MSSQL
  }

  /**
   * Inspects the provided {@link DataSource} to detect the database product name,
   * version, and identity (JDBC URL), and maps the product to a supported {@link Dialect}.
   *
   * @param dataSource the data source to inspect
   * @return populated {@link DbInfo} for the given data source
   * @throws RuntimeException if the connection metadata cannot be read
   */
  public static DbInfo detect(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData meta = connection.getMetaData();
      String productName = meta.getDatabaseProductName();
      String version = meta.getDatabaseProductVersion();
      String identity = connection.getMetaData().getURL();
      DbInfo.Dialect dialect = mapToDialect(productName);
      return new DbInfo(dialect, version, identity);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to detect database dialect", e);
    }
  }

  /**
   * Maps a vendor product name (from JDBC metadata) to an internal {@link Dialect}.
   *
   * @param productName vendor product name (e.g., "PostgreSQL", "MySQL")
   * @return detected dialect
   * @throws UnsupportedOperationException if the product is not supported
   */
  private static DbInfo.Dialect mapToDialect(String productName) {
    Objects.requireNonNull(productName);
    String name = productName.toLowerCase();

    // Order matters: match more specific strings first
    if (name.contains("postgres")) {
      return DbInfo.Dialect.PostgresSQL;
    }
    if (name.contains("mysql")) {
      return DbInfo.Dialect.MySQL;
    }
    if (name.contains("mariadb")) {
      return DbInfo.Dialect.MySQL;
    }
    if (name.contains("oracle")) {
      return DbInfo.Dialect.Oracle;
    }
    if (name.contains("db2")) {
      return DbInfo.Dialect.DB2;
    }
    if (name.contains("h2")) {
      return DbInfo.Dialect.H2;
    }
    if (name.contains("sqlite")) {
      return DbInfo.Dialect.Sqlite;
    }
    if (name.contains("microsoft sql") || name.contains("sql server")) {
      return DbInfo.Dialect.MSSQL;
    }
    throw new UnsupportedOperationException("Unsupported DB: " + productName);
  }
}
