package org.anthills.ui;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public final class BasicAuth {

  private final String expectedUser;
  private final String expectedPassword;
  private final String realm;

  public BasicAuth(String user, String password, String realm) {
    this.expectedUser = Objects.requireNonNull(user);
    this.expectedPassword = Objects.requireNonNull(password);
    this.realm = realm;
  }

  public boolean authorize(HttpExchange exchange) throws IOException {
    List<String> headers = exchange.getRequestHeaders().get("Authorization");

    if (headers == null || headers.isEmpty()) {
      challenge(exchange);
      return false;
    }

    String header = headers.get(0);
    if (!header.startsWith("Basic ")) {
      challenge(exchange);
      return false;
    }

    String encoded = header.substring(6);
    String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);

    int idx = decoded.indexOf(':');
    if (idx < 0) {
      challenge(exchange);
      return false;
    }

    String user = decoded.substring(0, idx);
    String password = decoded.substring(idx + 1);

    if (expectedUser.equals(user) && expectedPassword.equals(password)) {
      return true;
    }

    challenge(exchange);
    return false;
  }

  private void challenge(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
    exchange.sendResponseHeaders(401, -1);
    exchange.close();
  }
}
