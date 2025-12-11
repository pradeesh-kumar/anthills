package org.anthills.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class JdbcSchemaProvider {

  private static final Logger log = LoggerFactory.getLogger(JdbcSchemaProvider.class);
  public static final Set<String> schemaInitializedDataSources = new HashSet<>();

  public static void initializeSchema(DataSource dataSource) {
    log.info("Initializing database schema");
    synchronized (schemaInitializedDataSources) {
      try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        String dbIdentity = conn.getMetaData().getURL();
        if (schemaInitializedDataSources.contains(dbIdentity)) {
          log.debug("Schema already initialized for the datasource {}", dbIdentity);
          return;
        }
        String schemaFile = getSchemaFile(conn);
        log.debug("Schema file: {}", schemaFile);
        String sql = readSchemaFromClasspath("/sqldb/" + schemaFile);
        executeSqlStatements(conn, sql);
        conn.commit();
        schemaInitializedDataSources.add(dbIdentity);
      } catch (Exception e) {
        throw new RuntimeException("Failed to initialize schema", e);
      }
    }
  }

  private static String getSchemaFile(Connection conn) throws SQLException {
    DatabaseMetaData metaData = conn.getMetaData();
    String dbName = metaData.getDatabaseProductName().toLowerCase(Locale.ROOT);
    if (dbName.contains("sqlite")) {
      return "schema-sqlite.sql";
    }
    String url = metaData.getURL().toLowerCase(Locale.ROOT);
    if (url.contains("aurora")) {
      if (dbName.contains("mysql")) {
        return "schema-mysql.sql";
      } else if (dbName.contains("postgresql")) {
        return "schema-postgresql.sql";
      }
      throw new UnsupportedOperationException("Unknown Aurora flavor: " + dbName);
    }
    if (dbName.contains("spanner")) {
      return "schema-spanner.sql";
    }
    if (dbName.contains("google") && dbName.contains("spanner")) {
      return "schema-spanner.sql";
    }
    if (dbName.contains("db2")) {
      return "schema-db2.sql";
    }
    return switch (dbName) {
      case "postgresql" -> "schema-postgresql.sql";
      case "mysql" -> "schema-mysql.sql";
      case "h2" -> "schema-h2.sql";
      case "microsoft sql server" -> "schema-mssqlserver.sql";
      case "oracle" -> "schema-oracle.sql";
      default -> throw new UnsupportedOperationException("Unsupported DB: " + dbName);
    };
  }

  private static String readSchemaFromClasspath(String path) throws Exception {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
      Objects.requireNonNull(JdbcSchemaProvider.class.getResourceAsStream(path))))) {
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }

  private static void executeSqlStatements(Connection conn, String sql) throws SQLException {
    log.debug("Executing schema sql:\n{}", sql);
    try (Statement stmt = conn.createStatement()) {
      for (String ddl : sql.split(";")) {
        String trimmed = ddl.strip();
        if (!trimmed.isEmpty()) {
          stmt.execute(trimmed);
        }
      }
    } catch (SQLException e) {
      conn.rollback();
      throw e;
    }
  }
}
