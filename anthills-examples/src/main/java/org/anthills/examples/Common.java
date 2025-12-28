package org.anthills.examples;


import javax.sql.DataSource;

public final class Common {

  private Common() {}

  public static DataSource dataSource() {
    org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
    ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");
    return ds;
  }
}
