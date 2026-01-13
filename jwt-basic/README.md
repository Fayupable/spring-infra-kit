

# JWT Basic Authentication Module

A production-ready Spring Boot JWT authentication implementation with minimal dependencies and maximum clarity.

This module demonstrates stateless JWT authentication with:
- User registration and login
- BCrypt password hashing
- JWT token generation and validation
- Role-based access control
- Clean, well-documented code

---

## Purpose

This is **NOT** a full-featured application. It's a reference implementation that shows:
- How to set up JWT authentication from scratch
- How different components work together
- Real-world configuration patterns

Use this as a **starting point** for your own projects.

---

## Tech Stack

- **Spring Boot:** 4.0.1
- **Java:** 21
- **Database:** PostgreSQL 16
- **Security:** Spring Security + JWT (JJWT 0.12.5)
- **Password Hashing:** BCrypt
- **Build Tool:** Maven

---

## Prerequisites

- Java 17+ installed
- Maven installed
- PostgreSQL running (or use Docker)
- Postman or cURL for testing

---

## Quick Start

### 1. Clone and Build

```bash
git clone https://github.com/fayupable/spring-infrastructure-modules.git
cd spring-infrastructure-modules/jwt-basic
mvn clean install
```

### 2. Start PostgreSQL

**Option A: Docker**
```bash
docker run --name jwt-postgres \
  -e POSTGRES_USER=jwtuser \
  -e POSTGRES_PASSWORD=jwtpass123 \
  -e POSTGRES_DB=jwtdb \
  -p 5432:5432 \
  -d postgres:16-alpine
```

**Option B: Use existing PostgreSQL**
```sql
CREATE DATABASE jwt-basic-demo-db;
CREATE USER jwtuser WITH PASSWORD 'jwtpass123';
GRANT ALL PRIVILEGES ON DATABASE jwtdb TO jwtuser;
```

### 3. Run Application

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

**Postman Screenshot:**
![Register User](docs/images/01-register.png)
---

#### 2. Login

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
  }'
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "user@demo.com",
  "roles": ["ROLE_USER"]
}
```

**Postman Screenshot:**
![Login User](docs/images/02-login-user.png)
---

#### 2b. Login as Admin

**Request Body:**
```json
{
  "email": "admin@demo.com",
  "password": "admin123"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@demo.com",
    "password": "admin123"
  }'
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "admin@demo.com",
  "roles": ["ROLE_USER", "ROLE_ADMIN"]
}
```

**Postman Screenshot:**
![Login Admin](docs/images/03-login-admin.png)

---

### Protected Endpoints (JWT Token Required)

#### 3. Test Authenticated Endpoint

**Endpoint:** `GET /api/test/hello`

**Headers:**
```
Authorization: Bearer YOUR_JWT_TOKEN_HERE
```

**cURL:**
```bash
curl -X GET http://localhost:8080/api/test/hello \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response (200 OK):**
```
Hello, admin@demo.com! Your JWT token is working. Roles: [ROLE_ADMIN, ROLE_USER]
```

**Postman Screenshot:**
![Protected Endpoint](docs/images/04-protected-endpoint.png)

---

## Demo Users

The application automatically creates two demo users on startup:

| Email | Password | Roles |
|-------|----------|-------|
| `user@demo.com` | `user123` | ROLE_USER |
| `admin@demo.com` | `admin123` | ROLE_USER, ROLE_ADMIN |

**Note:** These users are for **demonstration only**. Remove or disable in production.

---

## Testing with Postman

### Setup

1. Open Postman
2. Create new requests for each endpoint
3. For protected endpoints, add token to Authorization tab:
    - Type: Bearer Token
    - Token: (paste your JWT token)

### Test Flow

1. **Register** a new user (or use demo users)
2. **Login** to get JWT token
3. **Copy token** from response
4. **Test protected endpoint** with token in Authorization header

---

## Project Structure

```
jwt-basic/
├── src/main/java/io/fayupable/jwtbasic/
│   ├── config/
│   │   └── SecurityConfig.java            # Spring Security configuration
│   ├── controller/
│   │   ├── AuthController.java            # Login/Register endpoints
│   │   └── TestController.java            # Test protected endpoint
│   ├── entity/
│   │   ├── Role.java                     # Role entity
│   │   └── User.java                     # User entity
│   ├── enums/
│   │   ├── RoleName.java                 # Role name enum
│   │   └── UserStatus.java               # User status enum
│   ├── init/
│   │   └── DataInitializer.java          # Database initialization with demo users
│   ├── repository/
│   │   ├── RoleRepository.java
│   │   └── UserRepository.java
│   ├── request/
│   │   ├── LoginRequest.java             # Login request DTO
│   │   └── RegisterRequest.java          # Register request DTO
│   ├── response/
│   │   ├── AuthResponse.java             # Login response with JWT token
│   │   └── MessageResponse.java          # Simple message response
│   ├── security/
│   │   ├── jwt/
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtAuthEntryPoint.java
│   │   │   └── JwtUtils.java             # JWT token operations
│   │   └── user/
│   │       ├── UserDetailsImpl.java
│   │       └── UserDetailsServiceImpl.java
│   ├── service/
│   │   ├── AuthService.java              # Service implementation
│   │   └── IAuthService.java             # Service interface
│   └── JwtBasicApplication.java          # Main application
├── src/main/resources/
│   └── application.yml                    # Application configuration
├── docs/
│   └── images/                            # Screenshots for README
├── pom.xml
└── README.md
```

---

## Key Components Explained

### 1. JWT Token Flow

```
User → Login Request → AuthenticationManager
  ↓
Password verified with BCrypt
  ↓
JWT Token generated with user info + roles
  ↓
Token returned to user
  ↓
User includes token in Authorization header
  ↓
JwtAuthenticationFilter validates token
  ↓
SecurityContext populated with user details
  ↓
Request proceeds to controller
```

### 2. Security Configuration

- **CSRF:** Disabled (using JWT, not cookies)
- **Session:** Stateless (no server-side sessions)
- **Password:** BCrypt hashed
- **Public endpoints:** `/api/auth/**`
- **Protected endpoints:** Everything else

### 3. JWT Token Contents

```json
{
  "sub": "user@demo.com",
  "roles": ["ROLE_USER"],
  "iat": 1704067200,
  "exp": 1704070800
}
```

---

## Configuration

### application.yml

Key configurations:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/jwt-basic-demo-db
    username: jwtuser
    password: jwtpass123

auth:
  token:
    jwtSecret: YOUR_SECRET_KEY_HERE
    expirationInMils: 900000  # 15 minutes
```

**Important:** Generate your own JWT secret:
```bash
openssl rand -base64 32
```

---

## Docker Support

### Run with Docker Compose

**Coming soon:** Full Docker Compose setup with PostgreSQL

```bash
docker-compose up -d
```

---

## Common Issues

### 1. Database Connection Failed

**Error:** `Unable to obtain JDBC Connection`

**Solution:** Ensure PostgreSQL is running and credentials match `application.yml`

### 2. Token Expired

**Error:** `JWT token is expired`

**Solution:** Login again to get a new token (tokens expire after 15 minutes)

### 3. Invalid Token

**Error:** `Invalid or expired token`

**Solution:** Check that you're including the full token with `Bearer ` prefix

---

## Production Checklist

Before deploying to production:

- [ ] Change JWT secret key
- [ ] Use environment variables for sensitive data
- [ ] Disable or remove demo users
- [ ] Set appropriate token expiration times
- [ ] Add refresh token functionality
- [ ] Implement token blacklist (for logout)
- [ ] Add rate limiting
- [ ] Enable HTTPS
- [ ] Set up proper logging
- [ ] Add monitoring and alerts

---

## What's NOT Included (By Design)

This is a **minimal** JWT implementation. The following are intentionally excluded:

- Refresh tokens
- Email verification
- Password reset
- OAuth2/Social login
- Rate limiting
- Advanced role hierarchies
- Account lockout
- Audit logging

These features can be added based on your requirements.

---

## Further Reading

### Official Documentation
- [Spring Security](https://spring.io/projects/spring-security)
- [JJWT Library](https://github.com/jwtk/jjwt)
- [JWT.io](https://jwt.io/)

### Related Modules
- `jwt-refresh-token` - JWT with refresh token support (coming soon)

---

## Contributing

Found a bug? Have a suggestion?

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

---

## Author

**Fayupable**

Part of the Spring Infrastructure Modules collection.

---

## Changelog

### v1.0.0 (2025-01-01)
- Initial release
- Basic JWT authentication
- User registration and login
- Role-based access control
- Demo users initialization

