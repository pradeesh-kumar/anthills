package org.anthills.api;

@FunctionalInterface
public interface WorkHandler<T> {
    void handle(WorkRequest<T> request) throws Exception;
}
