package org.anthills.api;

@FunctionalInterface
public interface ScheduledJob {
    void run() throws Exception;
}
