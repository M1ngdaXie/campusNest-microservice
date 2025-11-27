# Messaging Service Migration Guide
**From:** CampusNest Monolith → Messaging Microservice
**Author:** Migration Guide
**Date:** 2025-11-21

---

## 📋 Table of Contents
1. [Overview](#overview)
2. [What You're Migrating](#what-youre-migrating)
3. [Architecture Changes](#architecture-changes)
4. [Step-by-Step Migration](#step-by-step-migration)
5. [Key Modifications](#key-modifications)
6. [Testing Guide](#testing-guide)

---

## 🎯 Overview

### What's Already Built (Monolithic App)
✅ Conversation model (2 participants + housing listing)
✅ Message model with MessageType enum
✅ MessageStatus model for read tracking
✅ REST API endpoints for messaging
✅ WebSocket real-time messaging
✅ Typing indicators
✅ User presence tracking (Redis)
✅ Unread count tracking
✅ Message event publishing

### What We're Building (Microservice)
🎯 Standalone messaging-service
🎯 Separate messaging database
🎯 Feign clients for user-service & housing-service
🎯 Decoupled from User/HousingListing entities
🎯 WebSocket endpoints
🎯 Redis for presence & caching

---

##

 🏗️ Architecture Changes

### **Before (Monolithic)**
```
┌────────────────────────────────┐
│   CampusNest Application       │
│                                │
│  - User Management             │
│  - Housing Listings            │
│  - Messaging (WebSocket)       │
│  - All in one DB               │
└────────────────────────────────┘
```

### **After (Microservices)**
```
┌──────────┐  ┌──────────────┐  ┌──────────────────┐
│  User    │  │   Housing    │  │   Messaging      │
│ Service  │  │   Service    │  │   Service        │
│          │  │              │  │                  │
│ users_db │  │ housing_db   │  │ messaging_db     │
└─────┬────┘  └──────┬───────┘  └────┬─────────────┘
      │              │                │
      └──────────────┴────────────────┘
              Feign Client Calls
```

---

## 📦 What You're Migrating

### **Files to Copy & Modify:**

#### **1. Models** (entities need modification)
```
Source: campusnest-platform/src/.../models/
- Message.java          → MODIFY (remove User/HousingListing FK)
- Conversation.java     → MODIFY (store IDs instead)
- MessageStatus.java    → COPY AS-IS
```

#### **2. Enums**
```
- MessageType.java      → COPY AS-IS
```

#### **3. Repositories**
```
Source: campusnest-platform/src/.../repository/message/
- MessageRepository.java         → MODIFY queries
- ConversationRepository.java    → MODIFY queries
- MessageStatusRepository.java   → COPY AS-IS
```

#### **4. Services**
```
Source: campusnest-platform/src/.../services/
- MessagingService.java          → MODIFY interface
- MessagingServiceImpl.java      → HEAVY MODIFICATION
- UserPresenceService.java       → COPY AS-IS
- MessageEventProducer.java      → OPTIONAL (can skip for now)
```

#### **5. Controllers**
```
Source: campusnest-platform/src/.../controllers/websocket/
- MessagingController.java             → MODIFY (use Feign)
- WebSocketMessagingController.java    → MODIFY (use Feign)
```

#### **6. Configuration**
```
Source: campusnest-platform/src/.../config/websocket/
- WebSocketConfig.java                → COPY & ADAPT
- WebSocketAuthenticationHandler.java → COPY & ADAPT
```

#### **7. DTOs (Requests/Responses)**
```
Source: campusnest-platform/src/.../requests/
- SendMessageRequest.java           → COPY AS-IS
- ChatMessageRequest.java           → COPY AS-IS
- CreateConversationRequest.java    → MODIFY
- TypingIndicatorRequest.java       → COPY AS-IS

Source: campusnest-platform/src/.../response/
- MessageResponse.java              → MODIFY
- ChatMessageResponse.java          → MODIFY
- ConversationSummaryResponse.java  → MODIFY
- ConversationDetailResponse.java   → MODIFY
- TypingIndicatorResponse.java      → COPY AS-IS
```

---

## 🔧 Step-by-Step Migration

### **Step 1: Create Project Structure**

```bash
cd /Users/xiemingda/Downloads/campusnest-microservices
mkdir -p messaging-service/src/main/java/com/campusnest/messagingservice
mkdir -p messaging-service/src/main/resources
```

Create directory structure:
```
messaging-service/
├── src/main/java/com/campusnest/messagingservice/
│   ├── MessagingServiceApplication.java
│   ├── config/
│   │   ├── RedisConfig.java
│   │   ├── SecurityConfig.java
│   │   └── websocket/
│   │       ├── WebSocketConfig.java
│   │       └── WebSocketAuthenticationHandler.java
│   ├── models/
│   │   ├── Conversation.java
│   │   ├── Message.java
│   │   └── MessageStatus.java
│   ├── enums/
│   │   └── MessageType.java
│   ├── repository/
│   │   ├── ConversationRepository.java
│   │   ├── MessageRepository.java
│   │   └── MessageStatusRepository.java
│   ├── services/
│   │   ├── MessagingService.java
│   │   ├── MessagingServiceImpl.java
│   │   └── UserPresenceService.java
│   ├── controllers/
│   │   ├── MessagingController.java
│   │   └── WebSocketMessagingController.java
│   ├── client/
│   │   ├── UserServiceClient.java
│   │   └── HousingServiceClient.java
│   ├── dto/
│   │   ├── UserDTO.java
│   │   └── HousingListingDTO.java
│   ├── requests/
│   ├── response/
│   └── events/
├── src/main/resources/
│   ├── application.properties
│   └── application-docker.properties
├── Dockerfile
└── pom.xml
```

---

### **Step 2: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>com.campusnest</groupId>
    <artifactId>messaging-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>messaging-service</name>
    <description>Messaging Microservice for CampusNest Platform</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.3</spring-cloud.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Eureka Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- Feign Client for service-to-service communication -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Redis for caching & presence -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Jackson Hibernate6 module -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-hibernate6</artifactId>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.13.0</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.13.0</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.13.0</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### **Step 3: Key Modifications Required**

#### **3.1 Conversation.java - CRITICAL CHANGES**

**BEFORE (Monolithic):**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "participant1_id", nullable = false)
private User participant1;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "participant2_id", nullable = false)
private User participant2;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "housing_listing_id", nullable = false)
private HousingListing housingListing;
```

**AFTER (Microservice):**
```java
@Column(name = "participant1_id", nullable = false)
private Long participant1Id;

@Column(name = "participant2_id", nullable = false)
private Long participant2Id;

@Column(name = "housing_listing_id", nullable = false)
private Long housingListingId;

// For caching user details
@Transient
private String participant1Email;

@Transient
private String participant2Email;
```

#### **3.2 Message.java - CRITICAL CHANGES**

**BEFORE:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "sender_id", nullable = false)
private User sender;
```

**AFTER:**
```java
@Column(name = "sender_id", nullable = false)
private Long senderId;

// For caching
@Transient
private String senderEmail;

@Transient
private String senderName;
```

---

### **Step 4: Create Feign Clients**

#### **4.1 UserServiceClient.java**
```java
package com.campusnest.messagingservice.client;

import com.campusnest.messagingservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/users/email/{email}")
    UserDTO getUserByEmail(@PathVariable("email") String email);
}
```

#### **4.2 HousingServiceClient.java**
```java
package com.campusnest.messagingservice.client;

import com.campusnest.messagingservice.dto.HousingListingDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "housing-service")
public interface HousingServiceClient {

    @GetMapping("/api/housing/{id}")
    HousingListingDTO getListingById(@PathVariable("id") Long id);
}
```

#### **4.3 DTOs**

**UserDTO.java:**
```java
package com.campusnest.messagingservice.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
}
```

**HousingListingDTO.java:**
```java
package com.campusnest.messagingservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class HousingListingDTO {
    private Long id;
    private String title;
    private String city;
    private BigDecimal price;
    private Boolean isActive;
}
```

---

### **Step 5: Modify MessagingServiceImpl**

Key changes needed:

```java
// BEFORE (Monolithic):
public Conversation createOrGetConversation(User user1, User user2, HousingListing listing) {
    // Used full objects
}

// AFTER (Microservice):
public Conversation createOrGetConversation(Long user1Id, Long user2Id, Long listingId) {
    // Validate users exist via Feign
    UserDTO user1 = userServiceClient.getUserById(user1Id);
    UserDTO user2 = userServiceClient.getUserById(user2Id);

    // Validate listing exists via Feign
    HousingListingDTO listing = housingServiceClient.getListingById(listingId);

    // Check if conversation exists
    Optional<Conversation> existing = conversationRepository
        .findByParticipantIdsAndListingId(user1Id, user2Id, listingId);

    if (existing.isPresent()) {
        return existing.get();
    }

    // Create new
    Conversation conversation = new Conversation();
    conversation.setParticipant1Id(user1Id);
    conversation.setParticipant2Id(user2Id);
    conversation.setHousingListingId(listingId);
    conversation.setCreatedAt(LocalDateTime.now());
    conversation.setIsActive(true);

    return conversationRepository.save(conversation);
}
```

---

### **Step 6: Update application.properties**

```properties
# Server
server.port=8083
spring.application.name=messaging-service

# Database
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/campusNest_messaging?useSSL=false&serverTimezone=UTC}
spring.datasource.username=${DB_USERNAME:campusnest}
spring.datasource.password=${DB_PASSWORD:campusnest123}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}

# Eureka
eureka.client.serviceUrl.defaultZone=${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.prefer-ip-address=true

# JWT
jwt.secret=${JWT_SECRET:doitouKatsuki12345rtyui567gy3g2eygeh23fyg2hy3ue}

# Actuator
management.endpoints.web.exposure.include=health,info

# Logging
logging.level.com.campusnest.messagingservice=INFO
```

---

### **Step 7: Create Dockerfile**

```dockerfile
FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

COPY pom.xml .
COPY src src

RUN apk add --no-cache maven
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp

ARG JAR_FILE=/workspace/app/target/*.jar
COPY --from=build ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
```

---

### **Step 8: Update docker-compose.yml**

```yaml
# Add to docker-compose.yml

  # Messaging Service
  messaging-service:
    build:
      context: ./messaging-service
      dockerfile: Dockerfile
    container_name: campusnest-micro-messaging-service
    restart: unless-stopped
    depends_on:
      eureka-server:
        condition: service_healthy
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      # Database Configuration
      DB_URL: jdbc:mysql://mysql:3306/${MESSAGING_DB_NAME:-campusNest_messaging}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      DB_USERNAME: ${DB_USERNAME:-campusnest}
      DB_PASSWORD: ${DB_PASSWORD:-campusnest123}

      # JWT Configuration
      JWT_SECRET: ${JWT_SECRET:-doitouKatsuki12345rtyui567gy3g2eygeh23fyg2hy3ue}

      # Redis Configuration
      REDIS_HOST: redis
      REDIS_PORT: 6379

      # Eureka Configuration
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/

      # Spring Profile
      SPRING_PROFILES_ACTIVE: docker
    ports:
      - "8083:8083"
    networks:
      - campusnest-network
    deploy:
      resources:
        limits:
          memory: 768M
        reservations:
          memory: 384M
```

---

### **Step 9: Update API Gateway**

Add routing for messaging service in API Gateway configuration.

---

## ✅ Migration Checklist

- [ ] Step 1: Create project structure
- [ ] Step 2: Copy pom.xml
- [ ] Step 3: Modify entities (Conversation, Message, MessageStatus)
- [ ] Step 4: Create Feign clients (UserServiceClient, HousingServiceClient)
- [ ] Step 5: Copy & modify repositories
- [ ] Step 6: Copy & heavily modify MessagingServiceImpl
- [ ] Step 7: Copy & modify controllers
- [ ] Step 8: Copy WebSocket configuration
- [ ] Step 9: Copy UserPresenceService (should work as-is with Redis)
- [ ] Step 10: Create application.properties
- [ ] Step 11: Create Dockerfile
- [ ] Step 12: Update docker-compose.yml
- [ ] Step 13: Update API Gateway routes
- [ ] Step 14: Test REST endpoints
- [ ] Step 15: Test WebSocket connections

---

## 🧪 Testing Guide

### **1. Test REST API**
```bash
# Get conversations
curl http://localhost:8083/api/messaging/conversations \
  -H "X-User-Email: test@example.com"

# Create conversation
curl -X POST http://localhost:8083/api/messaging/conversations \
  -H "Content-Type: application/json" \
  -H "X-User-Email: test@example.com" \
  -d '{
    "otherParticipantId": 2,
    "housingListingId": 1,
    "initialMessage": "Hello!"
  }'
```

### **2. Test WebSocket**
Use frontend or WebSocket client to connect to:
```
ws://localhost:8083/ws
```

---

## 📝 Summary

**What Changed:**
- ✅ Entities now store IDs instead of full objects
- ✅ Feign clients fetch user/listing data when needed
- ✅ Separate messaging database
- ✅ WebSocket still works (same config)
- ✅ Redis presence tracking unchanged
- ✅ Service discovery via Eureka

**What Stays the Same:**
- ✅ WebSocket protocol/endpoints
- ✅ Message format
- ✅ REST API structure
- ✅ Redis usage for presence

---

**Next Steps:** Start with Step 1 and work through each step methodically!