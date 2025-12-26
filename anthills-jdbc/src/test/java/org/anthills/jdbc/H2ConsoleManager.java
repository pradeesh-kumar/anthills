package org.anthills.jdbc;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

public class H2ConsoleManager {

  private static final Logger log = LoggerFactory.getLogger(H2ConsoleManager.class);
  private static final String PORT = "8082";

  private static Server webServer;

  public static void startIfEnabled() {
    Properties props = new Properties();
    try (InputStream in = H2ConsoleManager.class.getClassLoader().getResourceAsStream("test.properties")) {
      if (in != null) {
        props.load(in);
        if ("true".equalsIgnoreCase(props.getProperty("h2webenabled"))) {
          startConsole();
        }
      }
    } catch (IOException | SQLException e) {
      throw new RuntimeException("Failed to load H2 console", e);
    }
  }

  private static void startConsole() throws SQLException {
    if (webServer == null || !webServer.isRunning(false)) {
      log.info("Starting H2 console at port {}", PORT);
      webServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", PORT).start();
      //webServer.start();
      log.info("Started H2 console at port http://localhost:{}", PORT);
    }
  }
}
