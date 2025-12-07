package org.anthills.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestMe {
  public static void main(String[] args) {

    // Create a scheduler with a single background thread
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    Runnable task = () -> {
      System.out.println("Task executed at: " + System.currentTimeMillis());
    };

    // Run first after 2 seconds, then every 5 seconds
    scheduler.scheduleAtFixedRate(
      task,
0,       // initial delay
      2,       // period
      TimeUnit.SECONDS
    );

  }
}
