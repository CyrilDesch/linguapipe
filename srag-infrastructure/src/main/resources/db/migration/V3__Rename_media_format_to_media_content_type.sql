-- Rename media_format column to media_content_type in ingestion_jobs table
ALTER TABLE ingestion_jobs RENAME COLUMN media_format TO media_content_type;

