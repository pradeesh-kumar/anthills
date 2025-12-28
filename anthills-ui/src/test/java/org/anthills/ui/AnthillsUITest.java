package org.anthills.ui;

import org.anthills.api.work.WorkStore;
import org.anthills.core.JsonPayloadCodec;
import org.anthills.jdbc.JdbcWorkStore;

import java.util.concurrent.CountDownLatch;

public class AnthillsUITest {

  static void main() throws Exception {
    WorkStore store = JdbcWorkStore.create(TestJdbc.newH2DataSource());
    submitWork(store, "notification", "Hello World");
    submitWork(store, "notification", "Hi World");
    submitWork(store, "notification", "Bye World");

    AnthillsUI ui = AnthillsUI.builder()
      .workStore(store)
      .port(8080)
      .bindAddress("localhost")
      .threads(4)
      .build();

    ui.start();

  }

  private static void submitWork(WorkStore store, String workType, Object payload) {
    JsonPayloadCodec codec = new JsonPayloadCodec();
    byte[] encoded = codec.encode(payload, 1);
    store.createWork(workType, encoded, payload.getClass().getName(),1, "json", 3);
  }
}
