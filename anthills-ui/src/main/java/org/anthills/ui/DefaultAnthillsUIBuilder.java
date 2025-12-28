package org.anthills.ui;

import org.anthills.api.work.WorkStore;

public final class DefaultAnthillsUIBuilder implements AnthillsUIBuilder {

  private int port = 8080;
  private String bindAddress = "localhost";
  private String contextPath = "/";
  private boolean enableWriteActions = false;
  private WorkStore store;
  private String username;
  private String password;
  private int threads = 4;
  private TlsOptions tls;

  DefaultAnthillsUIBuilder() {}

  public AnthillsUIBuilder port(int port) {
    this.port = port;
    return this;
  }

  @Override
  public AnthillsUIBuilder bindAddress(String address) {
    this.bindAddress = address;
    return this;
  }

  @Override
  public AnthillsUIBuilder workStore(WorkStore store) {
    this.store = store;
    return this;
  }

  @Override
  public AnthillsUIBuilder contextPath(String path) {
    this.contextPath = path;
    return this;
  }

  @Override
  public AnthillsUIBuilder enableWriteActions(boolean enabled) {
    this.enableWriteActions = enabled;
    return this;
  }

  @Override
  public AnthillsUIBuilder basicAuth(String username, String password) {
    this.username = username;
    this.password = password;
    return this;
  }

  @Override
  public AnthillsUIBuilder threads(int threads) {
    this.threads = threads;
    return this;
  }

  @Override
  public AnthillsUIBuilder tls(String certificatePemPath, String privateKeyPemPath) {
    this.tls = new TlsOptions(certificatePemPath, privateKeyPemPath);
    return this;
  }

  @Override
  public AnthillsUI build() {
    BasicAuth auth = null;
    if (!((username == null || password == null) || username.isEmpty() || password.isEmpty())) {
      auth = new BasicAuth(username, password, "default");
    }
    Options options = new Options(port, bindAddress, contextPath, enableWriteActions, auth, threads, store, tls);
    return new DefaultAnthillsUI(options);
  }
}
