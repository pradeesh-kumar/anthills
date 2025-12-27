package org.anthills.api.scheduler;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Describes when a {@link Job} should run.
 * Implementations include fixed-rate execution and CRON-based schedules.
 * Implementations must compute the delay to the next execution from "now".
 */
public sealed interface Schedule permits Schedule.FixedRate, Schedule.Cron {

  /**
   * Computes the delay until the next execution from the current instant.
   * Must always return a strictly positive duration.
   *
   * @return delay until the next scheduled run
   */
  Duration nextDelay();

  /**
   * Fixed-rate schedule that runs with a constant interval between executions.
   *
   * @param interval time between runs; must be positive
   */
  record FixedRate(Duration interval) implements Schedule {

    /**
     * Creates a fixed-rate schedule with the given interval.
     *
     * @param interval time between runs; must be positive
     * @return a fixed-rate schedule
     */
    public static FixedRate every(Duration interval) {
      return new FixedRate(interval);
    }

    /**
     * Returns the configured interval as the next delay.
     */
    @Override
    public Duration nextDelay() {
      return interval;
    }
  }

  /**
   * CRON-based schedule using Quartz flavor expressions.
   *
   * @param expression Quartz-style CRON expression (e.g. "0/5 * * * * ?")
   */
  record Cron(String expression) implements Schedule {

    private static final CronParser PARSER = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    /**
     * Validates the CRON expression at construction time.
     *
     * @throws NullPointerException if expression is null
     * @throws IllegalArgumentException if the expression is syntactically invalid
     */
    public Cron {
      Objects.requireNonNull(expression, "expression");
      PARSER.parse(expression);
    }

    /**
     * Parses a CRON expression into a schedule.
     *
     * @param expression Quartz-style CRON expression
     * @return a CRON schedule
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static Cron parse(String expression) {
      return new Cron(expression);
    }

    /**
     * Computes the delay until the next fire time according to the CRON expression.
     *
     * @return strictly positive delay until the next execution
     * @throws IllegalStateException if the next execution cannot be computed
     */
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
