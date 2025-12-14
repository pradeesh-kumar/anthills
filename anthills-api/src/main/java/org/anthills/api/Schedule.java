package org.anthills.api;

import java.time.Duration;

public sealed interface Schedule permits Schedule.FixedRate, Schedule.Cron {
  record FixedRate(Duration interval) implements Schedule {}
  record Cron(String expression) implements Schedule {}
}
