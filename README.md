# 📧 Email Backend Service

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](#)

A production-ready, secure email management backend service built with Spring Boot. This service provides a comprehensive email solution with hybrid authentication, Gmail API integration, and real-time email synchronization capabilities.

## 🌟 Key Features

### 🔐 **Advanced Authentication System**

- **Local Authentication**: Secure email/password registration and login with JWT tokens
- **Google OAuth 2.0**: Seamless Google Sign-In with offline access capabilities
- **Dual Token Architecture**: Separate app session tokens and Google API tokens for enhanced security
- **Automatic Token Refresh**: Background token renewal without user intervention

### 📬 **Email Management**

- **Gmail API Integration**: Full Gmail functionality through secure proxy architecture
- **Mock Data Support**: Development-friendly mock email data for testing
- **Real-time Synchronization**: Live email updates and notifications
- **Label Management**: Complete mailbox organization with folder/label support
- **Pagination**: Efficient email listing with customizable page sizes

### 🛡️ **Enterprise-Grade Security**

- **JWT Access & Refresh Tokens**: Stateless authentication with automatic renewal
- **BCrypt Password Hashing**: Industry-standard password encryption
- **OAuth 2.0 Compliance**: Secure third-party authentication
- **CORS Configuration**: Controlled cross-origin resource sharing

### 🏗️ **Modern Tech Stack**

- **Backend**: Java 17+, Spring Boot 3.x, Spring Security 6, Hibernate/JPA
- **Database**: PostgreSQL with connection pooling
- **Documentation**: OpenAPI 3.0 (Swagger) integration
- **Containerization**: Docker & Docker Compose ready
- **Testing**: Comprehensive unit and integration tests

## 🏛️ System Architecture

This application implements a sophisticated **"Dual Token Architecture"** that separates application session management from third-party API access:

### Token Flow Overview

- **Frontend (React)**: Holds JWT app tokens for API authentication
- **Backend (Spring Boot)**: Securely stores Google refresh tokens for Gmail API access
- **Database (PostgreSQL)**: Persistent storage for user data and OAuth tokens

![alt text](image.png)

### 🔄 Authentication Flow Benefits

- **Security**: Google tokens never exposed to client-side code
- **Reliability**: Automatic token refresh prevents session interruptions
- **Scalability**: Stateless JWT architecture supports horizontal scaling
- **Flexibility**: Supports both OAuth and traditional authentication methods

## 🚀 Quick Start

### Prerequisites

- **Docker & Docker Compose** - Container orchestration
- **Java 17+** - (Optional for local development)

- **Google Cloud Project** - Gmail API access

### 1. 🔧 Environment Setup

```bash
cp .env.example .env
```

### 1.1 🤖 AI Summary (Gemini / Google AI Studio)

Set an API key from Google AI Studio on the backend:

```bash
export GOOGLE_AI_STUDIO_API_KEY="your-key"
```

The backend exposes an authenticated endpoint:

- `POST /api/ai/email-summary`
  - Body: `{ "messageId": "<gmailMessageId>", "content": "optional pre-decoded text" }`
  - If `content` is provided, the backend summarizes it directly (avoids extra Gmail fetch latency).
  - If `content` is omitted, the backend fetches the email by `messageId` and extracts text.

### 1.2 🔍 Semantic Search (pgvector + Google Embeddings)

**What is Semantic Search?**
Semantic search uses AI to find emails based on conceptual meaning rather than exact keywords. Query "money" finds emails about "invoice", "price", "salary", "payment", "billing", etc.

**Setup Requirements:**

1. **Enable pgvector extension** (Docker Compose already configured)
2. **Set Google AI API key** for embedding generation:

```bash
export GOOGLE_AI_API_KEY="your-google-ai-api-key"
```

Get your API key from [Google AI Studio](https://makersuite.google.com/app/apikey)

**Endpoints:**

- **`POST /api/gmail/semantic-search`** - Search emails by semantic similarity
  - Body: `{ "query": "money matters", "limit": 20 }`
  - Response: Array of `SearchResult` with `strategy: "SEMANTIC"`
  - Example queries:
    - `"money"` → finds "invoice", "payment", "salary", "billing"
    - `"important updates"` → finds "announcement", "notification", "alert"
    - `"schedule meeting"` → finds "calendar", "appointment", "availability"

- **`POST /api/gmail/embeddings/generate`** - Generate embeddings for existing emails
  - No body required (authenticated endpoint)
  - Response: `{ "processed": 145 }`
  - Use this to backfill embeddings for emails synced before semantic search was enabled

**How it works:**

1. When emails are synced, the system generates 768-dimensional vector embeddings using Google's text-embedding-004 model
2. Embeddings are stored in PostgreSQL with pgvector extension
3. Search queries are converted to embeddings and matched using cosine similarity
4. Results are ranked by semantic relevance (closest vector distance)

**Performance:**

- **Query embedding generation**: ~1-2 seconds (Google AI API call)
- **Vector similarity search**: ~10-50ms with HNSW index
- **Total search time**: ~1-2 seconds end-to-end
- **Embedding generation**: ~100-200ms per email (background during sync)
- **Index type**: HNSW (Hierarchical Navigable Small World) for sub-millisecond vector search

**Index Creation:**

The system automatically creates an HNSW index for fast vector similarity search:

```sql
CREATE INDEX synced_emails_embedding_hnsw_idx
ON synced_emails
USING hnsw (embedding vector_cosine_ops);
```

**Frontend UI:**

The frontend provides a dropdown menu with three search modes:

- **Keyword Search** (🔍): Exact word matching
- **Fuzzy Search** (✨): Typo-tolerant search
- **AI Semantic Search** (🧠): Conceptual similarity using embeddings

**Security Considerations:**

- Google API keys are stored in environment variables
- Embeddings are generated server-side only
- JWT authentication required for all search endpoints
- Vector data stored securely in PostgreSQL

**Token Storage:**

- **Google Refresh Tokens**: Securely stored in PostgreSQL `users` table (encrypted at rest)
- **JWT Access Tokens**: Stored client-side (localStorage), 30-day expiration
- **JWT Refresh Tokens**: Stored server-side in `refresh_tokens` table with user association

📖 **Detailed documentation:** See [SEMANTIC_SEARCH.md](./SEMANTIC_SEARCH.md) for architecture, configuration, and troubleshooting.

### 2. 🔑 Google OAuth 2.0 Setup

**Prerequisites:**

- Google Cloud account
- Project with billing enabled (for Gmail API)

**Step-by-Step Setup:**

1. **Create Google Cloud Project**
   - Visit [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select existing one
   - Note your Project ID

2. **Enable Required APIs**

   ```bash
   # Enable Gmail API
   gcloud services enable gmail.googleapis.com

   # Enable Generative Language API (for embeddings)
   gcloud services enable generativelanguage.googleapis.com
   ```

3. **Configure OAuth Consent Screen**
   - Navigate to: APIs & Services > OAuth consent screen
   - Choose **External** (for testing) or **Internal** (for workspace)
   - Fill in required fields:
     - App name: "Email Dashboard"
     - User support email: your-email@example.com
     - Developer contact: your-email@example.com
   - **Scopes**: Add `https://www.googleapis.com/auth/gmail.readonly`
   - **Test users**: Add email addresses that can access during development

4. **Create OAuth 2.0 Credentials**
   - Navigate to: APIs & Services > Credentials
   - Click **Create Credentials** > **OAuth 2.0 Client ID**
   - Application type: **Web application**
   - Name: "Email Dashboard Client"
   - **Authorized JavaScript origins**:
     - `http://localhost:3000` (backend)
     - `http://localhost:5173` (frontend dev server)
     - Your production domain (when deploying)

## 📡 API Endpoints

### Authentication

- `POST /api/auth/register` - Register new user (local auth)
- `POST /api/auth/login` - Login with email/password
- `POST /api/auth/google` - Login with Google OAuth
- `POST /api/auth/refresh` - Refresh JWT access token
- `POST /api/auth/logout` - Logout (invalidate tokens)

### Email Operations

- `GET /api/gmail/messages` - List emails with pagination
- `GET /api/gmail/messages/{id}` - Get email details
- `GET /api/gmail/labels` - List mailbox labels
- `POST /api/gmail/search` - Keyword/fuzzy search
- `POST /api/gmail/semantic-search` - AI semantic search
- `POST /api/gmail/embeddings/generate` - Generate embeddings

### AI Features

- `POST /api/ai/email-summary` - Generate email summary (Gemini)

### User Management

- `GET /api/users/profile` - Get user profile
- `PUT /api/users/profile` - Update user profile

All endpoints (except auth) require `Authorization: Bearer <jwt-token>` header.

## 🔐 Token Storage & Security

### Token Types

1. **JWT Access Token** (Client-side)
   - **Storage**: Browser localStorage
   - **Expiration**: 30 days
   - **Purpose**: Authenticate API requests
   - **Contains**: User email, avatar URL
   - **Algorithm**: HMAC-SHA384

2. **JWT Refresh Token** (Server-side)
   - **Storage**: PostgreSQL `refresh_tokens` table
   - **Expiration**: 30 days
   - **Purpose**: Generate new access tokens
   - **Security**: UUID-based, one-time use with lock

3. **Google Refresh Token** (Server-side)
   - **Storage**: PostgreSQL `users.google_refresh_token` column
   - **Expiration**: Never (revocable by user)
   - **Purpose**: Access Gmail API without re-authentication
   - **Security**: Encrypted at rest, never exposed to client

### Token Flow Diagram

```
┌─────────────┐         JWT Access Token          ┌─────────────┐
│   Frontend  │◄──────────────────────────────────┤   Backend   │
│  (React)    │                                    │ (Spring)    │
└─────────────┘                                    └─────────────┘
       │                                                  │
       │  API Requests                                    │ Stores Google
       │  (Bearer Token)                                  │ Refresh Token
       │                                                  │
       ▼                                                  ▼
 localStorage                                    ┌─────────────┐
                                                 │ PostgreSQL  │
                                                 │  Database   │
                                                 └─────────────┘
```

### Security Measures

- ✅ **HTTPS Only**: Enforce SSL/TLS in production
- ✅ **CORS**: Whitelist allowed origins
- ✅ **Password Hashing**: BCrypt with 10 rounds
- ✅ **JWT Signing**: HMAC-SHA384 with secret key
- ✅ **Token Rotation**: Refresh tokens invalidated after use
- ✅ **XSS Protection**: Content Security Policy headers
- ✅ **SQL Injection**: Parameterized queries (JPA)
- ✅ **Rate Limiting**: Implement in production (e.g., Spring Bucket4j)

---

**Development Tools:**

```bash
# Format Java code
mvn spotless:apply

# Run tests
mvn test

# Build without tests
mvn clean package -DskipTests

# Check code quality
mvn verify
```

---

**Built with ❤️ by the Team**

---

> **⚠️ Important**: This is a demonstration project. For production deployment:
>
> - Enable HTTPS with valid SSL certificates
> - Implement rate limiting and DDoS protection
> - Set up monitoring and alerting (e.g., Prometheus, Grafana)
> - Enable database backups and disaster recovery
> - Conduct security audits and penetration testing
> - Comply with GDPR, CCPA, and other data protection regulations
> - Use secrets management (AWS Secrets Manager, HashiCorp Vault)
> - Implement comprehensive logging and audit trails
>   google-ai:

     api-key: "your-google-ai-studio-api-key"

````

**Security Best Practices:**

- ✅ Never commit credentials to git
- ✅ Use environment variables for production
- ✅ Rotate API keys periodically
- ✅ Restrict OAuth scopes to minimum required
- ✅ Enable audit logging in Google Cloud Console
- ✅ Use separate projects for dev/staging/production

### 3. 🐳 Deploy with Docker

```bash
# Build and start all services
docker compose up --build -d

# View logs
docker compose logs -f backend

# Stop services
docker compose down
````

**Service URLs:**

- 🖥️ **Backend API**: http://localhost:3000
- 📚 **API Documentation**: http://localhost:3000/swagger-ui/index.html
- 🗄️ **Database**: localhost:5432

---

**Format code:**

```bash
mvn spotless:apply
```

---

**Built with Passion**

---

> **Note**: This is a demonstration project. For production use, ensure proper security audits, monitoring, and compliance with data protection regulations.
