package org.anthills.core;

import java.sql.SQLException;

@FunctionalInterface
public interface TransactionManager {
  <T> T execute(TransactionCallback<T> callback);
}
