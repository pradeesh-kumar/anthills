-- WorkRequest Table
CREATE TABLE IF NOT EXISTS work_record
(
  id              VARCHAR(36) PRIMARY KEY,
  work_type       VARCHAR(100)  NOT NULL,

  payload         BLOB          NOT NULL,
  payload_type    VARCHAR(1000) NOT NULL,
  payload_version INT           NOT NULL,
  codec           VARCHAR(50)   NOT NULL,

  status          VARCHAR(20)   NOT NULL,
  attempt_count   INT           NOT NULL DEFAULT 0,
  max_retries     INT,

  owner_id        VARCHAR(100),
  lease_until     TIMESTAMP,

  failure_reason  TEXT,

  created_ts      TIMESTAMP     NOT NULL,
  updated_ts      TIMESTAMP     NOT NULL,
  started_ts      TIMESTAMP,
  completed_ts    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wr_claim ON work_request (work_type, status);


CREATE TABLE IF NOT EXISTS scheduler_lease
(
  job_name    VARCHAR(100) PRIMARY KEY,
  owner_id    VARCHAR(100) NOT NULL,
  lease_until TIMESTAMP    NOT NULL
);
