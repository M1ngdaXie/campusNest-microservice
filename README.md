# CampusNest Microservices Platform

A modern, scalable microservices platform designed to streamline campus housing management and student communication. Built with Spring Boot and modern cloud-native technologies.

## Overview

CampusNest is a comprehensive platform that helps students find campus housing, communicate with each other, and get AI-powered assistance. The platform leverages a microservices architecture to ensure scalability, maintainability, and high availability.

## Architecture

```
                                    ┌─────────────────┐
                                    │   Web Client    │
                                    │  (React/Next)   │
                                    └────────┬────────┘
                                             │
                                             ▼
                        ┌────────────────────────────────────┐
                        │      API Gateway :8080             │
                        │  • JWT Authentication              │
                        │  • Request Routing                 │
                        │  • Load Balancing                  │
                        │  • CORS Configuration              │
                        └──────────────┬─────────────────────┘
                                       │
                ┌──────────────────────┼───────────────────────┐
                │                      │                       │
                ▼                      ▼                       ▼
        ┌───────────────┐    ┌────────────────┐    ┌──────────────────┐
        │ User Service  │    │Housing Service │    │Messaging Service │
        │    :8081      │    │     :8082      │    │      :8083       │
        │               │    │                │    │                  │
        │ • Auth/Login  │    │ • Listings     │    │ • Chat           │
        │ • Registration│    │ • Search       │    │ • WebSocket      │
        │ • Profiles    │    │ • Images (S3)  │    │ • Notifications  │
        │ • Email       │    │ • Caching      │    │ • User Presence  │
        └───────┬───────┘    └────────┬───────┘    └────────┬─────────┘
                │                     │                      │
                └─────────────────────┼──────────────────────┘
                                      │
                ┌─────────────────────┼─────────────────────┐
                │                     │                     │
                ▼                     ▼                     ▼
        ┌───────────────┐    ┌────────────────┐   ┌────────────────┐
        │ Eureka Server │    │  MySQL :3306   │   │  Redis :6379   │
        │    :8761      │    │                │   │                │
        │ • Service     │    │ • User DB      │   │ • Caching      │
        │   Discovery   │    │ • Housing DB   │   │ • Sessions     │
        │ • Health      │    │ • Messaging DB │   │ • Presence     │
        │   Monitoring  │    │ • AI DB        │   │ • Rate Limit   │
        └───────────────┘    └────────────────┘   └────────────────┘
                                      │
                                      ▼
                            ┌──────────────────┐
                            │  AI Assistant    │
                            │  Service :8084   │
                            │  (FastAPI/Python)│
                            │                  │
                            │ • OpenAI GPT     │
                            │ • Context Cache  │
                            │ • Recommendations│
                            └──────────────────┘
```

## Key Features

### For Students
- **Housing Search**: Advanced search and filtering for campus housing listings
- **Real-time Messaging**: Chat directly with landlords and other students via WebSocket
- **AI Assistant**: Get intelligent recommendations and answers to housing-related questions
- **Secure Authentication**: JWT-based authentication with email verification
- **Image Support**: View high-quality housing photos stored on AWS S3

### For Administrators
- **User Management**: Complete control over user accounts and permissions
- **Content Moderation**: Monitor and manage housing listings
- **Analytics**: Track platform usage and performance metrics
- **System Health**: Real-time monitoring via Eureka dashboard

## Technology Stack

### Backend
- **Java 17** - Modern LTS version with performance improvements
- **Spring Boot 3.3.5** - Latest Spring Boot framework
- **Spring Cloud 2023.0.3** - Microservices infrastructure
- **Spring Cloud Gateway** - API Gateway with routing and filters
- **Netflix Eureka** - Service discovery and registration
- **Spring Data JPA** - Database ORM with Hibernate

### Data Layer
- **MySQL 8.0** - Primary relational database
- **Redis 7** - Caching and session management
- **AWS S3** - Cloud storage for images

### AI/ML
- **Python FastAPI** - High-performance Python web framework
- **OpenAI GPT-4** - Natural language processing and recommendations

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **Maven** - Build automation and dependency management

## Microservices Details

### 1. API Gateway (:8080)
**Purpose**: Single entry point for all client requests

**Responsibilities**:
- Route requests to appropriate microservices
- JWT token validation and authentication
- CORS policy enforcement
- Load balancing across service instances
- Request/response logging

**Technology**: Spring Cloud Gateway

---

### 2. User Service (:8081)
**Purpose**: User management and authentication

**Features**:
- User registration with email verification
- Login/logout with JWT tokens (access + refresh)
- Password reset and change functionality
- User profile management
- Admin role management
- Email notifications

**Database**: campusNest_users

**Integrations**: Gmail SMTP for emails

---

### 3. Housing Service (:8082)
**Purpose**: Housing listing management

**Features**:
- Create, read, update, delete housing listings
- Image upload to AWS S3
- Advanced search with filters (price, location, amenities)
- Redis caching for frequently accessed listings
- Pagination and sorting
- Bloom filter for optimized queries

**Database**: campusNest_users (housing tables)

**Integrations**: AWS S3, Redis

---

### 4. Messaging Service (:8083)
**Purpose**: Real-time communication between users

**Features**:
- WebSocket-based real-time chat
- One-on-one conversations
- Message persistence
- Read receipts and message status
- User online/offline presence tracking
- Conversation history

**Database**: campusNest_messaging

**Integrations**: Redis for presence tracking

---

### 5. AI Assistant Service (:8084)
**Purpose**: Intelligent housing recommendations and Q&A

**Features**:
- Natural language query processing
- Context-aware responses
- Housing recommendation engine
- Conversation history
- Rate limiting and cost optimization

**Database**: campusNest_ai

**Integrations**: OpenAI API, Redis for context caching

**Technology**: Python, FastAPI

---

### 6. Eureka Server (:8761)
**Purpose**: Service discovery and registration

**Features**:
- Automatic service registration
- Health check monitoring
- Service instance tracking
- Load balancing support
- Dashboard UI at http://localhost:8761

## Getting Started

### Prerequisites
- Docker Desktop installed
- Java 17 (for local development)
- Maven 3.8+ (for local development)
- Git

### Quick Start with Docker

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd campusnest-microservices
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your credentials
   ```

3. **Start all services**
   ```bash
   docker-compose up -d
   ```

4. **Verify services are running**
   ```bash
   docker-compose ps
   ```

5. **Access the platform**
   - API Gateway: http://localhost:8080
   - Eureka Dashboard: http://localhost:8761
   - Individual services accessible through the gateway

### Environment Configuration

Create a `.env` file with the following required variables:

```env
# Database
DB_ROOT_PASSWORD=your_root_password
DB_NAME=campusNest_users
DB_USERNAME=campusnest
DB_PASSWORD=your_db_password

# JWT Security
JWT_SECRET=your_jwt_secret_key_min_256_bits
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Email (Gmail SMTP)
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# AWS S3
AWS_S3_BUCKET_NAME=campusnest-images
AWS_S3_REGION=us-east-1
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key

# OpenAI
OPENAI_API_KEY=your_openai_api_key
AI_MODEL=gpt-4
AI_MAX_TOKENS=500

# URLs
FRONTEND_URL=http://localhost:3000
BACKEND_URL=http://localhost:8081
```

### Local Development

1. **Start infrastructure services**
   ```bash
   docker-compose up -d mysql redis
   ```

2. **Build all services**
   ```bash
   mvn clean package
   ```

3. **Run services individually**
   ```bash
   # Terminal 1 - Eureka Server
   cd eureka-server && mvn spring-boot:run

   # Terminal 2 - User Service
   cd user-service && mvn spring-boot:run

   # Terminal 3 - Housing Service
   cd housing-service && mvn spring-boot:run

   # Terminal 4 - Messaging Service
   cd messaging-service && mvn spring-boot:run

   # Terminal 5 - API Gateway
   cd api-gateway && mvn spring-boot:run
   ```

## API Documentation

### Authentication Endpoints (via API Gateway)

```
POST   /api/users/register          - Register new user
POST   /api/users/login             - User login
POST   /api/users/refresh-token     - Refresh JWT token
POST   /api/users/logout            - User logout
GET    /api/users/verify-email      - Email verification
POST   /api/users/reset-password    - Request password reset
```

### Housing Endpoints

```
GET    /api/housing/listings        - Get all listings (with pagination)
GET    /api/housing/listings/{id}   - Get specific listing
POST   /api/housing/listings        - Create new listing (auth required)
PUT    /api/housing/listings/{id}   - Update listing (auth required)
DELETE /api/housing/listings/{id}   - Delete listing (auth required)
GET    /api/housing/search          - Search listings with filters
```

### Messaging Endpoints

```
WebSocket: ws://localhost:8080/ws   - WebSocket connection
GET    /api/messages/conversations  - Get user's conversations
GET    /api/messages/{id}/messages  - Get conversation messages
POST   /api/messages/send           - Send message (via WebSocket)
```

### AI Assistant Endpoints

```
POST   /api/ai/chat                 - Send message to AI assistant
GET    /api/ai/history              - Get chat history
POST   /api/ai/recommendations      - Get housing recommendations
```

## Project Structure

```
campusnest-microservices/
├── api-gateway/                    # API Gateway service
│   ├── src/main/java/
│   └── Dockerfile
├── user-service/                   # User & Authentication service
│   ├── src/main/java/
│   └── Dockerfile
├── housing-service/                # Housing listings service
│   ├── src/main/java/
│   └── Dockerfile
├── messaging-service/              # Real-time messaging service
│   ├── src/main/java/
│   └── Dockerfile
├── ai-assistant-service/           # AI assistant service (Python)
│   ├── app/
│   ├── requirements.txt
│   └── Dockerfile
├── eureka-server/                  # Service discovery
│   ├── src/main/java/
│   └── Dockerfile
├── init-db/                        # Database initialization scripts
│   ├── 01-create-user-db.sql
│   ├── 02-create-messaging-db.sql
│   └── 03-create-ai-db.sql
├── scripts/                        # Utility scripts
├── docker-compose.yml              # Docker orchestration
├── pom.xml                         # Parent Maven configuration
├── .env.example                    # Environment template
└── README.md                       # This file
```

## Performance Optimizations

### Caching Strategy
- **Redis**: Frequently accessed housing listings cached for 1 hour
- **Bloom Filter**: Optimizes database queries in housing service
- **AI Context Cache**: Reduces OpenAI API calls and costs

### Database Optimizations
- **Connection Pooling**: HikariCP for efficient connection management
- **Lazy Loading Fix**: Resolved Hibernate N+1 query problems
- **Indexes**: Optimized queries on frequently searched fields

### Scalability Features
- **Stateless Services**: All services are horizontally scalable
- **Load Balancing**: Spring Cloud LoadBalancer for service-to-service calls
- **Health Checks**: Docker health checks ensure service availability
- **Resource Limits**: Docker memory limits prevent resource exhaustion

## Monitoring and Health

### Service Health Endpoints
```
GET /actuator/health          - Service health status
GET /actuator/info            - Service information
GET /actuator/metrics         - Service metrics
```

### Eureka Dashboard
Access the Eureka dashboard at http://localhost:8761 to monitor:
- Registered service instances
- Service health status
- Instance metadata
- Last heartbeat timestamps

## Security Features

- **JWT Authentication**: Secure token-based authentication
- **Password Hashing**: BCrypt for password storage
- **CORS Configuration**: Controlled cross-origin access
- **Email Verification**: Prevents fake account creation
- **Role-Based Access**: Admin and user role separation
- **Token Refresh**: Long-lived refresh tokens with short-lived access tokens


### Services not registering with Eureka
- Wait 30-60 seconds for initial registration
- Check EUREKA_CLIENT_SERVICEURL_DEFAULTZONE in docker-compose.yml
- Verify Eureka is running: http://localhost:8761

### Email verification not working
- Check MAIL_USERNAME and MAIL_PASSWORD in .env
- Ensure Gmail "App Password" is used (not regular password)
- Check spam folder

## Future Enhancements

- Elasticsearch integration for advanced search
- Kafka for event-driven architecture
- Kubernetes deployment configurations
- GraphQL API gateway option
- Mobile app support
- Payment integration for rent transactions
- Review and rating system
- Image recognition for listing verification
