-- === work_request ===
CREATE TABLE work_request
(
    id           STRING(100) NOT NULL,
    payload_class STRING(1000) NOT NULL,
    payload      STRING(MAX) NOT NULL,
    status       STRING(20) NOT NULL,
    details      STRING(MAX),
    max_retries  INTEGER NOT NULL DEFAULT 0,
    owner        VARCHAR(100),
    lease_until  TIMESTAMP,
    created_ts   TIMESTAMP NOT NULL,
    updated_ts   TIMESTAMP,
    started_ts   TIMESTAMP,
    completed_ts TIMESTAMP
) PRIMARY KEY (id);

CREATE INDEX idx_work_request_payload_class_status ON work_request (payload_class, status);


-- === lease ===
CREATE TABLE lease
(
    object     STRING(200) NOT NULL,
    owner      STRING(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL
) PRIMARY KEY (object);

CREATE INDEX idx_lease_owner ON lease (owner);
