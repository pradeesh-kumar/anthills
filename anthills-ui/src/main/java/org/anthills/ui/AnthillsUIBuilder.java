package org.anthills.ui;

import org.anthills.api.work.WorkStore;

/**
 * Fluent builder for configuring and constructing an {@link AnthillsUI} instance.
 * <p>
 * Unless otherwise specified, omitted options fall back to sensible defaults.
 * </p>
 *
 * <ul>
 *   <li>bindAddress: {@code "localhost"}</li>
 *   <li>port: {@code 8080}</li>
 *   <li>contextPath: {@code "/"}</li>
 *   <li>enableWriteActions: {@code false}</li>
 *   <li>threads: {@code 4}</li>
 * </ul>
 */
public interface AnthillsUIBuilder {

  /**
   * Sets the TCP port to bind the UI server to.
   *
   * @param port TCP port. Use {@code 0} to let the OS choose a free ephemeral port.
   * @return this builder
   */
  AnthillsUIBuilder port(int port);

  /**
   * Sets the network interface / address to bind to.
   *
   * @param address e.g. {@code "localhost"}, {@code "127.0.0.1"}, or {@code "0.0.0.0"}
   * @return this builder
   */
  AnthillsUIBuilder bindAddress(String address); // default localhost

  /**
   * Provides the {@link WorkStore} that backs the UI pages.
   *
   * @param store work store implementation
   * @return this builder
   */
  AnthillsUIBuilder workStore(WorkStore store);

  /**
   * Sets the context path under which the UI is served.
   *
   * @param path leading slash path (e.g. {@code "/"}, {@code "/ui"})
   * @return this builder
   */
  AnthillsUIBuilder contextPath(String path); // default: "/"

  /**
   * Enables or disables write actions from UI (future capability).
   *
   * @param enabled true to allow write actions
   * @return this builder
   */
  AnthillsUIBuilder enableWriteActions(boolean enabled); // default: false

  /**
   * Configures HTTP Basic authentication for the UI.
   *
   * @param username expected username
   * @param password expected password
   * @return this builder
   */
  AnthillsUIBuilder basicAuth(String username, String password);

  /**
   * Sets the number of worker threads used by the underlying HTTP(S) server.
   *
   * @param threads number of threads
   * @return this builder
   */
  AnthillsUIBuilder threads(int threads);

  /**
   * Enables HTTPS by supplying certificate and private key in PEM format.
   * <p>
   * Certificate must be X.509 PEM. Private key must be PKCS#8 PEM
   * (header {@code -----BEGIN PRIVATE KEY-----}). PKCS#1 keys are not supported.
   * </p>
   *
   * @param certificatePemPath path to a PEM file containing the leaf certificate and optionally the full chain
   * @param privateKeyPemPath  path to a PKCS#8 private key PEM corresponding to the certificate
   * @return this builder
   */
  AnthillsUIBuilder tls(String certificatePemPath, String privateKeyPemPath);

  /**
   * Builds an {@link AnthillsUI} with the configured options.
   *
   * @return a new {@link AnthillsUI} instance
   */
  AnthillsUI build();
}
