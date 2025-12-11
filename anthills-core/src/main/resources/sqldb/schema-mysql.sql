CREATE TABLE IF NOT EXISTS work_request
(
    id           VARCHAR(100) PRIMARY KEY,
    payload      TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL,
    details      TEXT,
    created_ts   TIMESTAMP   NOT NULL,
    updated_ts   TIMESTAMP NULL,
    started_ts   TIMESTAMP NULL,
    completed_ts TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_work_request_status ON work_request (status);

CREATE TABLE IF NOT EXISTS lease
(
    object     VARCHAR(200) PRIMARY KEY,
    owner      VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_lease_owner ON lease (owner);
