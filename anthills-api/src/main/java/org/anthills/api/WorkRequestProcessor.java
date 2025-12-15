package org.anthills.api;

public interface WorkRequestProcessor extends Worker {
  <T> void registerHandler(String workType, Class<T> payloadType, WorkHandler<T> handler);
}
