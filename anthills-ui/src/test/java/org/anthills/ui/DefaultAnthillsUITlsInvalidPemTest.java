package org.anthills.ui;

import org.anthills.api.work.WorkStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TLS negative-path tests to ensure invalid PEMs fail fast.
 */
class DefaultAnthillsUITlsInvalidPemTest {

  @Test
  void createServer_withInvalidPem_throwsException() throws Exception {
    // Prepare invalid PEM files
    Path cert = Files.createTempFile("invalid-cert", ".pem");
    Path key = Files.createTempFile("invalid-key", ".pem");

    Files.writeString(cert, "-----BEGIN CERTIFICATE-----\ninvalid\n-----END CERTIFICATE-----\n");
    Files.writeString(key, "-----BEGIN PRIVATE KEY-----\ninvalid\n-----END PRIVATE KEY-----\n");

    WorkStore store = Mockito.mock(WorkStore.class);

    DefaultAnthillsUI ui = (DefaultAnthillsUI) AnthillsUI.builder()
      .workStore(store)
      .bindAddress("127.0.0.1")
      .port(0)
      .tls(cert.toString(), key.toString())
      .build();

    // Use reflection to invoke createServer() directly so we don't mutate running state
    Method m = DefaultAnthillsUI.class.getDeclaredMethod("createServer");
    m.setAccessible(true);

    InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> m.invoke(ui));
    Throwable cause = ite.getCause();
    assertNotNull(cause, "Expected root cause from TLS initialization");
    // Accept a range of possible failures depending on parsing step
    String msg = cause.getMessage() == null ? "" : cause.getMessage();
    assertTrue(msg.contains("No X.509 certificates") ||
        msg.contains("PKCS#8") ||
        msg.contains("Illegal base64 character") ||
        msg.contains("Unsupported private key algorithm") ||
        msg.contains("Could not parse certificate"),
      "Unexpected error message: " + msg);
  }
}
