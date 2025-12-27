package org.anthills.examples.scheduler;

import org.anthills.api.scheduler.LeasedScheduler;
import org.anthills.api.scheduler.Schedule;
import org.anthills.api.scheduler.SchedulerConfig;
import org.anthills.core.factory.Schedulers;
import org.anthills.examples.Common;
import org.anthills.examples.Hostname;
import org.anthills.jdbc.JdbcWorkStore;

import javax.sql.DataSource;

public class HelloWorldCronSchedulerExample {

  static void main(String[] args) {
    DataSource dataSource = Common.dataSource();
    var store = JdbcWorkStore.create(dataSource);

    LeasedScheduler scheduler = Schedulers.createLeasedScheduler(SchedulerConfig.defaults(), store);
    // Runs every minute, but only on ONE node in the cluster
    Schedule everyMinute = Schedule.Cron.parse("* * * * *");
    scheduler.schedule("hello-world-job", everyMinute, HelloWorldCronSchedulerExample::helloWorldJob);
    scheduler.start();
    Runtime.getRuntime().addShutdownHook(new Thread(scheduler::stop));
  }

  static void helloWorldJob() {
    System.out.println("Hello world from node " + Hostname.current());
  }
}
