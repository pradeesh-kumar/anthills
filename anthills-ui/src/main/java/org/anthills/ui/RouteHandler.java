package org.anthills.ui;

import com.sun.net.httpserver.HttpExchange;
import org.anthills.api.work.WorkQuery;
import org.anthills.api.work.WorkStore;

import java.io.IOException;
import java.util.Map;

public final class RouteHandler {

  private final WorkStore workStore;
  private final PageRenderer renderer;

  RouteHandler(WorkStore workStore) {
    this.workStore = workStore;
    this.renderer = new PageRenderer();
  }

  void handleDashboard(HttpExchange exchange) throws IOException {
    if (!"GET".equals(exchange.getRequestMethod())) {
      sendError(exchange, 405);
      return;
    }
    var query = WorkQuery.builder()
      .workType("notification")
      .limit(100)
      .build();
    var works = workStore.listWork(query);
    renderer.render(exchange, "dashboard", Map.of());
  }

  void handleWork(HttpExchange exchange) throws IOException {
    if (!"GET".equals(exchange.getRequestMethod())) {
      sendError(exchange, 405);
      return;
    }
    var query = WorkQuery.builder()
      .workType("notification")
      .limit(100)
      .build();
    var worksRequests = workStore.listWork(query);
    renderer.render(exchange, "work", Map.of("workRequests", worksRequests));
  }

  private void sendError(HttpExchange exchange, int statusCode) throws IOException {
    exchange.sendResponseHeaders(statusCode, -1);
  }

}
