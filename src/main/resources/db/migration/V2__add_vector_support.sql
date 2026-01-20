-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Add embedding column to synced_emails table
-- Using vector(768) for Google's text-embedding-004 model which generates 768-dimensional embeddings
ALTER TABLE synced_emails 
ADD COLUMN IF NOT EXISTS embedding vector(768);

-- Create index for faster similarity search using cosine distance
CREATE INDEX IF NOT EXISTS synced_emails_embedding_idx 
ON synced_emails 
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Add a column to track when embedding was generated
ALTER TABLE synced_emails
ADD COLUMN IF NOT EXISTS embedding_generated_at TIMESTAMP;
