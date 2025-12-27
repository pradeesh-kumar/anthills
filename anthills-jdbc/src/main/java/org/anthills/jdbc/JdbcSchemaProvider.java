package org.anthills.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class JdbcSchemaProvider {

  private static final Logger log = LoggerFactory.getLogger(JdbcSchemaProvider.class);
  public static final Set<String> schemaInitializedDataSources = new HashSet<>();

  public static void initializeSchema(DataSource dataSource, DbInfo dbInfo) {
    log.info("Initializing database schema");
    synchronized (schemaInitializedDataSources) {
      try (Connection conn = dataSource.getConnection()) {
        if (schemaInitializedDataSources.contains(dbInfo.identity())) {
          log.debug("Schema already initialized for the datasource {}", dbInfo.identity());
          return;
        }
        // Check if the schema is already present (cross-vendor)
        if (schemaExists(conn)) {
          log.debug("Schema objects already present. Skipping DDL execution.");
          schemaInitializedDataSources.add(dbInfo.identity());
          return;
        }
        conn.setAutoCommit(false);
        String schemaFile = getSchemaFile(dbInfo.dialect());
        log.debug("Schema file: {}", schemaFile);
        String sql = readSchemaFromClasspath("/sqldb/" + schemaFile);
        executeSqlStatements(conn, sql);
        conn.commit();
        schemaInitializedDataSources.add(dbInfo.identity());
      } catch (Exception e) {
        throw new RuntimeException("Failed to initialize schema", e);
      }
    }
  }

  private static String getSchemaFile(DbInfo.Dialect dialect) {
    return switch (dialect) {
      case PostgresSQL -> "schema-postgresql.sql";
      case MySQL -> "schema-mysql.sql";
      case Oracle -> "schema-oracle.sql";
      case DB2 -> "schema-db2.sql";
      case H2 -> "schema-h2.sql";
      case Sqlite -> "schema-sqllite.sql";
      case MSSQL -> "schema-mssqlserver.sql";
    };
  }

  private static String readSchemaFromClasspath(String path) throws Exception {
    InputStream in = JdbcSchemaProvider.class.getResourceAsStream(path);
    if (in == null) {
      log.warn("Schema file {} not found on classpath. Falling back to default schema.", path);
      in = JdbcSchemaProvider.class.getResourceAsStream("/sqldb/schema-default.sql");
    }
    if (in == null) {
      throw new IllegalStateException("Schema resource not found for " + path);
    }
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }

  private static void executeSqlStatements(Connection conn, String sql) throws SQLException {
    log.debug("Executing schema sql:\n{}", sql);
    try (Statement stmt = conn.createStatement()) {
      for (String ddl : sql.split(";")) {
        String trimmed = ddl.strip();
        if (trimmed.isEmpty()) {
          continue;
        }
        try {
          stmt.execute(trimmed);
        } catch (SQLException e) {
          if (isAlreadyExistsError(e)) {
            log.debug("Ignoring DDL as object already exists: {}", e.getMessage());
            continue;
          }
          conn.rollback();
          throw e;
        }
      }
    }
  }

  private static boolean schemaExists(Connection conn) throws SQLException {
    return tableExists(conn, "work_request") && tableExists(conn, "scheduler_lease");
  }

  private static boolean tableExists(Connection conn, String tableName) throws SQLException {
    DatabaseMetaData meta = conn.getMetaData();
    try (ResultSet rs = meta.getTables(conn.getCatalog(), null, null, new String[] {"TABLE"})) {
      while (rs.next()) {
        String name = rs.getString("TABLE_NAME");
        if (name != null && name.equalsIgnoreCase(tableName)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if the SQLException corresponds to an "object already exists" condition
   * across common vendors. This allows running schema DDL safely without IF NOT EXISTS.
   */
  private static boolean isAlreadyExistsError(SQLException e) {
    String state = e.getSQLState();
    int code = e.getErrorCode();
    String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

    // Oracle ORA-00955 name is already used by an existing object
    if (code == 955) return true;

    // PostgreSQL: duplicate_table
    if ("42P07".equals(state)) return true;

    // MySQL: table exists
    if ("42S01".equals(state)) return true;

    // SQL Server: 2714 - There is already an object named 'X' in the database.
    if (code == 2714) return true;

    // DB2: 42710 - object already exists
    if ("42710".equals(state)) return true;

    // Generic fallback
    return msg.contains("already exists")
      || msg.contains("already an object")
      || msg.contains("name is already used");
  }
}
