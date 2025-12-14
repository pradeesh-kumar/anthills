package org.anthills.api;

public interface JobHandle {
    String jobName();
    void pause();
    void resume();
    void cancel();
}
