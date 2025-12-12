CREATE TABLE work_request
(
    id           NVARCHAR(100) PRIMARY KEY,
    payload_class NVARCHAR(1000) NOT NULL,
    payload      NVARCHAR(MAX),
    status       NVARCHAR(20) NOT NULL,
    details      NVARCHAR(MAX),
    max_retries  INTEGER NOT NULL DEFAULT 0,
    owner        NVARCHAR(100),
    lease_until  DATETIME2,
    created_ts   DATETIME2 DEFAULT SYSUTCDATETIME(),
    updated_ts   DATETIME2,
    started_ts   DATETIME2,
    completed_ts DATETIME2
);

CREATE INDEX idx_work_request_payload_class_status ON work_request (payload_class, status);
CREATE INDEX idx_work_request_updated_ts ON work_request (updated_ts);

CREATE TABLE lease
(
    object     NVARCHAR(100) PRIMARY KEY,
    owner      NVARCHAR(100) NOT NULL,
    expires_at DATETIME2 NOT NULL
);

CREATE INDEX idx_lease_expires_at ON lease (expires_at);
