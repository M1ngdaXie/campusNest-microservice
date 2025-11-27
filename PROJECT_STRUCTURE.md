# CampusNest Microservices

Spring Boot microservices platform for campus housing and student services.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Client Layer                        │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│              API Gateway :8080 (Entry Point)            │
│  - JWT Authentication                                    │
│  - Request Routing                                       │
│  - CORS Configuration                                    │
└─────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┼───────────────────┐
        ↓                   ↓                   ↓
┌──────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ User Service │  │ Housing Service  │  │ Messaging Service│
│    :8081     │  │      :8082       │  │      :8083       │
│              │  │                  │  │                  │
│ - Auth       │  │ - Listings      │  │ - Conversations  │
│ - Users      │  │ - Images (S3)   │  │ - Real-time Chat │
│ - Admin      │  │ - Search        │  │ - WebSocket      │
└──────────────┘  └──────────────────┘  └──────────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            ↓
        ┌───────────────────┼───────────────────┐
        ↓                   ↓                   ↓
┌──────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ Eureka Server│  │   MySQL :3306    │  │  Redis :6379     │
│    :8761     │  │                  │  │                  │
│              │  │ - campusNest_    │  │ - Caching        │
│ - Service    │  │   users          │  │ - User Presence  │
│   Discovery  │  │ - campusNest_    │  │                  │
│              │  │   messaging      │  │                  │
└──────────────┘  └──────────────────┘  └──────────────────┘
```

## Services

### API Gateway (:8080)
- Single entry point for all requests
- JWT authentication
- Request routing to microservices

### User Service (:8081)
- User registration and login
- JWT token management
- Email verification
- Admin operations

### Housing Service (:8082)
- Housing listing CRUD
- Image upload to AWS S3
- Search with Redis caching

### Messaging Service (:8083)
- Real-time messaging via WebSocket
- Conversation management
- Message status tracking

### Eureka Server (:8761)
- Service discovery and registration
- Health monitoring

## Tech Stack

- Java 17 + Spring Boot 3.x
- Spring Cloud Gateway + Eureka
- MySQL 8.0 + Redis 7
- AWS S3 for image storage
- Docker + Docker Compose

## Quick Start

1. Clone repository
2. Copy `.env.example` to `.env` and configure
3. Run `docker-compose up -d`
4. Access API Gateway at `http://localhost:8080`

## Project Structure

```
├── api-gateway/           # API Gateway
├── user-service/          # User & Auth Service
├── housing-service/       # Housing Listings Service
├── messaging-service/     # Real-time Messaging Service
├── eureka-server/         # Service Discovery
├── docker-compose.yml     # Docker orchestration
└── .env.example           # Environment template
```

## Environment Setup

Create `.env` file with:
- Database credentials (MySQL)
- JWT secret key
- Email credentials (Gmail SMTP)
- AWS S3 credentials

See `.env.example` for template.