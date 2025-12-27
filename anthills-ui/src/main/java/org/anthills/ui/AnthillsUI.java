package org.anthills.ui;

public interface AnthillsUI {

  void start();
  void stop();
  boolean isRunning();

  static AnthillsUIBuilder builder() {
    return DefaultAnthillsUI.builder();
  }
}
