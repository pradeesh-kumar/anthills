package org.anthills.jdbc;

@FunctionalInterface
public interface TransactionCallback<T> {
  T doInTransaction() throws Exception;
}
