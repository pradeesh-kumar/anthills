-- SQLite schema equivalent to schema-default.sql
-- Note: SQLite stores timestamps as TEXT (ISO-8601) or INTEGER (epoch). We use TEXT here.

-- WorkRequest Table
CREATE TABLE IF NOT EXISTS work_request
(
    id              TEXT PRIMARY KEY,
    work_type       TEXT        NOT NULL,

    payload         BLOB        NOT NULL,
    payload_type    TEXT        NOT NULL,
    payload_version INTEGER     NOT NULL,
    codec           TEXT        NOT NULL,

    status          TEXT        NOT NULL,
    attempt_count   INTEGER     NOT NULL DEFAULT 0,
    max_retries     INTEGER,

    owner_id        TEXT,
    lease_until     TEXT,              -- ISO-8601 timestamp string

    failure_reason  TEXT,

    created_ts      TEXT        NOT NULL,
    updated_ts      TEXT        NOT NULL,
    started_ts      TEXT,
    completed_ts    TEXT
);

CREATE INDEX IF NOT EXISTS idx_wr_claim ON work_request (work_type, status, lease_until);

-- Scheduler Lease Table
CREATE TABLE IF NOT EXISTS scheduler_lease
(
    job_name    TEXT PRIMARY KEY,
    owner_id    TEXT NOT NULL,
    lease_until TEXT NOT NULL
);
