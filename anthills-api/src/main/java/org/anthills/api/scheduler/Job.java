package org.anthills.api.scheduler;

@FunctionalInterface
public interface Job {
    void run() throws Exception;
}
