-- Oracle schema equivalent to schema-default.sql

-- WorkRequest Table
CREATE TABLE work_request
(
    id              VARCHAR2(36)    PRIMARY KEY,
    work_type       VARCHAR2(100)   NOT NULL,

    payload         BLOB            NOT NULL,
    payload_type    VARCHAR2(500)   NOT NULL,
    payload_version NUMBER(10)      NOT NULL,
    codec           VARCHAR2(50)    NOT NULL,

    status          VARCHAR2(20)    NOT NULL,
    attempt_count   NUMBER(10)      DEFAULT 0 NOT NULL,
    max_retries     NUMBER(10),

    owner_id        VARCHAR2(100),
    lease_until     TIMESTAMP,

    failure_reason  CLOB,

    created_ts      TIMESTAMP       NOT NULL,
    updated_ts      TIMESTAMP       NOT NULL,
    started_ts      TIMESTAMP,
    completed_ts    TIMESTAMP
);

CREATE INDEX idx_wr_claim ON work_request (work_type, status, lease_until);

-- Scheduler Lease Table
CREATE TABLE scheduler_lease
(
    job_name    VARCHAR2(100)  PRIMARY KEY,
    owner_id    VARCHAR2(100)  NOT NULL,
    lease_until TIMESTAMP      NOT NULL
);
