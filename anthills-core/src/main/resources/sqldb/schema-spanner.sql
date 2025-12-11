-- === work_request ===
CREATE TABLE work_request
(
    id           STRING(100) NOT NULL,
    payload      STRING(MAX) NOT NULL,
    status       STRING(20) NOT NULL,
    details      STRING(MAX),
    created_ts   TIMESTAMP NOT NULL,
    updated_ts   TIMESTAMP,
    started_ts   TIMESTAMP,
    completed_ts TIMESTAMP
) PRIMARY KEY (id);

CREATE INDEX idx_work_request_status ON work_request (status);


-- === lease ===
CREATE TABLE lease
(
    object     STRING(200) NOT NULL,
    owner      STRING(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL
) PRIMARY KEY (object);

CREATE INDEX idx_lease_owner ON lease (owner);
