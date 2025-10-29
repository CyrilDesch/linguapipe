-- Create transcripts table based on domain model
CREATE TABLE transcripts (
    id UUID PRIMARY KEY,
    language VARCHAR(2) CHECK (language IS NULL OR language ~ '^[a-z]{2}$'),
    text TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL CHECK (confidence >= 0.0 AND confidence <= 1.0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(20) NOT NULL CHECK (source IN ('Audio', 'Text', 'Document')),
    attributes JSONB DEFAULT '{}'::jsonb
);

-- Create indexes for performance
CREATE INDEX idx_transcripts_created_at ON transcripts(created_at);
CREATE INDEX idx_transcripts_attributes ON transcripts USING GIN(attributes);

-- Add column comments for documentation
COMMENT ON COLUMN transcripts.language IS 'ISO 639-1 language code (2-letter) or NULL if language cannot be determined';
COMMENT ON COLUMN transcripts.confidence IS 'Transcription confidence score (0.0 to 1.0)';
COMMENT ON COLUMN transcripts.source IS 'Source type: Audio, Text, or Document';
COMMENT ON COLUMN transcripts.attributes IS 'Additional metadata as JSON';
