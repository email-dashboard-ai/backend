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

- First search after enabling: May need to generate embeddings (use `/embeddings/generate`)
- Subsequent searches: Fast vector similarity search with IVFFlat index
- Embedding generation: ~100ms per email (done in background during sync)

📖 **Detailed documentation:** See [SEMANTIC_SEARCH.md](./SEMANTIC_SEARCH.md) for architecture, configuration, and troubleshooting.

### 2. 🔑 Google Cloud Setup

1. **Create Google Cloud Project**
   - Visit [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select existing one

2. **Enable APIs**

   ```bash
   # Enable Gmail API
   gcloud services enable gmail.googleapis.com
   ```

3. **Configure OAuth Consent Screen**
   - Add authorized domains
   - Set scopes (APIs & Services > OAuth Consent Screen > Data Access): `https://www.googleapis.com/auth/gmail.readonly`
   - Add test users

4. **Create OAuth 2.0 Credentials**
   - Application type: Web application
   - Authorized origins: `http://localhost:3000`, `http://localhost:5173`
   - **Note:** Since we use the popup flow (`postmessage`), you must add these to **Authorized JavaScript origins**. The **Authorized redirect URIs** field can be left empty.

### 3. 🐳 Deploy with Docker

```bash
# Build and start all services
docker compose up --build -d

# View logs
docker compose logs -f backend

# Stop services
docker compose down
```

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
