package org.anthills.ui;

import org.anthills.api.work.WorkStore;

/**
 * Default builder implementation for {@link AnthillsUI}.
 * Provides sensible defaults and fluent methods to configure the embedded UI.
 */
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

  /**
   * Sets the TCP port to bind the UI server to.
   *
   * @param port TCP port; use 0 for an ephemeral port (OS-chosen)
   * @return this builder
   */
  public AnthillsUIBuilder port(int port) {
    this.port = port;
    return this;
  }

  /**
   * Sets the network interface/address to bind to (e.g., "localhost", "0.0.0.0").
   *
   * @param address bind address
   * @return this builder
   */
  @Override
  public AnthillsUIBuilder bindAddress(String address) {
    this.bindAddress = address;
    return this;
  }

  /**
   * Sets the backing {@link WorkStore} used by the UI to query data.
   *
   * @param store work store implementation
   * @return this builder
   */
  @Override
  public AnthillsUIBuilder workStore(WorkStore store) {
    this.store = store;
    return this;
  }

  /**
   * Sets the root context path under which the UI is served.
   *
   * @param path leading slash path (e.g., "/", "/ui")
   * @return this builder
   */
  @Override
  public AnthillsUIBuilder contextPath(String path) {
    this.contextPath = path;
    return this;
  }

  /**
   * Enables or disables write operations in the UI (if/when available).
   *
   * @param enabled true to enable write actions
   * @return this builder
   */
  @Override
  public AnthillsUIBuilder enableWriteActions(boolean enabled) {
    this.enableWriteActions = enabled;
    return this;
  }

  /**
   * Configures HTTP Basic authentication for the UI.
   *
   * @param username expected username
   * @param password expected password
   * @return this builder
   */
  @Override
  public AnthillsUIBuilder basicAuth(String username, String password) {
    this.username = username;
    this.password = password;
    return this;
  }

  /**
   * Sets the number of worker threads used by the underlying HTTP(S) server.
   *
   * @param threads number of threads
   * @return this builder
   */
  @Override
  public AnthillsUIBuilder threads(int threads) {
    this.threads = threads;
    return this;
  }

  /**
   * Enables HTTPS by supplying certificate and private key in PEM format.
   * Certificate must be X.509 PEM (leaf first). Private key must be PKCS#8 PEM.
   *
   * @param certificatePemPath path to certificate PEM (optionally including full chain)
   * @param privateKeyPemPath path to PKCS#8 private key PEM
   * @return this builder
   */
  @Override
  public AnthillsUIBuilder tls(String certificatePemPath, String privateKeyPemPath) {
    this.tls = new TlsOptions(certificatePemPath, privateKeyPemPath);
    return this;
  }

  /**
   * Builds an {@link AnthillsUI} with the configured options.
   *
   * @return a new {@link AnthillsUI} instance
   */
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
