# Eureka Service Discovery & Caching in Housing Service

## Part 1: Netflix Eureka Service Discovery

### What is Eureka?

Eureka is a **service registry** that acts as a phonebook for microservices. Instead of hardcoding URLs like `http://localhost:8081`, services register themselves with Eureka and discover each other dynamically.

### How Eureka Works - The Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ STEP 1: Service Registration (on startup)                       │
│                                                                  │
│  user-service starts → Registers with Eureka                    │
│  POST http://eureka:8761/eureka/apps/USER-SERVICE              │
│  {                                                               │
│    "instance": {                                                 │
│      "hostName": "user-service",                                 │
│      "app": "USER-SERVICE",                                      │
│      "ipAddr": "172.18.0.3",                                     │
│      "port": 8081,                                               │
│      "healthCheckUrl": "http://user-service:8081/actuator/health"│
│    }                                                             │
│  }                                                               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ STEP 2: Heartbeat (every 30 seconds)                            │
│                                                                  │
│  user-service → Eureka: "I'm still alive!"                      │
│  PUT http://eureka:8761/eureka/apps/USER-SERVICE/instance-id   │
│                                                                  │
│  If no heartbeat for 90 seconds → Eureka removes the service    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ STEP 3: Service Discovery (when API Gateway needs a service)    │
│                                                                  │
│  API Gateway → Eureka: "Where is USER-SERVICE?"                 │
│  GET http://eureka:8761/eureka/apps/USER-SERVICE               │
│                                                                  │
│  Eureka responds:                                                │
│  [                                                               │
│    "http://172.18.0.3:8081",  ← Instance 1                      │
│    "http://172.18.0.4:8081"   ← Instance 2 (if scaled)          │
│  ]                                                               │
│                                                                  │
│  API Gateway picks one (round-robin) and makes request          │
└─────────────────────────────────────────────────────────────────┘
```

### Architecture Before Eureka (Current)

```
┌──────────────┐
│ API Gateway  │
│              │
│ Hardcoded:   │
│ user-service │──────────────────────┐
│ = localhost: │                      │
│   8081       │                      ↓
└──────────────┘              ┌───────────────┐
                              │ user-service  │
                              │ :8081         │
                              └───────────────┘

Problems:
❌ Can't scale (what if we add user-service:8084?)
❌ Manual configuration changes needed
❌ No automatic health checking
❌ No load balancing
```

### Architecture With Eureka

```
                    ┌─────────────────────┐
                    │  EUREKA SERVER      │
                    │  :8761              │
                    │  ┌───────────────┐  │
                    │  │ Registry:     │  │
                    │  │ user-service  │  │
                    │  │ housing-svc   │  │
                    │  └───────────────┘  │
                    └─────────────────────┘
                         ↑           ↓
              Register   │           │  Discover
              Heartbeat  │           │
                         │           │
         ┌───────────────┴───┐   ┌──┴──────────────────┐
         │                   │   │                      │
    ┌────┴─────┐      ┌─────┴────┐         ┌──────────┴────┐
    │  user-   │      │ housing- │         │  api-gateway  │
    │ service  │      │ service  │         │               │
    │ :8081    │      │ :8082    │         │ Discovers &   │
    └──────────┘      └──────────┘         │ Calls via     │
                                            │ "lb://USER-   │
                                            │  SERVICE"     │
                                            └───────────────┘

Benefits:
✅ Auto-discovery (no hardcoded URLs)
✅ Scale easily (add more instances)
✅ Health checking (auto-remove dead services)
✅ Load balancing (built-in)
```

---

## Step-by-Step: Adding Eureka to Existing Services

### Step 1: Update user-service to register with Eureka

**Add dependency to `user-service/pom.xml`:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**Update `user-service/src/main/resources/application.yml`:**
```yaml
spring:
  application:
    name: user-service  # This is how Eureka identifies this service

eureka:
  client:
    serviceUrl:
      defaultZone: http://eureka-server:8761/eureka/
    registerWithEureka: true
    fetchRegistry: true
  instance:
    preferIpAddress: true
    instance-id: ${spring.application.name}:${random.value}
```

**Enable Eureka Client in `UserServiceApplication.java`:**
```java
@SpringBootApplication
@EnableDiscoveryClient  // Add this annotation
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

### Step 2: Update housing-service (same process)

**housing-service/pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**housing-service/application.yml:**
```yaml
spring:
  application:
    name: housing-service

eureka:
  client:
    serviceUrl:
      defaultZone: http://eureka-server:8761/eureka/
    registerWithEureka: true
    fetchRegistry: true
```

### Step 3: Update API Gateway to use Eureka

**api-gateway/application.yml - BEFORE:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-auth
          uri: ${USER_SERVICE_URL:http://localhost:8081}  # ❌ Hardcoded
          predicates:
            - Path=/api/auth/**
```

**api-gateway/application.yml - AFTER:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-auth
          uri: lb://user-service  # ✅ Load-balanced discovery
          predicates:
            - Path=/api/auth/**
      discovery:
        locator:
          enabled: true  # Enable automatic route creation from Eureka

eureka:
  client:
    serviceUrl:
      defaultZone: http://eureka-server:8761/eureka/
    registerWithEureka: true
    fetchRegistry: true
```

**The `lb://` prefix means:**
- `lb` = Load Balancer
- Gateway queries Eureka for all instances of `user-service`
- Automatically distributes requests using round-robin

### Step 4: Update docker-compose.yml

```yaml
version: '3.8'

services:
  # Add Eureka Server
  eureka-server:
    build: ./eureka-server
    container_name: campusnest-micro-eureka
    ports:
      - "8761:8761"
    networks:
      - campusnest-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  api-gateway:
    build: ./api-gateway
    container_name: campusnest-micro-gateway
    ports:
      - "8080:8080"
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      eureka-server:
        condition: service_healthy
    networks:
      - campusnest-network

  user-service:
    build: ./user-service
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      eureka-server:
        condition: service_healthy
    networks:
      - campusnest-network

  housing-service:
    build: ./housing-service
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      eureka-server:
        condition: service_healthy
    networks:
      - campusnest-network
```

### Step 5: Start Everything

```bash
docker-compose up -d --build
```

**Check Eureka Dashboard:**
Open browser: `http://localhost:8761`

You'll see all registered services:
```
Instances currently registered with Eureka
┌─────────────────┬──────────────┬────────────────────┐
│ Application     │ AMIs         │ Availability Zones │
├─────────────────┼──────────────┼────────────────────┤
│ USER-SERVICE    │ n/a (2)      │ (2)                │
│ HOUSING-SERVICE │ n/a (1)      │ (1)                │
│ API-GATEWAY     │ n/a (1)      │ (1)                │
└─────────────────┴──────────────┴────────────────────┘
```

---

## Part 2: Caching in Housing Service

### What is Caching?

Instead of querying the database every time, we **store frequently accessed data in memory** (Redis) for super-fast retrieval.

### Current Housing Service Performance

**Without Cache:**
```
User requests listing #123
    ↓
API Gateway → housing-service
    ↓
housing-service → MySQL query
    ↓
MySQL reads from disk (5-50ms)
    ↓
Return to user
Total: ~50ms
```

**With Cache:**
```
User requests listing #123
    ↓
API Gateway → housing-service
    ↓
housing-service → Check Redis first
    ↓
Redis returns from memory (1-2ms) ← 25x faster!
    ↓
Return to user
Total: ~5ms
```

### How Caching Works in Housing Service

#### Current Implementation

**File:** `housing-service/src/main/java/com/campusnest/housingservice/config/RedisConfig.java`

```java
@Configuration
@EnableCaching  // Enable Spring Cache abstraction
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Define cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Cache for housing listings (30 min TTL)
        cacheConfigurations.put("housing-listings",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))  // Expire after 30 min
                .serializeValuesWith(/* Jackson serialization */)
        );

        // Cache for search results (15 min TTL)
        cacheConfigurations.put("housing-search",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
```

#### Cache Annotations in Service Layer

**File:** `housing-service/services/HousingListingServiceImpl.java`

```java
@Service
public class HousingListingServiceImpl implements HousingListingService {

    // 1. @Cacheable - Store result in cache
    @Cacheable(value = "housing-listings", key = "#id")
    public Optional<HousingListing> findById(Long id) {
        log.info("Cache MISS - Fetching listing {} from database", id);
        return housingListingRepository.findById(id);
    }
    // First call: Queries DB, stores in Redis
    // Subsequent calls: Returns from Redis (no DB query)

    // 2. @CachePut - Update cache with new value
    @CachePut(value = "housing-listings", key = "#id")
    public HousingListing updateListing(Long id, HousingListing updated, String email) {
        log.info("Updating cache for listing {}", id);
        HousingListing saved = housingListingRepository.save(updated);
        return saved;  // This gets stored in cache
    }
    // Updates DB AND cache simultaneously

    // 3. @CacheEvict - Remove from cache
    @CacheEvict(value = {"housing-listings", "housing-search"}, allEntries = true)
    public void deleteListing(Long id, String email) {
        log.info("Evicting cache for listing {}", id);
        listing.setIsActive(false);
        housingListingRepository.save(listing);
    }
    // Clears cache so next request fetches fresh data
}
```

### Cache Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ Request: GET /api/housing/123                                │
└─────────────────────────────────────────────────────────────┘
                        ↓
         ┌──────────────────────────────┐
         │ @Cacheable("housing-listings")│
         │ Check Redis for key="123"    │
         └──────────────────────────────┘
                        ↓
                   ┌────────┐
                   │ Exists?│
                   └────────┘
                   /        \
              YES /          \ NO (Cache Miss)
                 /            \
                ↓              ↓
    ┌─────────────────┐  ┌──────────────────┐
    │ Return from     │  │ Query MySQL DB   │
    │ Redis (1-2ms)   │  │ (5-50ms)         │
    │                 │  │                  │
    │ Cache HIT! ✅   │  │ Store in Redis   │
    └─────────────────┘  │ Return to client │
                         └──────────────────┘
```

### Cache Invalidation Strategies

#### 1. **Time-To-Live (TTL)**
```java
// Automatically expire after 30 minutes
.entryTtl(Duration.ofMinutes(30))
```
**When to use:** Data that changes infrequently (listings)

#### 2. **Explicit Eviction**
```java
@CacheEvict(value = "housing-listings", key = "#id")
public void updateListing(Long id) {
    // Update DB
    // Cache automatically cleared
}
```
**When to use:** When data is updated/deleted

#### 3. **Cache Update**
```java
@CachePut(value = "housing-listings", key = "#result.id")
public HousingListing createListing(HousingListing listing) {
    return repository.save(listing);
}
```
**When to use:** Keep cache in sync with DB writes

### Cache Performance Metrics

**Current Performance (from your logs):**

```
Cache HIT Performance:
- Response time: 1-2ms
- No database query

Cache MISS Performance:
- Response time: 3-7ms (includes DB query)
- Database query: ~3-5ms
- Serialization: ~1ms

Speedup: 4-19x faster with cache hits
Cache Hit Rate: ~75% (excellent!)
```

### Redis Data Structure

**In Redis, cached data looks like:**
```redis
# Key format: cacheName::keyValue
housing-listings::123 → {
  "id": 123,
  "title": "Cozy 2BR Apartment",
  "price": 1200.00,
  "ownerEmail": "user@utah.edu",
  ...
}

# TTL countdown
TTL housing-listings::123 → 1789 seconds remaining
```

### Testing Cache

**Test cache performance:**
```bash
# File: test-cache-performance.sh

# First request (Cache MISS)
time curl http://localhost:8080/api/housing/1
# Response time: ~50ms

# Second request (Cache HIT)
time curl http://localhost:8080/api/housing/1
# Response time: ~5ms (10x faster!)

# Check Redis directly
docker exec campusnest-micro-redis redis-cli
> KEYS housing-listings::*
1) "housing-listings::1"
> GET "housing-listings::1"
{...json data...}
> TTL "housing-listings::1"
(integer) 1523
```

### Advanced Caching Strategies

#### 1. **Cache Warming (Preloading)**
```java
@PostConstruct
public void warmCache() {
    // Load popular listings into cache on startup
    List<HousingListing> popular = repository.findTop100ByOrderByViewsDesc();
    popular.forEach(listing -> {
        cacheManager.getCache("housing-listings").put(listing.getId(), listing);
    });
}
```

#### 2. **Conditional Caching**
```java
@Cacheable(value = "housing-listings",
           condition = "#id != null",
           unless = "#result == null")
public HousingListing findById(Long id) {
    // Only cache if ID is not null and result exists
}
```

#### 3. **Multi-Level Caching**
```
Level 1: In-Memory (Caffeine) - 1ms
   ↓ (miss)
Level 2: Redis - 2-3ms
   ↓ (miss)
Level 3: Database - 5-50ms
```

---

## Combining Eureka + Caching

**Full Architecture:**
```
┌─────────────────────────────────────────────────────────────┐
│                       Client Request                         │
└─────────────────────────────────────────────────────────────┘
                         ↓
              ┌─────────────────────┐
              │   API Gateway       │
              │ Queries Eureka for  │
              │ "housing-service"   │
              └─────────────────────┘
                         ↓
         ┌───────────────────────────────┐
         │    Eureka Server              │
         │ Returns: [                    │
         │   instance1: 172.18.0.5:8082  │
         │   instance2: 172.18.0.6:8082  │← Load balanced!
         │ ]                             │
         └───────────────────────────────┘
                         ↓
              ┌─────────────────────┐
              │  housing-service    │
              │  (instance 1)       │
              └─────────────────────┘
                         ↓
         ┌───────────────────────────────┐
         │  Check Redis Cache            │
         │  Key: "housing-listings::123" │
         └───────────────────────────────┘
                /              \
           HIT /                \ MISS
              ↓                  ↓
     ┌─────────────┐      ┌──────────┐
     │ Return from │      │  Query   │
     │ Redis (2ms) │      │  MySQL   │
     └─────────────┘      │  (20ms)  │
                          │  Cache it│
                          └──────────┘
```

**Benefits of Both:**
- ✅ **Eureka:** Auto-discovery, load balancing, health checks
- ✅ **Caching:** Fast responses, reduced DB load
- ✅ **Combined:** Scalable AND fast microservices

---

## Summary

### Eureka:
- **What:** Service registry for microservice discovery
- **Why:** No hardcoded URLs, auto-scaling, health checking
- **How:** Services register on startup, query for others via `lb://service-name`

### Caching:
- **What:** Store frequently accessed data in Redis
- **Why:** 10-25x faster responses, reduced database load
- **How:** `@Cacheable`, `@CachePut`, `@CacheEvict` annotations

**Both together = Scalable, fast, resilient microservices! 🚀**