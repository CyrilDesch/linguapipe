CREATE TABLE IF NOT EXISTS ingestion_jobs (
    id UUID PRIMARY KEY,
    transcript_id UUID,
    source VARCHAR(32) NOT NULL,
    media_format VARCHAR(64),
    media_filename TEXT,
    status VARCHAR(32) NOT NULL,
    attempt INT NOT NULL,
    max_attempts INT NOT NULL,
    error_message TEXT,
    blob_key TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_status ON ingestion_jobs (status);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_transcript ON ingestion_jobs (transcript_id);
