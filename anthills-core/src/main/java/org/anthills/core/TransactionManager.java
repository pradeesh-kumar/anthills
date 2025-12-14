package org.anthills.core;

@FunctionalInterface
public interface TransactionManager {
  <T> T execute(TransactionCallback<T> callback);
}
