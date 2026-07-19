ALTER TABLE news_ingestion_source
    ADD COLUMN schedule_enabled TINYINT NOT NULL DEFAULT 0 AFTER enabled,
    ADD COLUMN schedule_interval_minutes INT NOT NULL DEFAULT 60 AFTER schedule_enabled,
    ADD COLUMN next_run_at DATETIME NULL AFTER schedule_interval_minutes;

ALTER TABLE news_ingestion_job
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 1 AFTER job_status,
    ADD COLUMN max_attempts INT NOT NULL DEFAULT 1 AFTER attempt_count,
    ADD COLUMN next_retry_at DATETIME NULL AFTER max_attempts,
    ADD COLUMN cancel_requested TINYINT NOT NULL DEFAULT 0 AFTER next_retry_at;

CREATE INDEX idx_ingestion_source_schedule ON news_ingestion_source (schedule_enabled, next_run_at);
CREATE INDEX idx_ingestion_job_retry ON news_ingestion_job (job_status, next_retry_at);
