package org.anthills.examples.work;

import org.anthills.api.work.ProcessorConfig;
import org.anthills.api.work.WorkClient;
import org.anthills.api.work.WorkRequestProcessor;
import org.anthills.core.JsonPayloadCodec;
import org.anthills.core.factory.WorkClients;
import org.anthills.core.factory.WorkRequestProcessors;
import org.anthills.examples.Common;
import org.anthills.jdbc.JdbcWorkStore;

import javax.sql.DataSource;

public class NotificationExample {

  static void main(String[] args) {

    DataSource dataSource = Common.dataSource();
    var store = JdbcWorkStore.create(dataSource);
    var codec = JsonPayloadCodec.defaultInstance();

    WorkClient notificationWorkClient = WorkClients.create(store, codec);
    notificationWorkClient.submit("notification", new SendEmail("user@example.com", "Welcome", "Hello from Anthills!"));
    notificationWorkClient.submit("notification", new SendSms("+441234567890", "Hello from Anthills!"));

    ProcessorConfig config = ProcessorConfig.defaults();

    WorkRequestProcessor processor = WorkRequestProcessors.create("notification", store, codec, config);

    processor.registerHandler("notification", SendEmail.class, req -> sendEmail(req.payload()));
    processor.registerHandler("notification", SendSms.class, req -> sendSms(req.payload()));

    processor.start();
    Runtime.getRuntime().addShutdownHook(new Thread(processor::stop));
  }

  static void sendEmail(SendEmail email) {
    System.out.println(
      "Sending email to " + email.to()
    );

    // simulate slow IO
    sleep(5000);
  }

  static void sendSms(SendSms sms) {
    System.out.println(
      "Sending SMS to " + sms.phone()
    );
  }

  static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ignored) {
    }
  }
}
