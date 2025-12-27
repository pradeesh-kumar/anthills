package org.anthills.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DbInfoTest {

  private HikariDataSource ds;

  @AfterEach
  void tearDown() {
    TestJdbc.closeQuietly(ds);
  }

  @Test
  void detect_h2_dialect_and_identity() {
    ds = TestJdbc.newH2DataSource();

    DbInfo info = DbInfo.detect(ds);

    assertNotNull(info);
    assertEquals(DbInfo.Dialect.H2, info.dialect());
    assertNotNull(info.version());
    assertFalse(info.version().isBlank());
    assertNotNull(info.identity());
    assertTrue(info.identity().startsWith("jdbc:h2:mem:"), "identity should be JDBC URL");
  }
}
