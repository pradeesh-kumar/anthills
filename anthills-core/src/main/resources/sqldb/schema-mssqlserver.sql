CREATE TABLE work_request
(
    id           NVARCHAR(100) PRIMARY KEY,
    payload      NVARCHAR(MAX),
    status       NVARCHAR(20) NOT NULL,
    details      NVARCHAR(MAX),
    created_ts   DATETIME2 DEFAULT SYSUTCDATETIME(),
    updated_ts   DATETIME2,
    started_ts   DATETIME2,
    completed_ts DATETIME2
);

CREATE INDEX idx_work_request_status ON work_request (status);
CREATE INDEX idx_work_request_updated_ts ON work_request (updated_ts);

CREATE TABLE lease
(
    object     NVARCHAR(100) PRIMARY KEY,
    owner      NVARCHAR(100) NOT NULL,
    expires_at DATETIME2 NOT NULL
);

CREATE INDEX idx_lease_expires_at ON lease (expires_at);
