-- WorkRequest Table
CREATE TABLE IF NOT EXISTS work_request
(
    id           VARCHAR(100) PRIMARY KEY,
    payload      VARCHAR(65535) NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    details      VARCHAR(65535)
    created_ts   TIMESTAMP NOT NULL,
    updated_ts   TIMESTAMP,
    started_ts   TIMESTAMP,
    completed_ts TIMESTAMP
);

-- Index on status to support queries like findAllNonTerminal
CREATE INDEX IF NOT EXISTS idx_work_request_status ON work_request (status);

-- Lease Table
CREATE TABLE IF NOT EXISTS lease
(
    object     VARCHAR(200) PRIMARY KEY,
    owner      VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

-- Add an index to speed up queries filtering by owner (if applicable)
CREATE INDEX IF NOT EXISTS idx_lease_owner ON lease (owner);
