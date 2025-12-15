package org.anthills.jdbc;

import java.sql.Connection;

public class TransactionContext {

  private static final ThreadLocal<Connection> context = new ThreadLocal<>();

  public static void set(Connection connection) {
    context.set(connection);
  }

  public static Connection get() {
    return context.get();
  }

  public static void clear() {
    context.remove();
  }
}
