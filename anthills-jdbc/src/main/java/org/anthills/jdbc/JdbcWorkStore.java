package org.anthills.jdbc;

import org.anthills.api.WorkStore;

import javax.sql.DataSource;

public final class JdbcWorkStore implements WorkStore {
    private final DataSource dataSource;

    //WorkStore store =
  //    JdbcWorkStore.create(dataSource)
  //                 .withDialect(SqlDialect.POSTGRES);
}
