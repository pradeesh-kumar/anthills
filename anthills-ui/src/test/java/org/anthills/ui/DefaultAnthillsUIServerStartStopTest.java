package org.anthills.ui;

import org.anthills.api.work.WorkStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAnthillsUIServerStartStopTest {

  private static Object reflectServer(DefaultAnthillsUI ui) throws Exception {
    Field f = DefaultAnthillsUI.class.getDeclaredField("server");
    f.setAccessible(true);
    return f.get(ui);
  }

  @Test
  void http_startStop_andNotHttpsServer() throws Exception {
    WorkStore store = Mockito.mock(WorkStore.class);

    DefaultAnthillsUI ui = (DefaultAnthillsUI) AnthillsUI.builder()
      .workStore(store)
      .bindAddress("127.0.0.1")
      .port(0) // OS-chosen ephemeral port
      .build();

    try {
      assertFalse(ui.isRunning());
      ui.start();
      assertTrue(ui.isRunning(), "UI should report running after start");
      Object server = reflectServer(ui);
      assertNotNull(server, "Underlying server should be initialized");
      // Ensure this is not an HttpsServer (plain HTTP path)
      assertFalse(server.getClass().getName().contains("HttpsServer"),
        "Expected plain HttpServer when TLS is not configured");
    } finally {
      ui.stop();
      assertFalse(ui.isRunning(), "UI should report not running after stop");
    }
  }
}
