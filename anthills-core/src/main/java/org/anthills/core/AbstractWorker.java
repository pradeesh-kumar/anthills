package org.anthills.core;

import java.util.UUID;
import java.util.concurrent.Callable;

public abstract class AbstractWorker {

  private final String identity;

  public AbstractWorker() {
    this.identity = UUID.randomUUID().toString();
  }

  public <T> T idempotentOperation(Callable<T> callable) throws Exception {
    // TODO implement Idempotent Operation
    T result = callable.call();
    return result;
  }
}
