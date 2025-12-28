package org.anthills.examples;

import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;

public final class Common {

  private Common() {}

  public static DataSource dataSource() {
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");
    return ds;
  }
}
