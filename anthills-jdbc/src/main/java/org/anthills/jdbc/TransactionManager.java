package org.anthills.jdbc;

@FunctionalInterface
public interface TransactionManager {
  <T> T execute(TransactionCallback<T> callback);
}
