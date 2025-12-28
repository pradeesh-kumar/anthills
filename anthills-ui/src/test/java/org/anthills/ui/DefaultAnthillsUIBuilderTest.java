package org.anthills.ui;

import org.anthills.api.work.WorkStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAnthillsUIBuilderTest {

  private static Options reflectOptions(DefaultAnthillsUI ui) throws Exception {
    Field f = DefaultAnthillsUI.class.getDeclaredField("options");
    f.setAccessible(true);
    return (Options) f.get(ui);
  }

  @Test
  void tlsOptionWiring_setsTlsOptionsOnBuild() throws Exception {
    WorkStore store = Mockito.mock(WorkStore.class);

    AnthillsUI ui = AnthillsUI.builder()
      .workStore(store)
      .bindAddress("127.0.0.1")
      .port(0)
      .tls("/path/to/cert.pem", "/path/to/key.pem")
      .build();

    assertTrue(ui instanceof DefaultAnthillsUI);
    Options opts = reflectOptions((DefaultAnthillsUI) ui);

    assertNotNull(opts.tls(), "TLS options should be present when tls(...) is configured");
    assertEquals("/path/to/cert.pem", opts.tls().certificatePemPath());
    assertEquals("/path/to/key.pem", opts.tls().privateKeyPemPath());
  }

  @Test
  void noTlsByDefault_tlsIsNull() throws Exception {
    WorkStore store = Mockito.mock(WorkStore.class);

    AnthillsUI ui = AnthillsUI.builder()
      .workStore(store)
      .bindAddress("127.0.0.1")
      .port(0)
      .build();

    Options opts = reflectOptions((DefaultAnthillsUI) ui);
    assertNull(opts.tls(), "TLS options should be null by default");
  }
}
