-- WorkRequest Table
CREATE TABLE IF NOT EXISTS work_request
(
    id
    VARCHAR
(
    100
) PRIMARY KEY,
    payload_class VARCHAR
(
    1000
) NOT NULL,
    payload VARCHAR
(
    65535
) NOT NULL,
    status VARCHAR
(
    20
) NOT NULL,
    details VARCHAR
(
    65535
),
    max_retries INTEGER NOT NULL DEFAULT 0,
    owner VARCHAR
(
    100
),
    lease_until TIMESTAMP,
    created_ts TIMESTAMP NOT NULL,
    updated_ts TIMESTAMP,
    started_ts TIMESTAMP,
    completed_ts TIMESTAMP
    );

-- Index on status to support queries like findAllNonTerminal
CREATE INDEX IF NOT EXISTS idx_work_request_payload_class_status ON work_request (payload_class, status);

// TODO refactored one
CREATE INDEX idx_work_polling
    ON work_request (work_type, status, lease_until);

CREATE INDEX idx_work_owner
    ON work_request (owner_id);


CREATE TABLE scheduled_job
(
    job_id                VARCHAR PRIMARY KEY,
    job_name              VARCHAR   NOT NULL UNIQUE,

    schedule_type         VARCHAR   NOT NULL, -- CRON | FIXED_RATE
    schedule_expression   VARCHAR   NOT NULL, -- cron string or ISO-8601 duration

    status                VARCHAR   NOT NULL, -- ACTIVE | INACTIVE | DELETED

    next_execution_ts     TIMESTAMP,
    last_execution_ts     TIMESTAMP,
    last_execution_status VARCHAR,            -- SUCCEEDED | FAILED | CANCELLED
    last_failure_error    TEXT,

    owner_id              VARCHAR,            -- lease holder
    lease_until           TIMESTAMP,

    created_ts            TIMESTAMP NOT NULL,
    updated_ts            TIMESTAMP NOT NULL
);

CREATE INDEX idx_scheduled_job_next_exec
    ON scheduled_job (status, next_execution_ts);

CREATE INDEX idx_scheduled_job_owner
    ON scheduled_job (owner_id);


-- Lease Table
CREATE TABLE IF NOT EXISTS lease
(
    object
    VARCHAR
(
    200
) PRIMARY KEY,
    owner VARCHAR
(
    100
) NOT NULL,
    expires_at TIMESTAMP NOT NULL
    );

-- Add an index to speed up queries filtering by owner (if applicable)
CREATE INDEX IF NOT EXISTS idx_lease_owner ON lease (owner);
