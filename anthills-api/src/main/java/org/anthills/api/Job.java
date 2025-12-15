package org.anthills.api;

@FunctionalInterface
public interface Job {
    void run() throws Exception;
}
