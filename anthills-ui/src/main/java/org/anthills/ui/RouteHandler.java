package org.anthills.ui;

import com.sun.net.httpserver.HttpExchange;
import org.anthills.api.scheduler.SchedulerLease;
import org.anthills.api.work.WorkQuery;
import org.anthills.api.work.WorkRecord;
import org.anthills.api.work.WorkStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class RouteHandler {

  private static final Logger log = LoggerFactory.getLogger(RouteHandler.class);

  private final WorkStore workStore;
  private final PageRenderer renderer;

  RouteHandler(WorkStore workStore) {
    this.workStore = workStore;
    this.renderer = new PageRenderer();
  }

  void handleDashboard(HttpExchange exchange) throws IOException {
    try {
      var query = WorkQuery.builder()
        .workType("notification")
        .limit(1)
        .build();
      renderer.render(exchange, "dashboard", Map.of("title", "Dashboard"));
    } catch (Exception e) {
      log.error("Failed to render dashboard", e);
      error(exchange, 500, "Something went wrong", e);
    }
  }

  void handleWork(HttpExchange exchange) throws IOException {
    try {
      var query = WorkQuery.builder()
        .workType("notification")
        .limit(100)
        .build();
      var worksRequests = workStore.listWork(query);
      renderer.render(exchange, "work-list", Map.of(
        "title", "Work List",
        "workRequests", worksRequests
      ));
    } catch (Exception e) {
      log.error("Failed to render work", e);
      error(exchange, 500, "Something went wrong", e);
    }
  }

  void handleWorkDetails(HttpExchange exchange, String id) throws IOException {
    try {
      Optional<WorkRecord> workRecord = workStore.getWork(id);
      if (workRecord.isEmpty()) {
        error(exchange, 404, "Work with id " + id + " does not exist");
        return;
      }
      renderer.render(exchange, "work-details", Map.of(
        "title", "Work Details " + id,
        "workRecord", workRecord.get()
      ));
    } catch (Exception e) {
      error(exchange, 500, "Something went wrong", e);
    }
  }

  void handleScheduler(HttpExchange exchange) throws IOException {
    try {
      List<SchedulerLease> leases = workStore.listSchedulerLeases();
      renderer.render(exchange, "scheduler", Map.of(
        "title", "Scheduler",
        "leases", leases));
    } catch (Exception e) {
      error(exchange, 500, "Something went wrong", e);
    }
  }

  void error(HttpExchange exchange, int statusCode, String message) throws IOException {
    error(exchange, statusCode, message, null);
  }

  private void error(HttpExchange exchange, int statusCode, String message, Throwable t) throws IOException {
    renderer.render(exchange, "error", Map.of(
      "title", "Error",
      "statusCode", statusCode,
      "message", message,
      "stacktrace", extractStackTrace(t)
    ));
  }

  private static String extractStackTrace(Throwable t) {
    if (t == null) {
      return "";
    }
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }

}
