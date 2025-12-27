-- Microsoft SQL Server schema equivalent to schema-default.sql

-- WorkRequest Table
CREATE TABLE work_request
(
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    work_type       NVARCHAR(100)   NOT NULL,

    payload         VARBINARY(MAX)  NOT NULL,
    payload_type    NVARCHAR(500)   NOT NULL,
    payload_version INT             NOT NULL,
    codec           NVARCHAR(50)    NOT NULL,

    status          NVARCHAR(20)    NOT NULL,
    attempt_count   INT             NOT NULL DEFAULT 0,
    max_retries     INT             NULL,

    owner_id        NVARCHAR(100)   NULL,
    lease_until     DATETIME2       NULL,

    failure_reason  NVARCHAR(MAX)   NULL,

    created_ts      DATETIME2       NOT NULL,
    updated_ts      DATETIME2       NOT NULL,
    started_ts      DATETIME2       NULL,
    completed_ts    DATETIME2       NULL
);

CREATE INDEX idx_wr_claim ON work_request (work_type, status, lease_until);

-- Scheduler Lease Table
CREATE TABLE scheduler_lease
(
    job_name    NVARCHAR(100)  NOT NULL PRIMARY KEY,
    owner_id    NVARCHAR(100)  NOT NULL,
    lease_until DATETIME2      NOT NULL
);
