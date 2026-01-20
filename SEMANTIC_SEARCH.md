# Email Semantic Search with pgvector

This implementation adds semantic search capabilities to the email dashboard using PostgreSQL's pgvector extension and Google's text embedding API.

## Overview

The system generates 768-dimensional vector embeddings for emails and stores them in PostgreSQL. This enables semantic search where users can find emails based on meaning rather than exact keyword matches.

## Architecture

### Components

1. **pgvector Extension** - PostgreSQL extension for vector similarity search
2. **Google Generative AI** - text-embedding-004 model for generating embeddings
3. **Vector Storage** - New `embedding` column in `synced_emails` table
4. **Semantic Search API** - REST endpoint for vector-based email search

### Database Schema

```sql
-- synced_emails table additions
embedding vector(768)              -- 768-dimensional embedding vector
embedding_generated_at TIMESTAMP   -- When the embedding was created
```

### Vector Index

An IVFFlat index is created for fast similarity searches using cosine distance:

```sql
CREATE INDEX synced_emails_embedding_idx
ON synced_emails
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

## Configuration

### Environment Variables

Add to your `.env` or `application-local.yml`:

```yaml
google:
  ai:
    api-key: your-google-ai-api-key
    embedding-model: text-embedding-004
```

### Getting Google AI API Key

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Create a new API key
3. Add it to your environment configuration

## API Endpoints

### 1. Semantic Search

**Endpoint:** `POST /api/gmail/semantic-search`

**Request Body:**

```json
{
  "query": "meetings about Q1 budget planning",
  "limit": 20
}
```

**Response:**

```json
{
  "success": true,
  "message": "Semantic search results fetched successfully",
  "data": [
    {
      "messageId": "abc123",
      "subject": "Q1 Budget Meeting",
      "from": "finance@company.com",
      "snippet": "Let's discuss the Q1 budget...",
      "receivedDate": "2024-01-15T10:30:00",
      "source": "SEMANTIC"
    }
  ]
}
```

### 2. Generate Missing Embeddings

**Endpoint:** `POST /api/gmail/embeddings/generate`

Generates embeddings for all emails that don't have them yet.

**Response:**

```json
{
  "success": true,
  "message": "Generated embeddings for 145 emails",
  "data": {
    "processed": 145
  }
}
```

## How It Works

### 1. Embedding Generation

When emails are synced from Gmail:

1. Email content (subject + from + body) is combined
2. Text is sent to Google's embedding API
3. Returns a 768-dimensional vector
4. Vector is stored in the `embedding` column

### 2. Semantic Search Process

1. User submits a search query
2. Query is converted to an embedding vector
3. PostgreSQL finds emails with similar vectors using cosine distance
4. Results are ordered by similarity (closest vectors first)
5. Top N results are returned

### 3. Vector Similarity

The search uses cosine distance operator `<=>`:

- Distance 0 = identical vectors (perfect match)
- Distance 2 = opposite vectors (completely different)
- Smaller distance = more similar content

## Performance Considerations

### Automatic Embedding Generation

- **New emails**: Embeddings generated automatically during sync
- **Existing emails**: Run `/embeddings/generate` to backfill
- **Rate limiting**: Google AI API has rate limits, generation is sequential

### Search Performance

- IVFFlat index provides fast approximate nearest neighbor search
- Search time: O(sqrt(n)) vs O(n) for sequential scan
- Trade-off: slight accuracy loss for significant speed gain

### Cost Considerations

- Google AI API charges per request
- text-embedding-004 pricing: Check [Google AI pricing](https://ai.google.dev/pricing)
- Embeddings are cached in DB, only generated once per email

## Migration

### Running the Migration

1. Ensure PostgreSQL has pgvector extension installed:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

2. Run the migration script:

```bash
# The migration runs automatically with Hibernate ddl-auto: update
# Or manually run: backend/src/main/resources/db/migration/V2__add_vector_support.sql
```

### Backfilling Embeddings

For existing emails without embeddings:

```bash
# Via API
curl -X POST http://localhost:3000/api/gmail/embeddings/generate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Or wait for automatic generation during normal sync
```

## Testing

### Example Semantic Queries

```json
// Find emails about meetings
{"query": "schedule a meeting with the team", "limit": 10}

// Find invoices or billing
{"query": "invoice payment receipt", "limit": 10}

// Find project updates
{"query": "project status update Q4", "limit": 10}

// Find job applications
{"query": "job application resume interview", "limit": 10}
```

### Comparing Search Types

- **Keyword Search**: Exact text matching (fast, precise)
- **Fuzzy Search**: Text similarity (typo-tolerant)
- **Semantic Search**: Meaning-based (understands context)

Example:

- Query: "invoice"
- Keyword: Finds emails containing "invoice"
- Semantic: Finds "invoice", "bill", "payment receipt", "statement"

## Troubleshooting

### No results from semantic search

1. Check if embeddings exist:

```sql
SELECT COUNT(*) FROM synced_emails WHERE embedding IS NOT NULL;
```

2. Generate embeddings if missing:

```bash
POST /api/gmail/embeddings/generate
```

### Slow search performance

1. Verify index exists:

```sql
SELECT * FROM pg_indexes WHERE tablename = 'synced_emails';
```

2. Increase IVFFlat lists for larger datasets:

```sql
DROP INDEX synced_emails_embedding_idx;
CREATE INDEX synced_emails_embedding_idx
ON synced_emails
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 200);  -- Increase from 100
```

### Google AI API errors

- Check API key is valid
- Verify API quota not exceeded
- Check network connectivity
- Review logs for specific error messages

## Future Enhancements

1. **Batch Embedding Generation**: Process multiple emails in parallel
2. **Hybrid Search**: Combine keyword + semantic search
3. **Fine-tuned Models**: Train custom embeddings on email data
4. **Multi-language Support**: Handle emails in different languages
5. **Search Analytics**: Track which semantic queries work best
6. **Auto-categorization**: Use embeddings to auto-tag emails

## References

- [pgvector Documentation](https://github.com/pgvector/pgvector)
- [Google Generative AI](https://ai.google.dev/)
- [Vector Similarity Search](https://www.postgresql.org/docs/current/functions-vector.html)
