package org.anthills.core;

import org.junit.jupiter.api.Test;

public class LeasedScheduledWorkerTest {

  @Test
  public void testLeasedScheduledWorker() {
    AnthillsEngine engine = AnthillsEngine.fromJdbcSettings(JdbcSettings.builder()
        .jdbcUrl("jdbc:h2:mem:anthills_test;DB_CLOSE_DELAY=-1")
        .username("sa")
        .password("")
      .build());

    LeasedScheduledWorker leasedScheduledWorker = engine.newLeasedScheduledWorker(SchedulerConfig.defaultConfig(), this::task);
    leasedScheduledWorker.start();
    System.out.println("LeasedScheduledWorker started");
    leasedScheduledWorker.awaitTermination();
  }

  public void task() {
    System.out.println("Hello World");
  }
}
