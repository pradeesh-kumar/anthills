package org.anthills.api.scheduler;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

public sealed interface Schedule permits Schedule.FixedRate, Schedule.Cron {

  /**
   * Returns the delay until the next execution.
   * Must always return a positive duration.
   */
  Duration nextDelay();

  record FixedRate(Duration interval) implements Schedule {

    public static FixedRate every(Duration interval) {
      return new FixedRate(interval);
    }

    @Override
    public Duration nextDelay() {
      return interval;
    }
  }

  record Cron(String expression) implements Schedule {

    private static final CronParser PARSER = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    public Cron {
      Objects.requireNonNull(expression, "expression");
      PARSER.parse(expression);
    }

    public static Cron parse(String expression) {
      return new Cron(expression);
    }

    @Override
    public Duration nextDelay() {
      var cron = PARSER.parse(expression);
      ExecutionTime executionTime = ExecutionTime.forCron(cron);

      ZonedDateTime now = ZonedDateTime.now();
      return executionTime
        .timeToNextExecution(now)
        .map(Duration::from)
        .orElseThrow(() -> new IllegalStateException("Cannot compute next execution for cron: " + expression));
    }
  }
}
