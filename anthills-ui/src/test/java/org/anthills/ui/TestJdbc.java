package org.anthills.ui;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;

final class TestJdbc {

  static DataSource newH2DataSource() {
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");
    return ds;
  }

  private TestJdbc() {}
}
