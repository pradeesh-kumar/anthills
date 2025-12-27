package org.anthills.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Connection and pool configuration used to create a JDBC {@code DataSource}.
 *
 * @param jdbcUrl JDBC connection string
 * @param username database username
 * @param password database password (may be blank, but never null)
 * @param maxPoolSize maximum number of connections in the pool
 * @param minIdleConnections minimum number of idle connections to keep alive
 * @param connectionTimeoutMs maximum time to wait for a connection from the pool (milliseconds)
 */
public record JdbcSettings(
  String jdbcUrl,
  String username,
  String password,
  int maxPoolSize,
  int minIdleConnections,
  long connectionTimeoutMs
) {

  private static final Logger log =  LoggerFactory.getLogger(JdbcSettings.class);

  private static void warnIfNonProdDatabase(String jdbcUrl) {
    String url = jdbcUrl.toLowerCase(Locale.ROOT);
    if (url.contains(":h2:mem:") || url.contains(":sqlite:") || url.contains(":derby:memory:")) {
      log.warn("JDBC URL {} appears to use an in-memory or development-only database. Avoid using this in production.", jdbcUrl);
    }
  }

  /**
   * Creates a new builder with sensible defaults.
   *
   * Defaults:
   * - maxPoolSize = 100
   * - minIdleConnections = 10
   * - connectionTimeoutMs = 30_000
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link JdbcSettings}.
   */
  public static class Builder {
    private String jdbcUrl;
    private String username;
    private String password;

    private int maxPoolSize = 100;
    private int minIdleConnections = 10;
    private long connectionTimeoutMs = 30_000;

    public Builder() {
    }

    /**
     * Sets the JDBC connection URL (required).
     */
    public Builder jdbcUrl(String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
      return this;
    }

    /**
     * Sets the database username (required).
     */
    public Builder username(String username) {
      this.username = username;
      return this;
    }

    /**
     * Sets the database password (required; may be blank but not null).
     */
    public Builder password(String password) {
      this.password = password;
      return this;
    }

    /**
     * Sets the maximum number of connections in the pool (must be > 0).
     */
    public Builder maxPoolSize(int maxPoolSize) {
      this.maxPoolSize = maxPoolSize;
      return this;
    }

    /**
     * Sets the minimum number of idle connections (0..maxPoolSize).
     */
    public Builder minIdleConnections(int minIdleConnections) {
      this.minIdleConnections = minIdleConnections;
      return this;
    }

    /**
     * Sets the maximum time to wait for a connection from the pool in milliseconds (≥ 1000).
     */
    public Builder connectionTimeoutMs(long connectionTimeoutMs) {
      this.connectionTimeoutMs = connectionTimeoutMs;
      return this;
    }

    /**
     * Validates inputs and constructs immutable {@link JdbcSettings}.
     *
     * @return settings instance
     * @throws IllegalArgumentException if any constraint is violated
     */
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

    private static boolean isBlank(String value) {
      return value == null || value.trim().isEmpty();
    }
  }
}
