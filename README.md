# HistoryAI

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-blue.svg" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-green.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/React-19-blue.svg" alt="React">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
</p>

An AI-powered educational platform enabling conversations with historical figures, featuring real-time fact-checking against Wikipedia sources.

## Overview

HistoryAI bridges the gap between artificial intelligence and historical education by allowing users to engage in realistic conversations with famous historical personalities. The application combines Large Language Models (via Ollama) with live Wikipedia data to provide accurate, contextually rich responses while maintaining historical authenticity.

## Features

### Core Functionality

- **Character Selection** — Browse and select from a curated database of historical figures (Copernicus, Marie Curie, Winston Churchill, and more)
- **AI-Powered Conversations** — Engage in natural language chats powered by Ollama (supports llama3.2, mistral, and other models)
- **Live Streaming Responses** — Experience real-time AI response generation with Server-Sent Events (SSE)
- **Smart Context Integration** — Each conversation is enriched with relevant Wikipedia context about the historical figure

### Fact-Checking System

- **Automatic Claim Detection** — Identifies factual claims within conversations
- **Wikipedia Verification** — Cross-references claims against Wikipedia and Wikidata sources
- **Confidence Scoring** — Provides verification status (VERIFIED/UNVERIFIED/UNVERIFIABLE) with confidence levels
- **Quote Enrichment** — Enhances responses with relevant historical quotes from Wikiquote

### Developer Experience

- **Comprehensive Testing** — Unit, integration, and E2E test coverage
- **Code Quality** — Checkstyle enforcement with Google Java Style Guide
- **API Documentation** — RESTful endpoints with Spring Boot
- **Observability** — Distributed tracing with X-Trace-Id correlation

## Technology Stack

| Component | Technology |
|-----------|------------|
| Backend | Java 25, Spring Boot 3.5, Spring AI |
| Database | PostgreSQL with Flyway migrations |
| AI | Ollama (local LLM) |
| Data Sources | Wikipedia REST API, Wikidata API, Wikiquote API |
| Frontend | React 19, TypeScript, Tailwind CSS |
| Testing | JUnit 5, Vitest, Playwright |
| Caching | Caffeine (in-memory) |

## Getting Started

### Prerequisites

- Java 25+
- Node.js 20+
- PostgreSQL 16+
- Ollama (optional, for local AI inference)

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/Dekrate/history-ai.git
   cd history-ai
   ```

2. **Start the database**
   ```bash
   docker-compose up -d postgres
   ```

3. **Start Ollama** (optional, for local AI)
   ```bash
   ollama serve
   ollama pull llama3.2:3b
   ```

4. **Run the backend**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

5. **Run the frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

The application will be available at `http://localhost:5173`

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | history_ai | Database name |
| `DB_USER` | postgres | Database user |
| `DB_PASSWORD` | postgres | Database password |
| `OLLAMA_BASE_URL` | http://localhost:11434 | Ollama server URL |
| `OLLAMA_MODEL` | SpeakLeash/bielik-11b-v3.0-instruct:bf16 | AI model to use |
| `SPRING_PROFILES_ACTIVE` | dev | Active Spring profile |

## Project Structure

```
history-ai/
├── backend/                 # Spring Boot application
│   └── src/
│       ├── main/
│       │   ├── java/com/historyai/
│       │   │   ├── client/       # External API clients (Wikipedia, Wikidata, Ollama)
│       │   │   ├── config/       # Configuration classes
│       │   │   ├── controller/   # REST controllers
│       │   │   ├── dto/          # Data Transfer Objects
│       │   │   ├── entity/        # JPA entities
│       │   │   ├── exception/     # Custom exceptions
│       │   │   ├── repository/    # JPA repositories
│       │   │   └── service/       # Business logic
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/  # Flyway migrations
│       └── test/                 # Test sources
├── frontend/                # React application
│   └── src/
│       ├── pages/           # Page components
│       ├── services/        # API services
│       ├── types/           # TypeScript types
│       └── utils/           # Utility functions
├── docker-compose.yml      # Docker services
└── README.md
```

## API Endpoints

### Character Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/characters` | List all characters |
| GET | `/api/characters/{id}` | Get character by ID |
| GET | `/api/characters/search?q=` | Search characters |
| POST | `/api/characters/import` | Import from Wikipedia |

### Chat

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/chat/stream` | Stream chat response (SSE) |

### Fact-Checking

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/factcheck` | Check claim against sources |

## Development

### Running Tests

```bash
# Backend tests
cd backend && mvn test

# Frontend tests
cd frontend && npm test

# All tests
mvn test && cd frontend && npm test
```

### Code Quality

```bash
# Backend checkstyle
cd backend && mvn checkstyle:check

# Frontend lint
cd frontend && npm run lint
```

### Building

```bash
# Backend
cd backend && mvn package

# Frontend
cd frontend && npm run build
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend                              │
│                    (React + TypeScript)                     │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTP / SSE
┌─────────────────────▼───────────────────────────────────────┐
│                        Backend                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Controller  │  │   Service    │  │ Repository   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                 │                                   │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐       │
│  │   Ollama    │  │  Wikipedia   │  │ PostgreSQL   │       │
│  │   Client    │  │   Client     │  │              │       │
│  └─────────────┘  └──────────────┘  └─────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

## License

This project is licensed under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
