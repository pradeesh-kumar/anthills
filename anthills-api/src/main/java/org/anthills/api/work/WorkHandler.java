package org.anthills.api.work;

@FunctionalInterface
public interface WorkHandler<T> {
    void handle(WorkRequest<T> request) throws Exception;
}
