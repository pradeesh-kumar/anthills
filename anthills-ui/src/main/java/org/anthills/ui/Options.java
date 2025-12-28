package org.anthills.ui;

import org.anthills.api.work.WorkStore;

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

