package org.anthills.jdbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class JdbcSettingsTest {

  @Test
  void build_success_with_defaults_and_h2_url() {
    JdbcSettings s = JdbcSettings.builder()
      .jdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
      .username("sa")
      .password("")
      .build();

    assertEquals("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", s.jdbcUrl());
    assertEquals("sa", s.username());
    assertEquals("", s.password());
    assertEquals(100, s.maxPoolSize());
    assertEquals(10, s.minIdleConnections());
    assertEquals(30_000, s.connectionTimeoutMs());
  }

  @Test
  void validation_fails_on_blank_jdbcUrl() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
      JdbcSettings.builder()
        .jdbcUrl("  ")
        .username("u")
        .password("p")
        .build()
    );
    assertTrue(ex.getMessage().toLowerCase().contains("jdbc url"));
  }

  @Test
  void validation_fails_on_blank_username() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
      JdbcSettings.builder()
        .jdbcUrl("jdbc:h2:mem:x")
        .username(" ")
        .password("p")
        .build()
    );
    assertTrue(ex.getMessage().toLowerCase().contains("username"));
  }

  @Test
  void validation_fails_on_null_password() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
      JdbcSettings.builder()
        .jdbcUrl("jdbc:h2:mem:x")
        .username("u")
        .password(null)
        .build()
    );
    assertTrue(ex.getMessage().toLowerCase().contains("password"));
  }

  @Test
  void validation_fails_on_invalid_pool_sizes() {
    // maxPoolSize <= 0
    IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
      JdbcSettings.builder()
        .jdbcUrl("jdbc:h2:mem:x")
        .username("u")
        .password("p")
        .maxPoolSize(0)
        .build()
    );
    assertTrue(ex1.getMessage().toLowerCase().contains("maximum"));

    // minIdle < 0
    IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
      JdbcSettings.builder()
        .jdbcUrl("jdbc:h2:mem:x")
        .username("u")
        .password("p")
        .maxPoolSize(5)
        .minIdleConnections(-1)
        .build()
    );
    assertTrue(ex2.getMessage().toLowerCase().contains("minimum"));

    // minIdle > max
    IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
      JdbcSettings.builder()
        .jdbcUrl("jdbc:h2:mem:x")
        .username("u")
        .password("p")
        .maxPoolSize(5)
        .minIdleConnections(6)
        .build()
    );
    assertTrue(ex3.getMessage().toLowerCase().contains("minimum"));
  }

  @Test
  void validation_fails_on_too_small_connection_timeout() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
      JdbcSettings.builder()
        .jdbcUrl("jdbc:h2:mem:x")
        .username("u")
        .password("p")
        .connectionTimeoutMs(999)
        .build()
    );
    assertTrue(ex.getMessage().toLowerCase().contains("timeout"));
  }
}
