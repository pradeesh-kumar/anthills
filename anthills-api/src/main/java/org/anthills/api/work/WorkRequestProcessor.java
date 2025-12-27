package org.anthills.api.work;

public interface WorkRequestProcessor extends Worker {
  <T> void registerHandler(String workType, Class<T> payloadType, WorkHandler<T> handler);
}
