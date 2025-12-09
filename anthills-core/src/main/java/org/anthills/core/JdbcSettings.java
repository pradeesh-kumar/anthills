package org.anthills.core;

import java.util.Locale;
import java.util.logging.Logger;

import static org.anthills.core.Utils.isBlank;

public record JdbcSettings(
  String jdbcUrl,
  String username,
  String password,
  int maxPoolSize,
  int minIdleConnections,
  long connectionTimeoutMs
) {

  private static final Logger log = Logger.getLogger(JdbcSettings.class.getName());

  private static void warnIfNonProdDatabase(String jdbcUrl) {
    String url = jdbcUrl.toLowerCase(Locale.ROOT);
    if (url.contains(":h2:mem:") || url.contains(":sqlite:") || url.contains(":derby:memory:")) {
      log.warning(() -> String.format(
        "JDBC URL [%s] appears to use an in-memory or development-only database. " +
          "Avoid using this in production.", jdbcUrl));
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String jdbcUrl;
    private String username;
    private String password;

    private int maxPoolSize = 100;
    private int minIdleConnections = 10;
    private long connectionTimeoutMs = 30_000;

    public Builder() {
    }

    public Builder jdbcUrl(String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder maxPoolSize(int maxPoolSize) {
      this.maxPoolSize = maxPoolSize;
      return this;
    }

    public Builder minIdleConnections(int minIdleConnections) {
      this.minIdleConnections = minIdleConnections;
      return this;
    }

    public Builder connectionTimeoutMs(long connectionTimeoutMs) {
      this.connectionTimeoutMs = connectionTimeoutMs;
      return this;
    }

    public JdbcSettings build() {
      validate();
      return new JdbcSettings(jdbcUrl, username, password, maxPoolSize, minIdleConnections, connectionTimeoutMs);
    }

    private void validate() {
      if (isBlank(this.jdbcUrl)) {
        throw new IllegalArgumentException("JDBC URL must not be null or blank");
      }

      if (isBlank(this.username)) {
        throw new IllegalArgumentException("Username must not be null or blank");
      }

      if (this.password == null) {
        throw new IllegalArgumentException("Password must not be null (can be blank)");
      }

      if (this.maxPoolSize <= 0) {
        throw new IllegalArgumentException("Maximum pool size must be greater than 0");
      }

      if (this.minIdleConnections < 0 || this.minIdleConnections > this.maxPoolSize) {
        throw new IllegalArgumentException("Minimum idle must be >= 0 and <= maximum pool size");
      }

      if (this.connectionTimeoutMs < 1000) {
        throw new IllegalArgumentException("Connection timeout must be at least 1000 ms");
      }
      warnIfNonProdDatabase(this.jdbcUrl);
    }
  }
}
