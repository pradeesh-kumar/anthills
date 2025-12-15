package org.anthills.api;

public interface WorkSubmissionClient {
  <T> WorkRequest<T> submit(T payload);
  <T> WorkRequest<T> submit(T payload, SubmissionOptions options);
}
