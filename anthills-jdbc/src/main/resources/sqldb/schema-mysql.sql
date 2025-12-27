-- MySQL schema equivalent to schema-default.sql

-- WorkRequest Table
CREATE TABLE work_request
(
    id              VARCHAR(36) PRIMARY KEY,
    work_type       VARCHAR(100) NOT NULL,

    payload         BLOB         NOT NULL,
    payload_type    VARCHAR(500) NOT NULL,
    payload_version INT          NOT NULL,
    codec           VARCHAR(50)  NOT NULL,

    status          VARCHAR(20)  NOT NULL,
    attempt_count   INT          NOT NULL DEFAULT 0,
    max_retries     INT,

    owner_id        VARCHAR(100),
    lease_until     DATETIME,

    failure_reason  TEXT,

    created_ts      DATETIME     NOT NULL,
    updated_ts      DATETIME     NOT NULL,
    started_ts      DATETIME,
    completed_ts    DATETIME
);

CREATE INDEX idx_wr_claim ON work_request (work_type, status, lease_until);

-- Scheduler Lease Table
CREATE TABLE scheduler_lease
(
    job_name    VARCHAR(100) PRIMARY KEY,
    owner_id    VARCHAR(100) NOT NULL,
    lease_until DATETIME     NOT NULL
);
