package org.anthills.core.utils;

import org.anthills.core.DbInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class JdbcSchemaProvider {

  private static final Logger log = LoggerFactory.getLogger(JdbcSchemaProvider.class);
  public static final Set<String> schemaInitializedDataSources = new HashSet<>();

  public static void initializeSchema(DataSource dataSource, DbInfo dbInfo) {
    log.info("Initializing database schema");
    synchronized (schemaInitializedDataSources) {
      try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        if (schemaInitializedDataSources.contains(dbInfo.identity())) {
          log.debug("Schema already initialized for the datasource {}", dbInfo.identity());
          return;
        }
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
    return "schema" + dialect.name().toLowerCase() + ".sql";
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
