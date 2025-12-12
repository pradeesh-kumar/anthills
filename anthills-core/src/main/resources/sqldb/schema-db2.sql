-- === work_request ===
CREATE TABLE work_request
(
    id            VARCHAR(100)  NOT NULL PRIMARY KEY,
    payload_class VARCHAR(1000) NOT NULL,
    payload       CLOB          NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    details       CLOB,
    max_retries   INTEGER       NOT NULL DEFAULT 0,
    owner         VARCHAR(100),
    lease_until   TIMESTAMP,
    created_ts    TIMESTAMP     NOT NULL,
    updated_ts    TIMESTAMP,
    started_ts    TIMESTAMP,
    completed_ts  TIMESTAMP
);

CREATE INDEX idx_work_request_payload_class_status ON work_request (payload_class, status);


-- === lease ===
CREATE TABLE lease
(
    object     VARCHAR(200) NOT NULL PRIMARY KEY,
    owner      VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_lease_owner ON lease (owner);
