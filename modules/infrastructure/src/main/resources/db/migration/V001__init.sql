-- LinguaPipe Initial Schema
-- This migration creates the core tables for transcript and segment storage

-- Transcripts table
CREATE TABLE IF NOT EXISTS transcripts (
    id VARCHAR(255) PRIMARY KEY,
    language VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    source_type VARCHAR(100) NOT NULL,
    metadata_json JSONB NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transcripts_created_at ON transcripts(created_at);
CREATE INDEX idx_transcripts_source_type ON transcripts(source_type);

-- Segments table
CREATE TABLE IF NOT EXISTS segments (
    id VARCHAR(255) PRIMARY KEY,
    transcript_id VARCHAR(255) NOT NULL,
    text TEXT NOT NULL,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    speaker VARCHAR(255),
    confidence DOUBLE PRECISION,
    tags_json JSONB,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transcript FOREIGN KEY (transcript_id) REFERENCES transcripts(id) ON DELETE CASCADE
);

CREATE INDEX idx_segments_transcript_id ON segments(transcript_id);
CREATE INDEX idx_segments_start_at ON segments(start_at);
CREATE INDEX idx_segments_speaker ON segments(speaker);

-- Ingestion jobs table (for tracking async ingestion status)
CREATE TABLE IF NOT EXISTS ingestion_jobs (
    id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    source_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    error_message TEXT,
    metadata_json JSONB,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_ingestion_jobs_status ON ingestion_jobs(status);
CREATE INDEX idx_ingestion_jobs_created_at ON ingestion_jobs(created_at);
