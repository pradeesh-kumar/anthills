package org.anthills.ui;

/**
 * Webserver for Anthils UI
 * <p>
 * Implementations start an HTTP or HTTPS server based on configuration provided via {@link AnthillsUIBuilder}.
 * </p>
 */
public interface AnthillsUI {

  /**
   * Starts the server. Subsequent calls when already started are no-ops.
   * Throws a {@link RuntimeException} if the server fails to start.
   */
  void start();
  /**
   * Stops the server if running. Subsequent calls when already stopped are no-ops.
   * Shuts down the underlying HTTP(S) server and its executor.
   */
  void stop();
  /**
   * Returns true if the server has been started and not yet stopped.
   */
  boolean isRunning();

  /**
   * Creates a new builder for configuring and constructing an {@link AnthillsUI} instance.
   */
  static AnthillsUIBuilder builder() {
    return DefaultAnthillsUI.builder();
  }
}
