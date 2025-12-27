# Anthills JDBC
`anthills-jdbc` provides a JDBC-based implementation of `WorkStore` which is Anthills persistence and leasing layer by using a relational database as the coordination mechanism.

---

## What is this module?

Anthills core logic is database-agnostic.
The `anthills-jdbc` module is the reference implementation that uses:
- JDBC
- a relational database
- row-level leasing semantics

This module implements
- persisting work requests
- claiming work using leases
- renewing and releasing leases
- persisting scheduler leases

---

## Supported databases
The JDBC implementation is designed to work with most relational databases.

- PostgreSQL
- MySQL
- Oracle
- DB2
- H2
- Sqlite
- MSSQL

---

## Creating a JdbcWorkStore
Using an existing `DataSource`
```java
DataSource dataSource = ...;
WorkStore store = JdbcWorkStore.create(dataSource);
```

This will:
- auto-detect the database
- initializes schema

---

## Using JDBC settings (HikariCP)
```java
JdbcSettings settings = new JdbcSettings(
        "jdbc:postgresql://localhost:5432/anthills",
        "anthills",
        "anthills", 10, 2, 30000);

WorkStore store = JdbcWorkStore.create(settings);
```
Connection pooling is handled internally using HikariCP.

---

## Schema management
On startup, `JdbcWorkStore` automatically:
- creates required tables and indices if they don’t exist
- applies database-specific SQL where necessary
No manual migration is required for initial usage.
Schema evolution is handled conservatively to avoid data loss.

---

## Transaction & locking model
Anthills JDBC relies on:
- `SELECT … FOR UPDATE`
- lease expiry timestamps
- optimistic ownership checks
This ensures:
- no duplicate work execution
- safe crash recovery
- predictable behavior under contention

---

## How JDBC integrates with Anthills components
With `LeasedScheduler`
```java
WorkStore store = JdbcWorkStore.create(dataSource);
LeasedScheduler scheduler = Schedulers.create(store, SchedulerConfig.defaults());
```

Scheduler leases are stored and renewed in the database.

---

With `WorkRequestProcessor`
```java
WorkStore store = JdbcWorkStore.create(dataSource);
WorkRequestProcessor processor = WorkRequestProcessors.create("notification", store, JsonPayloadCodec.defaultInstance(), ProcessorConfig.defaults());
```

--
## Operational notes
- Lease durations and renew intervals are configurable
- Polling frequency can be tuned
- Database load is minimized via adaptive polling
- All operations are idempotent where possible

