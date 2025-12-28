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

/**
 * Handles UI routes and delegates rendering to {@link PageRenderer}.
 * Uses the configured {@link org.anthills.api.work.WorkStore} to fetch data.
 */
final class RouteHandler {

  private static final Logger log = LoggerFactory.getLogger(RouteHandler.class);

  private final WorkStore workStore;
  private final PageRenderer renderer;

  RouteHandler(WorkStore workStore) {
    this.workStore = workStore;
    this.renderer = new PageRenderer();
  }

  /**
   * Renders the dashboard page.
   *
   * @param exchange current HTTP exchange
   * @throws IOException when writing the response fails
   */
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

  /**
   * Renders the work list page.
   *
   * @param exchange current HTTP exchange
   * @throws IOException when writing the response fails
   */
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

  /**
   * Renders the work details page for the given work id.
   *
   * @param exchange current HTTP exchange
   * @param id work identifier
   * @throws IOException when writing the response fails
   */
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

  /**
   * Renders the scheduler leases page.
   *
   * @param exchange current HTTP exchange
   * @throws IOException when writing the response fails
   */
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

  /**
   * Renders an error response with the given status code and message.
   *
   * @param exchange current HTTP exchange
   * @param statusCode HTTP status code
   * @param message error message to display
   * @throws IOException when writing the response fails
   */
  void error(HttpExchange exchange, int statusCode, String message) throws IOException {
    error(exchange, statusCode, message, null);
  }

  /**
   * Renders an error response and optionally includes a stacktrace.
   */
  private void error(HttpExchange exchange, int statusCode, String message, Throwable t) throws IOException {
    renderer.render(exchange, "error", Map.of(
      "title", "Error",
      "statusCode", statusCode,
      "message", message,
      "stacktrace", extractStackTrace(t)
    ));
  }

  /**
   * Extracts the stacktrace text for the given throwable or returns an empty string if null.
   */
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
