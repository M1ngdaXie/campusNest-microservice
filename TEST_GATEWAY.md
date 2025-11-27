# API Gateway Testing Guide

## Architecture
```
Client → API Gateway (8080) → User Service (8081) → MySQL
```

All requests now go through the gateway on port **8080** instead of directly to services.

---

## Start the Services

```bash
cd /Users/xiemingda/Downloads/campusnest-microservices
docker-compose up --build
```

Wait for all 3 services to be healthy:
- ✅ `campusnest-micro-gateway` (API Gateway)
- ✅ `campusnest-micro-user-service` (User Service)
- ✅ `campusnest-micro-mysql` (Database)

---

## Test 1: Health Check (Public)

```bash
curl http://localhost:8080/actuator/health
```

**Expected:** `{"status":"UP"}`

---

## Test 2: Register User (Public - No JWT Required)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@stanford.edu",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Expected:** Success message with verification email sent

---

## Test 3: Login (Public - No JWT Required)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@stanford.edu",
    "password": "SecurePass123!"
  }'
```

**Expected:** Response with `access_token` and `refresh_token`

**Save the access_token for next tests!**

---

## Test 4: Get User Profile (Protected - JWT Required)

Replace `YOUR_ACCESS_TOKEN` with the token from login:

```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Expected:** User profile data

**If no token or invalid token:**
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

---

## Test 5: Access Admin Endpoint (Protected - Admin Role Required)

```bash
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Expected:**
- If STUDENT role: `403 Forbidden`
- If ADMIN role: List of users

---

## How It Works

### Public Endpoints (No JWT Required)
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh-token`
- `GET /api/auth/verify-email`
- `POST /api/auth/resend-verification`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `GET /actuator/health`

### Protected Endpoints (JWT Required)
- All other endpoints require `Authorization: Bearer <token>` header
- Gateway validates JWT before forwarding to services
- Gateway adds user info to request headers:
  - `X-User-Id`: User ID from JWT
  - `X-User-Email`: User email
  - `X-User-Role`: User role (STUDENT/ADMIN)

---

## Architecture Benefits

### Before (Without Gateway):
```
Client → User Service (8081)
- Each service handles its own auth
- CORS configured per service
- No centralized security
- Client needs to know all service URLs
```

### After (With Gateway):
```
Client → API Gateway (8080) → User Service (8081)
- Centralized authentication
- Centralized CORS
- Single entry point
- Client only knows gateway URL
- Rate limiting possible
- Request logging
- Load balancing ready
```

---

## Troubleshooting

### Gateway not starting
```bash
# Check logs
docker-compose logs api-gateway

# Common issues:
# - Port 8080 already in use
# - User service not healthy
# - JWT secret mismatch
```

### 401 Unauthorized on protected endpoints
```bash
# Check if token is included
curl -v http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN"

# Look for:
# > Authorization: Bearer eyJhbGc...
```

### 502 Bad Gateway
```bash
# User service is down or unhealthy
docker ps  # Check if user-service is running
docker-compose logs user-service
```

---

## What Changed?

### Port Changes:
- **OLD:** User Service directly on `8080`
- **NEW:** API Gateway on `8080`, User Service on `8081`

### Request Flow:
- **OLD:** `Client → localhost:8080/api/auth/login`
- **NEW:** `Client → localhost:8080/api/auth/login → Gateway → localhost:8081/api/auth/login`

### Headers Added by Gateway:
The gateway automatically adds these headers to forwarded requests:
```
X-User-Id: 1
X-User-Email: test@stanford.edu
X-User-Role: STUDENT
```

Your services can now use these headers instead of parsing JWT again!

---

## View Gateway Routes

```bash
curl http://localhost:8080/actuator/gateway/routes
```

Shows all configured routes and their status.

---

## Next Steps

1. Update your frontend to use port **8080** (gateway) instead of **8081**
2. Add rate limiting to gateway
3. Add request logging
4. Add circuit breaker for resilience
5. Extract Housing Service and add routes to gateway

---

**Resume Talking Point:**
> "I implemented an API Gateway using Spring Cloud Gateway for centralized routing, authentication, and CORS handling. The gateway validates JWT tokens and adds user context headers before forwarding requests to downstream microservices, providing a single entry point for all client requests."