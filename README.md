# Anthills
![logo.png](logo.png)

Anthills is a Distributed Work Request Engine written in Java 🧠⚙️

A **database-backed, lease-based distributed worker framework** for reliably executing work across multiple nodes **without duplicates**, **without centralized coordination**, and **without external queues**.

This library is designed for engineers who like **explicit control**, **strong correctness guarantees**, and **clear data models** over opaque magic.

---

## ✨ Key Features
- ✅ **Distributed-safe execution**
- 🔐 **Lease-based work claiming**
- 🔁 **Automatic retries with limits**
- 🧾 **Explicit work lifecycle & state transitions**
- 🧠 **DB as the source of truth**
- 🚫 **No duplicate execution**
- 🪶 **No heavyweight dependencies (no Kafka / no ORM required)**

## 🧩 Core Concepts

### WorkRequest

A `WorkRequest` represents a single unit of work stored in the database.

```java
public record WorkRequest<T>(
  String id,
  String payloadClass,
  T payload,
  Status status,
  String details,
  int maxRetries,
  String owner,
  Instant leaseUntil,
  Instant createdTs,
  Instant updatedTs,
  Instant startedTs,
  Instant completedTs
)
```

Each work request is:
- Immutable
- Strongly validated
- State-driven

## WorkRequest Status Lifecycle
```
New
↓
InProgress
↓
Succeeded | Failed | Cancelled
```
### Additional states:
- Paused
- Cancelled
- ✔ Terminal states are immutable
- ✔ Non-terminal states are lease-claimable

# 🔐 Lease-Based Execution Model
Instead of locks, this system uses time-bound leases:
- Workers atomically claim work by updating:
  - owner
  - leaseUntil
- Lease expiration allows safe recovery if a worker crashes
- No in-memory coordination required
This model scales naturally across:
- Multiple JVMs
- Multiple hosts
- Multiple regions (with a shared DB)

🏗 Architecture Overview
```
+------------------+
|  Worker Node A   |
+------------------+
         |
         | Atomic DB Claim
         ↓
+------------------+
|   Database       |
|  (Work Requests) |
+------------------+
         ↑
         | Competing Workers
+------------------+
|  Worker Node B   |
+------------------+
```

# The database is the coordinator.
## 🧠 Design Principles
- DB-first consistency
- Explicit state transitions
- No background magic
- Fail-safe retries
- Predictable recovery

## This design borrows ideas from:
- Distributed locks
- Task leasing
- Workflow engines
- Queue semantics (without queues)

# 🧰 Modules
## WorkRequestService
High-level API for managing work requests.
```java
WorkRequest<?> create(WorkRequest<?> wr);
WorkRequest<?> update(WorkRequest<?> wr);
void markSucceeded(WorkRequest<?> wr);
void markFailedOrRetry(WorkRequest<?> wr);
Optional<WorkRequest<?>> findById(String id, Class<?> payloadClass);
```
Encodes business rules, validation, and lifecycle safety.

##❓ Why Not Kafka / SQS / Quartz?
- Because sometimes you want:
- Strong DB transactions
- Simple deployments
- Transparent behavior
- Debuggable state
- Full control over execution

## This project is ideal for:
- Internal job orchestration
- Background workers
- Stateful workflows
- Control-plane style systems

# 📌 Future Roadmap
- Metrics & observability hooks
- UI for visibility and Workflow management
- REST Api

---
# 🤝 Contributions
PRs and design discussions are welcome — especially around:
- Distributed correctness
- Failure recovery
- Performance optimizations
- Test cases

---
Built with ❤️ for engineers who enjoy distributed systems done right.
