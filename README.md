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
   - Add test users (APIs & Services > OAuth Consent Screen > Data Access - for development)

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
