package org.anthills.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Objects;

public record DbInfo(Dialect dialect, String version, String identity) {

  public enum Dialect {
    PostgresSQL,
    MySQL,
    Oracle,
    DB2,
    H2,
    Sqlite,
    MSSQL
  }

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
