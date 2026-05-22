-- 本机 PostgreSQL（无需 pgvector），向量存为 real[]

CREATE TABLE knowledge_base (
    id                  VARCHAR(32) PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    status              VARCHAR(32) NOT NULL,
    config_json         JSONB NOT NULL,
    embedding_model     VARCHAR(128) NOT NULL,
    embedding_dimensions INT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);

CREATE TABLE document (
    id              VARCHAR(32) PRIMARY KEY,
    kb_id           VARCHAR(32) NOT NULL REFERENCES knowledge_base (id) ON DELETE CASCADE,
    file_name       VARCHAR(512) NOT NULL,
    storage_key     VARCHAR(1024),
    status          VARCHAR(32) NOT NULL,
    chunk_count     INT NOT NULL DEFAULT 0,
    profile         VARCHAR(64),
    quality_score   INT NOT NULL DEFAULT 0,
    ai_triggered    BOOLEAN NOT NULL DEFAULT FALSE,
    ai_trigger_id   VARCHAR(16),
    ai_fallback     BOOLEAN NOT NULL DEFAULT FALSE,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_document_kb_id ON document (kb_id);

CREATE TABLE chunk (
    id              VARCHAR(64) PRIMARY KEY,
    kb_id           VARCHAR(32) NOT NULL REFERENCES knowledge_base (id) ON DELETE CASCADE,
    doc_id          VARCHAR(32) NOT NULL REFERENCES document (id) ON DELETE CASCADE,
    chunk_index     INT NOT NULL,
    text_content    TEXT NOT NULL,
    source          VARCHAR(64),
    embedding       REAL[] NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_chunk_doc_index UNIQUE (doc_id, chunk_index)
);

CREATE INDEX idx_chunk_kb_id ON chunk (kb_id);
CREATE INDEX idx_chunk_doc_id ON chunk (doc_id);
