package org.anthills.examples;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class Common {

    private Common() {}

    public static DataSource dataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:postgresql://localhost:5432/anthills");
        cfg.setUsername("anthills");
        cfg.setPassword("anthills");
        cfg.setMaximumPoolSize(10);
        return new HikariDataSource(cfg);
    }
}
