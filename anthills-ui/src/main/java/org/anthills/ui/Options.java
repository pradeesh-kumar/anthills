package org.anthills.ui;

import org.anthills.api.work.WorkStore;

/**
 * Immutable configuration for {@link DefaultAnthillsUI}.
 *
 * @param port TCP port to bind
 * @param bindAddress network interface/address to bind (e.g. "localhost", "0.0.0.0")
 * @param contextPath root context path (e.g. "/")
 * @param enableWriteActions whether UI write operations are enabled
 * @param auth optional basic authentication configuration
 * @param threads number of threads for the HTTP(S) server executor
 * @param workStore backing store used to query work and scheduler state
 * @param tls optional TLS configuration – when present, an HTTPS server is created
 */
public record Options(
  int port,
  String bindAddress,
  String contextPath,
  boolean enableWriteActions,
  BasicAuth auth,
  int threads,
  WorkStore workStore,
  TlsOptions tls
) {}
