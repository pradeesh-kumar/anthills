CREATE TABLE IF NOT EXISTS work_request
(
    id           TEXT PRIMARY KEY,
    payload_class TEXT NOT NULL
    payload      TEXT NOT NULL,
    status       TEXT NOT NULL,
    details      TEXT,
    max_retries  INTEGER NOT NULL DEFAULT 0,
    owner        TEXT,
    lease_until  TEXT,
    created_ts   TEXT NOT NULL, -- SQLite stores timestamps as TEXT (ISO-8601), REAL, or INTEGER
    updated_ts   TEXT,
    started_ts   TEXT,
    completed_ts TEXT
);

CREATE INDEX IF NOT EXISTS idx_work_request_payload_class_status ON work_request (payload_class, status);

-- === lease ===
CREATE TABLE IF NOT EXISTS lease
(
    object     TEXT PRIMARY KEY,
    owner      TEXT NOT NULL,
    expires_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_lease_owner ON lease (owner);
