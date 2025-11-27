# CampusNest Microservices - Quick Start Guide

## Prerequisites

- **Docker Desktop** (includes Docker Compose)
  - Download: https://www.docker.com/products/docker-desktop
- **Gmail App Password** (for email verification)
  - Get it: https://myaccount.google.com/apppasswords

---

## 🚀 Quick Start (3 Steps)

### Step 1: Configure Environment

```bash
# Navigate to the project
cd /Users/xiemingda/Downloads/campusnest-microservices

# Copy the example env file
cp .env.example .env

# Edit .env and add your email credentials
nano .env  # or use any text editor
```

**Required changes in `.env`:**
```bash
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password
```

### Step 2: Start Everything

```bash
# Build and start all services
docker-compose up --build
```

**What this does:**
- ✅ Builds user-service Docker image
- ✅ Starts MySQL database
- ✅ Starts Redis cache
- ✅ Starts user-service
- ✅ Creates database schema automatically

**Wait for this message:**
```
user-service | Started UserServiceApplication in X seconds
```

### Step 3: Test the Service

**Register a new user:**
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@stanford.edu",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Expected response:**
```json
{
  "success": true,
  "message": "Registration successful! Please check your university email to verify your account.",
  "user": {
    "id": "1",
    "email": "j***@stanford.edu",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

**Check your email** - You'll receive a verification email!

---

## 📋 Common Operations

### View Logs
```bash
# All services
docker-compose logs -f

# Just user-service
docker-compose logs -f user-service

# Just database
docker-compose logs -f mysql
```

### Stop Services
```bash
# Stop but keep data
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop and remove everything (including database)
docker-compose down -v
```

### Restart Services
```bash
# Restart everything
docker-compose restart

# Restart just user-service
docker-compose restart user-service
```

### Check Health
```bash
# User service health
curl http://localhost:8081/actuator/health

# MySQL health
docker-compose exec mysql mysqladmin ping -h localhost -u root -prootpassword
```

---

## 🧪 Testing All Endpoints

### 1. Register User
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@mit.edu",
    "password": "SecurePass123!",
    "firstName": "Jane",
    "lastName": "Smith"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@mit.edu",
    "password": "SecurePass123!"
  }'
```

**Save the `access_token` from the response!**

### 3. Get User Profile (requires token)
```bash
curl -X GET http://localhost:8081/api/user/profile \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
```

### 4. Forgot Password
```bash
curl -X POST http://localhost:8081/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@mit.edu"
  }'
```

---

## 🐛 Troubleshooting

### Problem: "Connection refused" to MySQL

**Solution:** Wait 30 seconds for MySQL to fully start
```bash
# Check MySQL is ready
docker-compose logs mysql | grep "ready for connections"
```

### Problem: "Email sending failed"

**Solution:** Check your Gmail app password
```bash
# Verify email settings in .env
cat .env | grep MAIL_

# Test email config
docker-compose logs user-service | grep "mail"
```

### Problem: Port already in use

**Solution:** Stop conflicting services
```bash
# Find what's using port 8081
lsof -i :8081

# Or change ports in docker-compose.yml
# Change "8081:8081" to "8082:8081"
```

### Problem: Database changes not showing

**Solution:** Rebuild containers
```bash
docker-compose down -v  # Remove volumes
docker-compose up --build
```

---

## 📊 Monitoring & Management

### Access MySQL Database
```bash
# Connect to MySQL shell
docker-compose exec mysql mysql -u campusnest -pcampusnest123 campusNest_users

# View users table
mysql> SELECT id, email, first_name, last_name, email_verified FROM users;
```

### Access Redis
```bash
# Connect to Redis
docker-compose exec redis redis-cli

# Check keys
redis> KEYS *
```

### View Container Stats
```bash
# Resource usage
docker stats
```

---

## 🎯 What's Running?

| Service | Port | URL | Purpose |
|---------|------|-----|---------|
| user-service | 8081 | http://localhost:8081 | User authentication API |
| MySQL | 3306 | localhost:3306 | Database |
| Redis | 6379 | localhost:6379 | Cache (future) |

### Health Check Endpoints
- User Service: http://localhost:8081/actuator/health
- MySQL: Port 3306 (check with `docker-compose ps`)
- Redis: Port 6379 (check with `docker-compose ps`)

---

## 🚦 Service Status

Check all services are healthy:
```bash
docker-compose ps
```

Expected output:
```
NAME                      STATUS         PORTS
campusnest-mysql          Up (healthy)   0.0.0.0:3306->3306/tcp
campusnest-redis          Up (healthy)   0.0.0.0:6379->6379/tcp
campusnest-user-service   Up (healthy)   0.0.0.0:8081->8081/tcp
```

---

## 📝 Notes

- **First startup** takes 2-3 minutes (building + database init)
- **Subsequent startups** take ~30 seconds
- **Database persists** between restarts (uses Docker volumes)
- **Code changes** require rebuild: `docker-compose up --build`

---

## 🎓 For Interviews

**"How do you run this?"**
> "Just `docker-compose up` - everything is containerized. MySQL, Redis, and the microservice all start together with proper health checks and dependency ordering."

**"How do you ensure services start in order?"**
> "I use Docker Compose's `depends_on` with health checks. User-service waits for MySQL to be healthy before starting, preventing connection errors."

**"What about configuration?"**
> "Environment-based configuration via `.env` file. Database credentials, JWT secrets, and email settings are all externalized and never hardcoded."

---

Need help? Check the main [README.md](./README.md) for architecture details.