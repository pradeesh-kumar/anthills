package org.anthills.api.work;

@FunctionalInterface
public interface WorkRequestHandler<T> {
  void handle(WorkRequest<T> request) throws Exception;
}
