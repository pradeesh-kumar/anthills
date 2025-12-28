package org.anthills.examples.ui;

import org.anthills.api.work.WorkStore;
import org.anthills.core.JsonPayloadCodec;
import org.anthills.jdbc.JdbcWorkStore;
import org.anthills.ui.AnthillsUI;
import org.anthills.examples.Common;

/**
 * Minimal example showing how to start the Anthills UI server.
 *
 * Run this class' main() from your IDE. It starts an HTTP server on localhost:8080.
 * Uncomment the tls(...) line to run over HTTPS after providing proper PEM files.
 */
public class AnthillsUIExample {

  static void main(String[] args) {
    // Create a WorkStore backed by in-memory H2
    WorkStore store = JdbcWorkStore.create(Common.dataSource());

    // Seed a few work requests so the UI has something to show
    seedWork(store);

    // Build and start the UI server
    AnthillsUI ui = AnthillsUI.builder()
      .workStore(store)
      .bindAddress("localhost")
      .port(8080)
      .contextPath("/")
      .threads(4)
      .basicAuth("admin", "admin") // Optional auth
      // Enable HTTPS by supplying certificate chain PEM and PKCS#8 private key PEM:
      // .tls("/path/to/cert.pem", "/path/to/server-key-pkcs8.pem")
      .build();

    ui.start();
    // The server runs until the process is terminated.
  }

  private static void seedWork(WorkStore store) {
    JsonPayloadCodec codec = new JsonPayloadCodec();
    create(store, codec, "notification", new Msg("Hello UI"));
    create(store, codec, "notification", new Msg("Welcome to Anthills UI"));
    create(store, codec, "notification", new Msg("TLS works too!"));
  }

  private static void create(WorkStore store, JsonPayloadCodec codec, String type, Object payload) {
    byte[] encoded = codec.encode(payload, 1);
    store.createWork(type, encoded, payload.getClass().getName(), 1, "json", 3);
  }

  // Simple payload record for sample work requests
  public record Msg(String text) {}
}
