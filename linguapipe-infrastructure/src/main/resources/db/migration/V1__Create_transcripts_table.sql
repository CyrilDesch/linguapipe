-- Create transcripts table based on domain model
CREATE TABLE transcripts (
    id UUID PRIMARY KEY,
    language VARCHAR(10) NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(20) NOT NULL CHECK (source IN ('Audio', 'Text', 'Document')),
    attributes JSONB DEFAULT '{}'::jsonb
);

-- Create indexes for performance
CREATE INDEX idx_transcripts_language ON transcripts(language);
CREATE INDEX idx_transcripts_created_at ON transcripts(created_at);
CREATE INDEX idx_transcripts_source ON transcripts(source);
CREATE INDEX idx_transcripts_attributes ON transcripts USING GIN(attributes);
