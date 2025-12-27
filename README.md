# 🐜 Anthills
![logo.png](logo.png)

**Anthills** is a **lightweight distributed database-backed Job and Worker library for Java.
It helps you run **Scheduled Jobs** and **background workers** safely across **multiple nodes**, without leader election, message queues, or extra infrastructure.

> #### Simple Idea:
>
> Use your database as the source of truth.
>
> Let nodes come and go freely.


---

## Why Anthills
Distributed coordination is hard — but most applications already have a database.
Anthills lets you build **reliable distributed systems** using primitives you already trust:

- transactions
- timestamps
- row-level locks

No Kafka.
No ZooKeeper.
No Redis locks.
No leader election.

Just **leases + workers.**

---

## ✨ Key Features
- ✅ **Distributed-safe execution**
- 🔐 **Lease-based work claiming**
- 🔁 **Automatic retries with limits**
- 🧠 **DB as the source of truth**
- 🚫 **No duplicate execution**
- 🪶 **No heavyweight dependencies (no Kafka / no ORM required)**

---

## What problems does Anthills solve?
- ✅ Run scheduled jobs on only one node
  - Even if your app runs on 10 servers.
- ✅ Process background work across many nodes
  - With retries, backoff, and crash recovery.
- ✅ Handle long-running tasks safely
  - Leases are automatically renewed while work runs.
- ✅ Survive crashes and restarts
  - Expired leases allow other nodes to take over.

---

## Core concepts
Anthills has two main primitives:

---
### 🕒 LeasedScheduler
>*“Run this job periodically, but only on one node.”*

Use this for:
- cleanup jobs
- reconciliation
- scheduled maintenance
- aggregation tasks

```java
scheduler.schedule("hello-world-job", Schedule.Cron.parse("* * * * *"), () -> System.out.println("Hello world"));
```

Deploy this on **every node** — Anthills ensures only one executes it.

---

#### 🧵 WorkRequestProcessor
>*“Process background work across many nodes.”*

Use this for:
- email / SMS sending
- async workflows
- retries and compensation logic
- event-driven processing

```java
processor.registerHandler("notification", SendEmail.class, req -> sendEmail(req.payload()));

WorkRequest<SendEmail> request = workClient.submit(new SendEmail("hello@example.com", "Hello", "Hello from Anthills!"));
```

Work is:
- persisted
- leased
- retried with backoff
- safely redistributed on failure

#### WorkRequest
A WorkRequest represents a single unit of work stored in the database.

```java
public record WorkRequest<T>(
  String id,
  String workType,
  T payload,
  int payloadVersion,
  String codec,
  Status status,
  int maxRetries,
  int attemptCount,
  String ownerId,
  Instant leaseUntil,
  String failureReason,
  Instant createdTs,
  Instant updatedTs,
  Instant startedTs,
  Instant completedTs
) {}
```

#### WorkRequest Status Lifecycle
```
New
↓
InProgress
↓
Succeeded | Failed | Cancelled
```

---

## How Anthills works (high level)
```pgsql
┌──────────┐
│ Database │  ← source of truth
└────┬─────┘
     │
┌────▼──────────┐
│ Leases        │
│ (time-bound)  │
└────┬──────────┘
     │
┌────▼──────────┐
│ Workers       │
│ (any node)    │
└───────────────┘
```

- Work is claimed with a lease
- Leases expire automatically
- Active work renews its lease
- Crashed nodes lose ownership
- Other nodes recover the work

No global state.
No coordination service.
No single point of failure.

---

## Project structure
```nginx
anthills
├── anthills-api       # Public interfaces & models
├── anthills-core      # Core execution logic
├── anthills-jdbc      # JDBC-based implementation
└── anthills-examples  # Runnable examples
```

---

## Quick start

### 1️⃣ Add dependency
```xml
<!-- Contains API and Core implementation -->
<dependency>
  <groupId>org.anthills</groupId>
  <artifactId>anthills-core</artifactId>
  <version>${anthills.version}</version>
</dependency>

<!-- JDBC Implementation of WorkStore -->
<dependency>
  <groupId>org.anthills</groupId>
  <artifactId>anthills-jdbc</artifactId>
  <version>${anthills.version}</version>
</dependency>
```

### 2️⃣ Create a store
```java
WorkStore store = JdbcWorkStore.create(dataSource);
```

### 3️⃣ Run a scheduled job (single-node execution)
```java
LeasedScheduler scheduler = Schedulers.create(SchedulerConfig.defaults(), store);

scheduler.schedule("hello-world", FixedRate.every(Duration.ofSeconds(5)), () -> System.out.println("Hello from Anthills"));
scheduler.start();
```
Run this on multiple nodes — it still executes once per interval.

### 4️⃣ Process background work (multi-node execution)
```java
WorkRequestProcessor processor = WorkRequestProcessors.create("notification", store, JsonPayloadCodec.defaultInstance(), ProcessorConfig.defaults());

processor.registerHandler("notification", SendEmail.class, req -> sendEmail(req.payload()));
processor.start();
```

Submit work from anywhere:
```java
WorkClient workClient = WorkClients.create(workStore);
workClient.submit("notification", new SendEmail("user@example.com", "Hi", "Hello!"));
```
---

## What Anthills is (and isn’t)
### ✔️ Anthills is
- a distributed coordination library
- database-backed and transaction-safe
- simple to reason about
- crash-resilient by design
- framework-agnostic

### ❌ Anthills is not
- a message queue
- a stream processor
- a workflow engine
- a replacement for Kafka
- a leader-election framework

Anthills is about execution guarantees, not messaging semantics.

---

## Why database-backed leases?
Because they are:
- already available
- strongly consistent
- observable
- debuggable

You can:
- inspect tables
- see who owns what
- manually intervene if needed
- Operational transparency is a feature.

---

## Examples
See [anthills-examples](anthills-examples) for:

- HelloWorld scheduled jobs
- Fixed-rate vs cron scheduling
- Distributed background workers
- Retry and backoff behavior

---

## Design principles

Anthills is built on a few strong principles:
- Leases over locks
- Failure is normal
- Recovery over prevention
- Simple beats clever
- Infrastructure should be boring

---
## Who should use Anthills?

Anthills is a great fit if you:
- already use a relational database
- run multiple instances of your app
- need reliable background execution
- want minimal infrastructure
- value debuggability

---

## Contributing

Contributions are welcome!
- Areas of interest:
- alternative persistence backends
- metrics & observability
- performance tuning
- documentation & examples

Please open an issue or PR to discuss.

---

License

Apache License 2.0

---
🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜🐜
