package org.anthills.ui;

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
    server.createContext(resolvePath("/"), routeHandler::handleDashboard);
    server.createContext(resolvePath("/work"), routeHandler::handleWork);
  }

  private String resolvePath(String path) {
    String resolved = options.contextPath() + path;
    return resolved.replaceAll("//", "/");
  }

  public static AnthillsUIBuilder builder() {
    return new DefaultAnthillsUIBuilder();
  }

}
