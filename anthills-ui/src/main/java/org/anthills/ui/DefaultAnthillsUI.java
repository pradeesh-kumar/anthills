package org.anthills.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultAnthillsUI implements AnthillsUI {

  private static final Logger log = LoggerFactory.getLogger(DefaultAnthillsUI.class);

  private final Options options;
  private final RouteHandler routeHandler;

  private HttpServer server;
  private ExecutorService executor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  DefaultAnthillsUI(Options options) {
    this.options = options;
    this.routeHandler = new RouteHandler(options.workStore());
  }

  @Override
  public void start() {
    if (!running.compareAndSet(false, true)) return;
    try {
      server = HttpServer.create(new InetSocketAddress(options.bindAddress(), options.port()), 0);
      executor = Executors.newFixedThreadPool(options.threads());
      server.setExecutor(executor);
      registerRoutes();
      server.start();
      log.info("Anthills UI started at http://{}:{}{}", options.bindAddress(), options.port(), options.contextPath());
    } catch (IOException e) {
      throw new RuntimeException("Failed to start Anthills UI", e);
    }
  }

  @Override
  public void stop() {
    if (!running.compareAndSet(true, false)) return;
    server.stop(0);
    executor.shutdownNow();
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  private void registerRoutes() {
    server.createContext(resolvePath("/"), this::handle);
  }

  private void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    log.debug("Received request for path {}", path);
    if (options.auth() != null && !options.auth().authorize(exchange)) {
      return;
    }
    if (path.equals("/") || path.isEmpty()) {
      if (!"GET".equals(exchange.getRequestMethod())) {
        routeHandler.error(exchange, 405, "Method not allowed");
        return;
      }
      routeHandler.handleDashboard(exchange);
      return;
    }
    if (path.equals("/work")) {
      if (!"GET".equals(exchange.getRequestMethod())) {
        routeHandler.error(exchange, 405, "Method not allowed");
        return;
      }
      routeHandler.handleWork(exchange);
      return;
    }
    if (path.startsWith("/work/")) {
      if (!"GET".equals(exchange.getRequestMethod())) {
        routeHandler.error(exchange, 405, "Method not allowed");
        return;
      }
      String id =  path.substring("/work/".length());
      if (id.isEmpty() || id.contains("/")) {
        routeHandler.error(exchange, 404, "Not found");
        return;
      }
      routeHandler.handleWorkDetails(exchange, id);
      return;
    }
    if (path.equals("/scheduler")) {
      if (!"GET".equals(exchange.getRequestMethod())) {
        routeHandler.error(exchange, 405, "Method not allowed");
        return;
      }
      routeHandler.handleScheduler(exchange);
      return;
    }
    routeHandler.error(exchange, 404, "Not Found");
  }

  private String resolvePath(String path) {
    String resolved = options.contextPath() + path;
    return resolved.replaceAll("//", "/");
  }

  public static AnthillsUIBuilder builder() {
    return new DefaultAnthillsUIBuilder();
  }

}
