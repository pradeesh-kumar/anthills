package org.anthills.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LeasedSchedulerTest {

  @BeforeAll
  public static void startH2Console() {
    H2ConsoleManager.startIfEnabled();
  }

  @Test
  public void testLeasedScheduler() {
    AnthillsEngine engine = AnthillsEngine.fromJdbcSettings(JdbcSettings.builder()
      .jdbcUrl("jdbc:h2:mem:anthills_test;DB_CLOSE_DELAY=-1")
      .username("sa")
      .password("")
      .build());

    LeasedScheduler leasedScheduler = engine.newLeasedScheduler(SchedulerConfig.defaultConfig("hello"), () -> {
      task("Hello World", 100);
    });

    AnthillsEngine engine2 = AnthillsEngine.fromJdbcSettings(JdbcSettings.builder()
      .jdbcUrl("jdbc:h2:mem:anthills_test;DB_CLOSE_DELAY=-1")
      .username("sa")
      .password("")
      .build());

    LeasedScheduler leasedScheduler2 = engine2.newLeasedScheduler(SchedulerConfig.defaultConfig("hello"), () -> {
      task("Bye World", 100);
    });
    leasedScheduler.start();
    leasedScheduler2.start();

    leasedScheduler.awaitTermination();
    leasedScheduler2.awaitTermination();
  }

  public void task(String msg, int sleepMillis) {
    System.out.println(msg);
    try {
      Thread.sleep(sleepMillis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
