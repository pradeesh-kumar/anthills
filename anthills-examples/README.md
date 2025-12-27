# Anthills Examples

This module contains minimal, runnable examples demonstrating how to use Anthills in real applications.
The goal of these examples is to show correct usage patterns, not to be production-ready apps.
---

## Modules demonstrated

Anthills provides two core primitives:

1. **LeasedScheduler** – run scheduled jobs on only one node in a cluster
2. **WorkRequestProcessor** – process background work distributed across many nodes

Each example focuses on one idea and keeps everything else simple.

---

## Prerequisites

- Java 25+
- A running relational database (PostgreSQL recommended)
- Database credentials configured in Common.java
  Anthills uses the database for leases, coordination, and fault tolerance.

---

## Project structure

```css
anthills-examples
└── src /main/ java /org/ anthills

examples
├── Common.java
├── Hostname.java
├── scheduler
│ ├── HelloWorldCronSchedulerExample.java
│ └── HelloWorldFixedRateSchedulerExample.java
├── work
│ ├── HelloWorldCronSchedulerExample.java
├── SendEmail.java
└── SendSms.java
```

---

## LeasedScheduler examples

### What is LeasedScheduler?

Use `LeasedScheduler` when you want to run a scheduled job but must guarantee that only one node executes it, even if
your application is running on multiple machines.

Typical use cases:

- Generate period reports
- Cleanup jobs
- Reconciliation jobs
- Scheduled maintenance
- Periodic aggregation

---

### HelloWorld (Cron)

📄 **HelloWorldCronSchedulerExample.java**

```java
Schedule everyMinute = new Cron("* * * * *");

scheduler.schedule("hello-world-job", everyMinute, () ->System.out.println("Hello world"));
```
Even if this application runs on multiple nodes, the job executes on only one node per minute.

---

### HelloWorld (FixedRate)

📄 **HelloWorldFixedRateSchedulerExample.java**

```java
Schedule everyFiveSeconds = FixedRate.every(Duration.ofSeconds(5));

scheduler.schedule("hello-world-fixed-rate", everyFiveSeconds, () ->System.out.println("Hello world"));
```
Use `FixedRate` when you care about frequency rather than wall-clock time.

---

### Cron vs FixedRate

| Schedule type | Use when                      |
|---------------|-------------------------------|
| Cron          | specific wall-clock times     |
| FixedRate     | run every N seconds/minutes   |
| Cron          | business schedules            |
| FixedRate     | heartbeats, polling           |

---

## WorkRequestProcessor example
### What is WorkRequestProcessor?
Use `WorkRequestProcessor` when you want to process background work in a distributed and fault-tolerant way.

### Features:
- multiple workers across nodes
- leasing to prevent duplicate execution
- automatic retries with backoff
- safe handling of long-running tasks

### Typical use cases:
- email / SMS sending
- async workflows
- background processing
- event handling

### Notification example
📄 **NotificationExample.java**
This example demonstrates:
- submitting work requests
- processing them across nodes
- handling multiple payload types under one workType

#### Submitting work
```java
WorkClient client = WorkClients.create(store, codec);

client.submit("notification", new SendEmail("user@example.com", "Welcome", "Hello from Anthills!"));
client.submit("notification", new SendSms("+441234567890", "Hello from Anthills!"));
```

#### Registering handlers
```java
processor.registerHandler("notification", SendEmail.class, req -> sendEmail(req.payload()));

processor.registerHandler("notification", SendSms.class, req -> sendSms(req.payload()));
```
Multiple handlers can exist for the same `workType`, the processor automatically chooses the correct handler by decoding the payload.

---

### What happens under the hood?

- Work is stored in the database
- A worker claims work using a lease
- The lease is renewed while the handler runs
- On failure, the work is retried with backoff
- If a node crashes, another node picks up the work
- All of this happens automatically.
---

### Running the examples

1. Start your database
2. Update JDBC settings in Common.java
3. Run any example main() method
4. (Optional) Run the same example on multiple machines or JVMs to see distributed behavior
---
