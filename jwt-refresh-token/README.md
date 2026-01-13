# JWT Refresh Token Authentication Module

A production-ready Spring Boot JWT authentication implementation with **refresh token rotation**, **HttpOnly cookies**, and **sliding window expiration**.

This module demonstrates advanced JWT authentication with:
- Access token (short-lived, 15 minutes)
- Refresh token (long-lived, 30 days sliding, 90 days absolute)
- **Token rotation** (security best practice)
- **HttpOnly cookies** (XSS protection)
- **Token blacklisting** (immediate logout)
- **Sliding window + absolute expiry** (balance security & UX)
- **Automated cleanup** (scheduled task)
- Clean, well-documented code

---

## Purpose

This is **NOT** a basic JWT module. It's an **advanced production-grade** implementation that shows:
- How to implement secure refresh token rotation
- How to prevent token replay attacks
- How to balance security with user experience
- Real-world configuration patterns for high-traffic applications

Use this as a **reference** for building secure, scalable authentication systems.

---

## Why Refresh Tokens?

### Problem with Access Tokens Only

```
Access Token Lifetime: 15 minutes
User Experience: Login every 15 minutes 
Security: Short-lived tokens 
```

### Solution: Refresh Token Pattern

```
Access Token: 15 minutes (API calls)
Refresh Token: 30 days (get new access token)
User Experience: Stay logged in for 30 days 
Security: Short-lived access tokens 
```

---

## Key Security Features

### 1. Token Rotation
```
Old Refresh Token → Validate → Generate New → Revoke Old
```
**Why?** Stolen tokens become useless after one use.

### 2. HttpOnly Cookies
```
Refresh Token → Stored in HttpOnly Cookie
JavaScript: Cannot access 
XSS Attacks: Cannot steal token 
```

### 3. Token Hashing
```
Plain JWT → SHA-256 Hash → Store in Database
Database Breach: Tokens can't be used 
```

### 4. Sliding Window Expiry
```
Token Created: Jan 1 → Expires Jan 31 (30 days)
Token Used: Jan 15 → Expires Feb 14 (30 days from use)
Active Users: Stay logged in 
Inactive Users: Token expires 
```

### 5. Absolute Max Expiry
```
Token Created: Jan 1 → Max Expiry Apr 1 (90 days)
Daily Usage: Token still dies on Apr 1
Force Re-authentication: Every 90 days 
```

### 6. Token Blacklisting
```
Logout → Blacklist Access Token (Redis)
Immediate Invalidation: Token can't be reused 
```

---

## Tech Stack

- **Spring Boot:** 4.0.1
- **Java:** 21
- **Database:** PostgreSQL 16
- **Cache:** Redis (token blacklist)
- **Security:** Spring Security + JWT (JJWT 0.12.5)
- **Password Hashing:** BCrypt
- **Token Hashing:** SHA-256
- **Build Tool:** Maven

---

## Prerequisites

- Java 17+ installed
- Maven installed
- PostgreSQL running
- Redis running (for token blacklist)
- Postman or cURL for testing

---

## Quick Start

### 1. Clone and Build

```bash
git clone https://github.com/fayupable/spring-infrastructure-modules.git
cd spring-infrastructure-modules/jwt-refresh-token
mvn clean install
```

### 2. Start PostgreSQL

**Option A: Docker**
```bash
docker run --name jwt-postgres \
  -e POSTGRES_USER=jwtuser \
  -e POSTGRES_PASSWORD=jwtpass123 \
  -e POSTGRES_DB=jwt_refresh_db \
  -p 5432:5432 \
  -d postgres:16-alpine
```

**Option B: Use existing PostgreSQL**
```sql
CREATE DATABASE jwt_refresh_db;
CREATE USER jwtuser WITH PASSWORD 'jwtpass123';
GRANT ALL PRIVILEGES ON DATABASE jwt_refresh_db TO jwtuser;
```

### 3. Start Redis

**Option A: Docker**
```bash
docker run --name jwt-redis \
  -p 6379:6379 \
  -d redis:7-alpine
```

**Option B: Use existing Redis**
```bash
redis-server
```

### 4. Run Application

```bash
mvn spring-boot:run
```

Application starts at: `http://localhost:8080`

---

## API Endpoints

### Public Endpoints (No Authentication Required)

#### 1. Register New User

**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "username": "johndoe"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "username": "johndoe"
  }'
```

**Response (201 Created):**
```json
{
  "message": "User registered successfully"
}
```

---

#### 2. Login (Get Tokens)

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "user@demo.com",
  "password": "user123"
}
```


**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@demo.com",
    "password": "user123"
  }' \
  -c cookies.txt
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 900000,
  "email": "user@demo.com",
  "roles": ["ROLE_USER"]
}
```
**Postman Screenshot:**
![Login User](docs/images/01-login-user.png)

**Set-Cookie Header:**
```
Set-Cookie: refresh_token=eyJhbGc...; 
  Path=/; 
  Max-Age=2592000; 
  HttpOnly; 
  Secure; 
  SameSite=Strict
```

**What Happens:**
1. User credentials verified 
2. Access token generated (15 min) 
3. Refresh token generated (30 days) 
4. Refresh token **hashed** and stored in database 
5. Refresh token sent in **HttpOnly cookie** 

---

#### 3. Refresh Access Token

**Endpoint:** `POST /api/auth/refresh`

**Headers:**
```
Cookie: refresh_token=eyJhbGc...
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -b cookies.txt \
  -c cookies.txt
```

**Response (200 OK):**
```json
{
  "accessToken": "NEW_ACCESS_TOKEN_HERE",
  "refreshToken": "NEW_REFRESH_TOKEN_HERE",
  "type": "Bearer",
  "expiresIn": 900000,
  "success": true,
  "message": "Token refreshed successfully"
}
```

**Postman Screenshot:**
![Refresh Token](docs/images/04-refresh-token.png)

**What Happens (Token Rotation):**
1. Old refresh token validated 
2. **New** access token generated
3. **New** refresh token generated 
4. **Old** refresh token **revoked** 
5. New refresh token sent in cookie 

**Security:** Old token can't be reused (replay attack prevention)

---

### Protected Endpoints (Access Token Required)

#### 4. Test Authenticated Endpoint

**Endpoint:** `GET /api/test/hello`

**Headers:**
```
Authorization: Bearer YOUR_ACCESS_TOKEN_HERE
```

**cURL:**
```bash
curl -X GET http://localhost:8080/api/test/hello \
  -H "Authorization: Bearer eyJhbGc..."
```

**Response (200 OK):**
```
Hello, user@demo.com! Your JWT token is working. Roles: [ROLE_USER]
```

---

#### 5. Logout (Revoke Tokens)

**Endpoint:** `POST /api/auth/logout`

**Headers:**
```
Authorization: Bearer YOUR_ACCESS_TOKEN_HERE
Cookie: refresh_token=eyJhbGc...
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer eyJhbGc..." \
  -b cookies.txt
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Logged out successfully",
  "shouldClearTokens": true
}
```

**Postman Screenshot:**
![Logout](docs/images/02-logout-user.png)

**Revoked Tokens Screenshot:**
![Revoked Tokens](docs/images/05-revoked-token.png)

**Set-Cookie Header (Clear Cookie):**
```
Set-Cookie: refresh_token=; 
  Path=/; 
  Max-Age=0; 
  HttpOnly; 
  Secure; 
  SameSite=Strict
```

**What Happens:**
1. Refresh token **revoked** in database 
2. Access token **blacklisted** in Redis (15 min TTL) 
3. Refresh token cookie **cleared** 

---

## Token Lifecycle

### Complete Authentication Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. LOGIN                                                    │
├─────────────────────────────────────────────────────────────┤
│ POST /api/auth/login                                        │
│   ↓                                                         │
│ Verify Credentials (BCrypt)                                │
│   ↓                                                         │
│ Generate Access Token (15 min)                             │
│ Generate Refresh Token (30 days)                           │
│   ↓                                                         │
│ Hash Refresh Token (SHA-256)                               │
│ Store in Database                                          │
│   ↓                                                         │
│ Return: Access Token + Refresh Token (HttpOnly Cookie)     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 2. API CALLS (Next 15 minutes)                             │
├─────────────────────────────────────────────────────────────┤
│ GET /api/test/hello                                         │
│ Header: Authorization: Bearer ACCESS_TOKEN                  │
│   ↓                                                         │
│ Filter: Extract & Validate Access Token                    │
│   ↓                                                         │
│ Check: Token Blacklisted? (Redis)                          │
│   ↓                                                         │
│ Check: Token Expired? (JWT validation)                     │
│   ↓                                                         │
│ Load User from Database                                    │
│   ↓                                                         │
│ Check: Account Enabled? Account Locked?                    │
│   ↓                                                         │
│ Set SecurityContext                                        │
│   ↓                                                         │
│ Continue to Controller                                     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 3. ACCESS TOKEN EXPIRES (After 15 minutes)                 │
├─────────────────────────────────────────────────────────────┤
│ POST /api/auth/refresh                                      │
│ Cookie: refresh_token=REFRESH_TOKEN                         │
│   ↓                                                         │
│ Extract Refresh Token from Cookie                          │
│   ↓                                                         │
│ Hash Token & Find in Database                              │
│   ↓                                                         │
│ Validate: Not Revoked? Not Expired?                        │
│   ↓                                                         │
│ Generate NEW Access Token                                  │
│ Generate NEW Refresh Token                                 │
│   ↓                                                         │
│ REVOKE OLD Refresh Token (set revoked=true)                │
│ SAVE NEW Refresh Token Hash                                │
│   ↓                                                         │
│ Return: New Tokens + New Cookie                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 4. LOGOUT                                                   │
├─────────────────────────────────────────────────────────────┤
│ POST /api/auth/logout                                       │
│ Header: Authorization: Bearer ACCESS_TOKEN                  │
│ Cookie: refresh_token=REFRESH_TOKEN                         │
│   ↓                                                         │
│ Revoke Refresh Token in Database                           │
│   ↓                                                         │
│ Blacklist Access Token in Redis (15 min TTL)               │
│   ↓                                                         │
│ Clear Refresh Token Cookie (Max-Age=0)                     │
│   ↓                                                         │
│ Return: Success + shouldClearTokens=true                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 5. CLEANUP (Every 30 minutes)                              │
├─────────────────────────────────────────────────────────────┤
│ @Scheduled Task                                             │
│   ↓                                                         │
│ Find Expired Tokens:                                       │
│   - expiresAt < now (sliding window)                       │
│   - maxExpiry < now (absolute limit)                       │
│   - revoked=true AND updatedAt < 12 hours ago              │
│   ↓                                                         │
│ Delete in Batches (100 at a time)                          │
│   ↓                                                         │
│ Log: "Deleted X tokens"                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Token Expiry Strategy

### Sliding Window Expiry

```
Token Created:  Jan 1, 00:00  →  Expires: Jan 31, 00:00 (30 days)
Token Used:     Jan 15, 12:00 →  Expires: Feb 14, 12:00 (30 days from use)
Token Used:     Feb 10, 08:00 →  Expires: Mar 12, 08:00 (30 days from use)
```

**Benefit:** Active users stay logged in

### Absolute Max Expiry

```
Token Created:  Jan 1  →  maxExpiry: Apr 1 (90 days, NEVER CHANGES)
Token Used:     Feb 15 →  maxExpiry: STILL Apr 1
Token Used:     Mar 20 →  maxExpiry: STILL Apr 1
Apr 1:          Token DIES regardless of usage
```

**Benefit:** Force re-authentication every 90 days (security/compliance)

### Combined Example

```
Day 1:   Login  → expiresAt: Day 31, maxExpiry: Day 90
Day 15:  Refresh → expiresAt: Day 45, maxExpiry: Day 90 (unchanged)
Day 40:  Refresh → expiresAt: Day 70, maxExpiry: Day 90 (unchanged)
Day 85:  Refresh → expiresAt: Day 115, maxExpiry: Day 90 (unchanged)
Day 90:  TOKEN DIES (maxExpiry reached)
```

---

## Security Deep Dive

### 1. Token Rotation (Replay Attack Prevention)

**Without Rotation:**
```
Attacker steals token → Uses forever
```

**With Rotation:**
```
User:     Use token A → Get token B (A revoked)
Attacker: Use token A → REJECTED (already revoked) 
```

**Implementation:**
```
Old Token → Validate → Generate New Token → Revoke Old → Link Tokens
```

### 2. Token Family Tracking

```
Token A (replaced by B)
  ↓
Token B (replaced by C)
  ↓
Token C (active)
```

**If Token A is reused:**
```
Security Breach Detected!
→ Revoke ENTIRE family (A, B, C)
→ Force user to re-login
```

### 3. Token Hashing (Database Protection)

```
Plain Token:  eyJhbGc... (1024 chars)
     ↓ SHA-256
Hash:         a3f5... (64 chars)
     ↓ Store
Database:     a3f5... (can't reverse to plain token)
```

**If database compromised:**
-  Attacker has hashes
-  **Cannot** use hashes as tokens
-  **Cannot** reverse hashes to get tokens

### 4. HttpOnly Cookies (XSS Protection)

```javascript
// JavaScript CANNOT access HttpOnly cookies
document.cookie  //  refresh_token not visible
localStorage.getItem('refreshToken')  //  Not here either
```

**XSS Attack Scenario:**
```
Attacker injects: <script>steal(document.cookie)</script>
Browser: refresh_token is HttpOnly → NOT accessible 
```

### 5. Token Blacklisting (Immediate Logout)

**Problem:** Access tokens are stateless
```
User logs out at 10:00
Token expires at 10:15
Token STILL VALID for 15 minutes 
```

**Solution:** Blacklist in Redis
```
Logout → Add token to Redis (TTL: 15 min)
API Call → Check Redis first
If blacklisted → REJECT 
```

---

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    user_status VARCHAR(50) NOT NULL,
    account_non_locked BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Refresh Tokens Table
```sql
CREATE TABLE refresh_tokens (
    refresh_token_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    max_expiry TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT false,
    replaced_by UUID REFERENCES refresh_tokens(refresh_token_id),
    device_info VARCHAR(500),
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_user_id_revoked ON refresh_tokens(user_id, revoked);
CREATE INDEX idx_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_max_expiry ON refresh_tokens(max_expiry);
```

---

## Configuration

### application.yml

```yaml
spring:
  application:
    name: jwt-refresh-token
    
  datasource:
    url: jdbc:postgresql://localhost:5432/jwt_refresh_db
    username: jwtuser
    password: jwtpass123
    
  data:
    redis:
      host: localhost
      port: 6379
      
auth:
  token:
    # JWT Secret (generate with: openssl rand -base64 32)
    jwtSecret: ${JWT_SECRET:your-secret-key-here}
    
    # Access token expiration (15 minutes)
    expirationInMils: 900000
    
    # Refresh token sliding window expiration (30 days)
    refreshExpirationInMils: 2592000000
    
    # Refresh token absolute max lifetime (90 days)
    maxAbsoluteLifetimeInMils: 7776000000
    
    # Token cleanup configuration
    cleanup:
      # How long to keep revoked tokens before deletion
      retentionHours: 12
      
      # Cleanup interval (for documentation)
      # Actual interval set in @Scheduled cron
      intervalMinutes: 30
      
cookie:
  # Cookie settings
  httpOnly: true
  secure: ${COOKIE_SECURE:false}  # true in production
  sameSite: Strict
  path: /
  domain: ${COOKIE_DOMAIN:}  # Set for production
```

---

## Production Deployment

### Environment Variables

```bash
# Required
export JWT_SECRET="your-generated-secret-key-here"
export DATABASE_URL="jdbc:postgresql://db-host:5432/jwt_db"
export DATABASE_USERNAME="jwtuser"
export DATABASE_PASSWORD="secure-password"
export REDIS_HOST="redis-host"
export REDIS_PORT="6379"

# Recommended
export COOKIE_SECURE="true"
export COOKIE_DOMAIN="yourdomain.com"
```

### Generate JWT Secret

```bash
openssl rand -base64 32
```

### Docker Compose (Production)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: jwt_refresh_db
      POSTGRES_USER: jwtuser
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      
  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
      
  app:
    build: .
    environment:
      JWT_SECRET: ${JWT_SECRET}
      DATABASE_URL: jdbc:postgresql://postgres:5432/jwt_refresh_db
      DATABASE_USERNAME: jwtuser
      DATABASE_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      COOKIE_SECURE: "true"
      COOKIE_DOMAIN: "yourdomain.com"
    depends_on:
      - postgres
      - redis
    ports:
      - "8080:8080"

volumes:
  postgres_data:
  redis_data:
```

---

## Monitoring & Metrics

### Key Metrics to Track

1. **Token Creation Rate**
    - Logins per minute
    - New refresh tokens created

2. **Token Refresh Rate**
    - Refresh requests per minute
    - Success vs failure rate

3. **Token Cleanup**
    - Tokens deleted per run
    - Cleanup duration
    - Retention hours

4. **Security Events**
    - Token reuse attempts (replay attacks)
    - Blacklisted token usage attempts
    - Failed login attempts

### Example Log Output

```
2025-01-11 10:00:00 INFO  - Refresh token created for user: abc-123 from IP: 192.168.1.1
2025-01-11 10:15:00 INFO  - Token rotated for user: abc-123. Old token revoked, new JWT hash saved
2025-01-11 10:30:00 INFO  - Starting scheduled cleanup (retention: 12 hours, interval: 30 minutes)
2025-01-11 10:30:05 INFO  - Cleanup completed. Deleted 42 tokens (retention: 12 hours, interval: 30 minutes)
2025-01-11 10:45:00 WARN  - TOKEN REUSE DETECTED! User: abc-123
2025-01-11 10:45:00 INFO  - All tokens revoked for user: abc-123 (security breach)
```

---

## Testing Guide

### 1. Test Login Flow

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test123","username":"testuser"}'

# Login (save cookies)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test123"}' \
  -c cookies.txt

# Extract access token from response and save
export ACCESS_TOKEN="eyJhbGc..."
```

### 2. Test Protected Endpoint

```bash
curl -X GET http://localhost:8080/api/test/hello \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 3. Test Token Refresh

```bash
# Wait for access token to expire (or use short expiry for testing)
# Refresh token
curl -X POST http://localhost:8080/api/auth/refresh \
  -b cookies.txt \
  -c cookies.txt

# Extract NEW access token and test
curl -X GET http://localhost:8080/api/test/hello \
  -H "Authorization: Bearer $NEW_ACCESS_TOKEN"
```

### 4. Test Logout

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -b cookies.txt

# Try to use old access token (should fail)
curl -X GET http://localhost:8080/api/test/hello \
  -H "Authorization: Bearer $ACCESS_TOKEN"
# Expected: 401 Unauthorized (token blacklisted)

# Try to refresh with old refresh token (should fail)
curl -X POST http://localhost:8080/api/auth/refresh \
  -b cookies.txt
# Expected: 401 Unauthorized (token revoked)
```

### 5. Test Token Replay Attack

```bash
# Login and get tokens
curl -X POST http://localhost:8080/api/auth/login \
  -d '{"email":"test@test.com","password":"test123"}' \
  -c cookies_original.txt

# Refresh once (rotates token)
curl -X POST http://localhost:8080/api/auth/refresh \
  -b cookies_original.txt \
  -c cookies_new.txt

# Try to use ORIGINAL token again (REPLAY ATTACK)
curl -X POST http://localhost:8080/api/auth/refresh \
  -b cookies_original.txt

# Expected: 401 Unauthorized + "Security breach detected: Token reuse"
# All user tokens revoked
```

---

## Troubleshooting

### Issue 1: Cookie Not Set

**Symptom:** Refresh token not in cookies

**Possible Causes:**
- HTTPS required but using HTTP
- Domain mismatch
- SameSite policy blocking

**Solution:**
```yaml
# For local testing
cookie_local:
  secure: false
  sameSite: Lax
  domain: ""

# For production
cookie_production:
  secure: true
  sameSite: Strict
  domain: "yourdomain.com"
```

### Issue 2: Token Always Expired

**Symptom:** "Token expired" immediately after login

**Possible Causes:**
- Server time mismatch
- Clock skew

**Solution:**
```bash
# Sync server time
sudo ntpdate -s time.nist.gov
```

### Issue 3: Redis Connection Failed

**Symptom:** "Unable to connect to Redis"

**Solution:**
```bash
# Check Redis is running
redis-cli ping
# Should return: PONG

# Check connection
redis-cli -h localhost -p 6379
```

### Issue 4: Cleanup Not Running

**Symptom:** Revoked tokens not deleted

**Possible Causes:**
- `@EnableScheduling` missing
- Cron expression wrong

**Solution:**
```java
@SpringBootApplication
@EnableScheduling  // ← Add this
public class JwtRefreshTokenApplication {
    // ...
}
```

---

## Performance Optimization

### 1. Database Indexes

**Already Configured:**
```sql
CREATE INDEX idx_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_user_id_revoked ON refresh_tokens(user_id, revoked);
CREATE INDEX idx_expires_at ON refresh_tokens(expires_at);
```

### 2. Redis Caching

**Token blacklist uses Redis:**
- Fast O(1) lookup
- Automatic TTL expiration
- No database overhead

### 3. Batch Cleanup

**Processes tokens in batches:**
```java
batchSize = 100  // Process 100 tokens at a time
```

### 4. Connection Pooling

**HikariCP (default):**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

---

## Advanced Features

### 1. Multiple Device Support

**Tracked per token:**
```java
RefreshToken {
    deviceInfo: "Mozilla/5.0..."
    ipAddress: "192.168.1.1"
}
```

**View all sessions:**
```sql
SELECT device_info, ip_address, issued_at 
FROM refresh_tokens 
WHERE user_id = ? AND revoked = false;
```

### 2. Logout from All Devices

```bash
curl -X POST http://localhost:8080/api/auth/logout-all \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Implementation:**
```java
@Transactional
public void revokeAllUserTokens(UUID userId) {
    refreshTokenRepository.revokeAllUserTokens(userId);
}
```

### 3. Token Audit Trail

**Track token usage:**
```
Token A → replaced by → Token B → replaced by → Token C
```

**If Token A is reused:**
```java
if (dbToken.getReplacedBy() != null) {
    // Security breach!
    revokeAllUserTokens(userId);
    throw new TokenReusedException();
}
```

---

## Comparison with JWT Basic

| Feature | JWT Basic  | JWT Refresh Token |
|---------|------------|------------|
| **Access Token** | +          | -          |
| **Refresh Token** | -          | +          |
| **Token Rotation** | -          | +           |
| **HttpOnly Cookies** | -          | +           |
| **Token Blacklist** | -          | +           |
| **Sliding Expiry** | -          | +           |
| **Absolute Expiry** | -          | +           |
| **Replay Protection** | -          | +           |
| **Database Storage** | Users only | Users + Tokens |
| **Redis Required** | -          | +           |
| **Complexity** | Low        | Medium     |
| **Security Level** | Basic      | Advanced   |
| **Production Ready** | Demo       | Yes        |

---

## Migration from JWT Basic

### Step 1: Add Dependencies

```xml
<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Step 2: Update Configuration

```yaml
# Add refresh token settings
auth:
  token:
    refreshExpirationInMils: 2592000000
    maxAbsoluteLifetimeInMils: 7776000000
    cleanup:
      retentionHours: 12
      intervalMinutes: 30
```

### Step 3: Update Login Response

```java
// Before: Return only access token
return new AuthResponse(accessToken, "Bearer", email, roles);

// After: Return access + refresh token + cookie
String refreshToken = jwtUtils.generateRefreshToken(email, userId);
refreshTokenService.createRefreshToken(userId, refreshToken, userAgent, ip);
ResponseCookie cookie = refreshTokenService.createRefreshTokenCookie(refreshToken);
return ResponseEntity.ok()
    .header(HttpHeaders.SET_COOKIE, cookie.toString())
    .body(new AuthResponse(accessToken, refreshToken, ...));
```

### Step 4: Add Refresh Endpoint

```java
@PostMapping("/refresh")
public ResponseEntity<RefreshTokenResponse> refresh(
    @CookieValue(name = "refresh_token")
String refreshToken,
    HttpServletRequest request) {
    // ...
}
```

---

## Production Checklist

Before deploying:

- [ ] **Security**
    - [ ] Generate unique JWT secret
    - [ ] Enable HTTPS (cookie.secure=true)
    - [ ] Set proper cookie domain
    - [ ] Configure CORS properly
    - [ ] Set appropriate token expiration times

- [ ] **Database**
    - [ ] Create database indexes
    - [ ] Set up database backups
    - [ ] Configure connection pooling

- [ ] **Redis**
    - [ ] Enable Redis persistence (AOF/RDB)
    - [ ] Set up Redis password
    - [ ] Configure Redis maxmemory policy

- [ ] **Monitoring**
    - [ ] Set up logging (ELK/CloudWatch)
    - [ ] Configure metrics (Prometheus/Grafana)
    - [ ] Add health check endpoints
    - [ ] Set up alerts for security events

- [ ] **Testing**
    - [ ] Load test token refresh flow
    - [ ] Test token rotation under load
    - [ ] Verify cleanup task performance

- [ ] **Deployment**
    - [ ] Use environment variables for secrets
    - [ ] Disable demo users
    - [ ] Set up CI/CD pipeline
    - [ ] Configure auto-scaling

---

## Further Reading

### Security Best Practices
- [OWASP JWT Security](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [RFC 6749: OAuth 2.0](https://datatracker.ietf.org/doc/html/rfc6749)
- [Token Rotation Pattern](https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation)

### Spring Security
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)

### Related Modules
- `jwt-basic` - Basic JWT authentication (simpler version)
- `oauth2-social-login` - OAuth2 with Google/GitHub (coming soon)

---

## Contributing

Issues and PRs welcome!

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## Author

**Fayupable**

Part of the Spring Infrastructure Modules collection.

GitHub: [@fayupable](https://github.com/fayupable)

---

## Changelog

### v1.0.0 (2025-01-11)
- Initial release
- Refresh token rotation
- HttpOnly cookie support
- Token blacklisting (Redis)
- Sliding window + absolute expiry
- Automated cleanup task
- Production-ready implementation

---

## Support

Questions? Issues?

- Email: support@fayupable.io
- Issues: [GitHub Issues](https://github.com/fayupable/spring-infrastructure-modules/issues)
- Docs: [Full Documentation](https://docs.fayupable.io)

