package org.anthills.ui;

import org.anthills.api.work.WorkStore;

public interface AnthillsUIBuilder {
  AnthillsUIBuilder port(int port);
  AnthillsUIBuilder bindAddress(String address); // default localhost
  AnthillsUIBuilder workStore(WorkStore store);
  AnthillsUIBuilder contextPath(String path); // default: "/"
  AnthillsUIBuilder enableWriteActions(boolean enabled); // default: false
  AnthillsUIBuilder basicAuth(String username, String password);
  AnthillsUIBuilder threads(int threads);
  AnthillsUIBuilder tls(String certificatePemPath, String privateKeyPemPath);
  AnthillsUI build();
}
