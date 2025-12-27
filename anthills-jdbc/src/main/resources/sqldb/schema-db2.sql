-- IBM DB2 schema equivalent to schema-default.sql

-- WorkRequest Table
CREATE TABLE work_request
(
    id              VARCHAR(36)   PRIMARY KEY,
    work_type       VARCHAR(100)  NOT NULL,

    payload         BLOB          NOT NULL,
    payload_type    VARCHAR(500)  NOT NULL,
    payload_version INTEGER       NOT NULL,
    codec           VARCHAR(50)   NOT NULL,

    status          VARCHAR(20)   NOT NULL,
    attempt_count   INTEGER       NOT NULL DEFAULT 0,
    max_retries     INTEGER,

    owner_id        VARCHAR(100),
    lease_until     TIMESTAMP,

    failure_reason  CLOB,

    created_ts      TIMESTAMP     NOT NULL,
    updated_ts      TIMESTAMP     NOT NULL,
    started_ts      TIMESTAMP,
    completed_ts    TIMESTAMP
);

CREATE INDEX idx_wr_claim ON work_request (work_type, status, lease_until);

-- Scheduler Lease Table
CREATE TABLE scheduler_lease
(
    job_name    VARCHAR(100) PRIMARY KEY,
    owner_id    VARCHAR(100) NOT NULL,
    lease_until TIMESTAMP    NOT NULL
);
