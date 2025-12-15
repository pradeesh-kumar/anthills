package org.anthills.api;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import java.time.Duration;

public sealed interface Schedule permits Schedule.FixedRate, Schedule.Cron {

  record FixedRate(Duration interval) implements Schedule {}

  record Cron(String expression) implements Schedule {

    private static final CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    public Cron {
      parser.parse(expression);
    }
  }
}
