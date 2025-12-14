package org.anthills.api;

@FunctionalInterface
public interface WorkRequestHandler<T> {
  void handle(WorkRequest<T> request) throws Exception;
}
