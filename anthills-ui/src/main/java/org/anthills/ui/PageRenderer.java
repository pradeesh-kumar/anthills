package org.anthills.ui;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Renders UI pages using Mustache templates packaged under {@code ui-templates}.
 */
final class PageRenderer {

  private static final Logger log = LoggerFactory.getLogger(PageRenderer.class);

  private final MustacheFactory factory = new DefaultMustacheFactory("ui-templates");

  /**
   * Renders the given template with the provided context and writes an HTTP 200 response.
   *
   * @param exchange HTTP exchange to write the response to
   * @param page template name without extension (e.g. {@code "dashboard"})
   * @param context key/value model used by the Mustache template
   * @throws IOException if writing the response fails
   */
  public void render(HttpExchange exchange, String page, Map<String, Object> context) throws IOException {
    String rendered = renderContent(page, context);
    byte[] bytes = rendered.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  /**
   * Compiles and executes the given template name against the supplied context.
   *
   * @param template template name without extension (e.g. {@code "dashboard"})
   * @param context key/value model used by the Mustache template
   * @return rendered HTML content
   * @throws RuntimeException when template rendering fails
   */
  private String renderContent(String template, Map<String, Object> context) {
    try {
      Mustache mustache = factory.compile(template + ".mustache");
      StringWriter writer = new StringWriter();
      mustache.execute(writer, context);
      return writer.toString();
    } catch (Exception e) {
      log.error("Failed to render content", e);
      throw new RuntimeException(e);
    }
  }
}
