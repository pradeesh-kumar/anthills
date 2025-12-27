package org.anthills.ui;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class PageRenderer {

  private final MustacheFactory factory = new DefaultMustacheFactory("ui-templates");

  public void render(HttpExchange exchange, String path, Map<String, Object> context) throws IOException {
    String rendered = renderContent(path, context);
    byte[] bytes = rendered.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  private String renderContent(String template, Map<String, Object> context) {
    Mustache mustache = factory.compile(template + ".mustache");
    StringWriter writer = new StringWriter();
    mustache.execute(writer, context);
    return writer.toString();
  }
}
