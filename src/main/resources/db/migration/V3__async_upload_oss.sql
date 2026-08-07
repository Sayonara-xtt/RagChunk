-- 异步批量上传、OSS 归档、流程阶段与可重复训练

CREATE TABLE upload_batch (
    id              VARCHAR(32) PRIMARY KEY,
    kb_id           VARCHAR(32) NOT NULL REFERENCES knowledge_base (id) ON DELETE CASCADE,
    source_type     VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    total_count     INT NOT NULL DEFAULT 0,
    queued_count    INT NOT NULL DEFAULT 0,
    processing_count INT NOT NULL DEFAULT 0,
    success_count   INT NOT NULL DEFAULT 0,
    failed_count    INT NOT NULL DEFAULT 0,
    smart_chunk     BOOLEAN NOT NULL DEFAULT FALSE,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_upload_batch_kb ON upload_batch (kb_id, created_at DESC);

ALTER TABLE document
    ADD COLUMN IF NOT EXISTS batch_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS process_stage VARCHAR(32),
    ADD COLUMN IF NOT EXISTS progress_percent INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS file_size BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS storage_url VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS retrain_version INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_document_batch ON document (batch_id);
CREATE INDEX IF NOT EXISTS idx_document_stage ON document (kb_id, process_stage);
