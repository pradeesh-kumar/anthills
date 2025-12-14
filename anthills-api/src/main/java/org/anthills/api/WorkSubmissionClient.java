package org.anthills.api;

public interface WorkSubmissionClient<T> {

  static <T> WorkSubmissionClient<T> create(
    Class<T> payloadType,
    WorkStore workStore
  ) {
    return new DefaultWorkSubmissionClient<>(payloadType, workStore);
  }

  String submit(T payload);
  String submit(T payload, SubmissionOptions options);
}
