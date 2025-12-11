CREATE TABLE work_request
(
    id           VARCHAR2(100) PRIMARY KEY,
    payload      CLOB,
    status       VARCHAR2(20) NOT NULL,
    details      CLOB,
    created_ts   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_ts   TIMESTAMP,
    started_ts   TIMESTAMP,
    completed_ts TIMESTAMP
);

CREATE INDEX idx_work_request_status ON work_request (status);
CREATE INDEX idx_work_request_updated_ts ON work_request (updated_ts);

CREATE TABLE lease
(
    object     VARCHAR2(100) PRIMARY KEY,
    owner      VARCHAR2(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_lease_expires_at ON lease (expires_at);
