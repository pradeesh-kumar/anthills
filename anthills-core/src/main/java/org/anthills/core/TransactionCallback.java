package org.anthills.core;

@FunctionalInterface
public interface TransactionCallback<T> {
  T doInTransaction() throws Throwable;
}
