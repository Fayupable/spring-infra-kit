# PostgreSQL Integration Demo

A comprehensive Spring Boot application demonstrating PostgreSQL integration with JPA/Hibernate, featuring CRUD
operations, pagination, and RESTful API design.

## Overview

This project serves as a production-ready template for integrating PostgreSQL with Spring Boot applications. It
implements a clean architecture with proper separation of concerns, including entities, repositories, services,
controllers, and comprehensive error handling.

## Features

- RESTful API endpoints for User, Product, and Order management
- PostgreSQL database integration with JPA/Hibernate
- Docker containerization with docker-compose
- Database initialization with sample data
- Request validation using Jakarta Bean Validation
- Global exception handling
- Health check endpoints with Actuator
- Pagination and sorting support
- Transactional operations
- Structured logging

## Architecture

### Project Structure

```
postgres-integration/
├── src/
│   └── main/
│       ├── java/io/fayupable/postgres/
│       │   ├── config/          # Configuration classes
│       │   ├── controller/      # REST controllers
│       │   ├── dto/             # Data Transfer Objects
│       │   │   ├── request/     # Request DTOs
│       │   │   └── response/    # Response DTOs
│       │   ├── entity/          # JPA entities
│       │   ├── exception/       # Custom exceptions
│       │   ├── mapper/          # Entity-DTO mappers
│       │   ├── repository/      # JPA repositories
│       │   └── service/         # Business logic
│       │                        # Service implementations
│       └── resources/
│           └── application.yml  # Application configuration
├── docker/
│   ├── docker-compose.yml       # Docker composition
│   └── init-scripts.sql         # Database initialization
├── Dockerfile                   # Application container
└── pom.xml                      # Maven dependencies
```

### Technology Stack

- **Java 21**
- **Spring Boot 3.5.9**
- **Spring Data JPA**
- **PostgreSQL 16**
- **Hibernate**
- **Lombok**
- **Maven**
- **Docker & Docker Compose**

## Prerequisites

- JDK 21 or higher
- Maven 3.9+
- Docker and Docker Compose
- Postman (for API testing)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd postgres-integration
```

### 2. Build the Application

```bash
mvn clean package -DskipTests
```

### 3. Run with Docker Compose

```bash
cd docker
docker-compose up -d
```

This will start:

- PostgreSQL database on port 5433
- Spring Boot application on port 8080

### 4. Verify Application Status

Check application logs:

```bash
docker-compose logs -f app
```

Expected output:

![Application Startup Logs](docs/images/startup-logs.png)

Check health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP",
  "components": {
    "databaseHealth": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "status": "Connected",
        "schema": "app_schema"
      }
    },
    "db": {
      "status": "UP"
    }
  }
}
```

## Database Schema

The application uses the following database schema in the `app_schema`:

### Tables

**users**

- `id` (BIGSERIAL, Primary Key)
- `username` (VARCHAR(50), Unique, Not Null)
- `email` (VARCHAR(100), Unique, Not Null)
- `full_name` (VARCHAR(100))
- `age` (INTEGER)
- `status` (VARCHAR(20), Default: 'ACTIVE')
- `created_at` (TIMESTAMP WITH TIME ZONE)
- `updated_at` (TIMESTAMP WITH TIME ZONE)

**products**

- `id` (BIGSERIAL, Primary Key)
- `name` (VARCHAR(200), Not Null)
- `description` (TEXT)
- `price` (DECIMAL(10,2), Not Null)
- `stock_quantity` (INTEGER, Default: 0)
- `category` (VARCHAR(50))
- `is_active` (BOOLEAN, Default: true)
- `created_at` (TIMESTAMP WITH TIME ZONE)
- `updated_at` (TIMESTAMP WITH TIME ZONE)

**orders**

- `id` (BIGSERIAL, Primary Key)
- `user_id` (BIGINT, Foreign Key → users.id)
- `order_number` (VARCHAR(50), Unique, Not Null)
- `total_amount` (DECIMAL(10,2), Not Null)
- `status` (VARCHAR(20), Default: 'PENDING')
- `notes` (TEXT)
- `created_at` (TIMESTAMP WITH TIME ZONE)
- `updated_at` (TIMESTAMP WITH TIME ZONE)

**order_items**

- `id` (BIGSERIAL, Primary Key)
- `order_id` (BIGINT, Foreign Key → orders.id)
- `product_id` (BIGINT, Foreign Key → products.id)
- `quantity` (INTEGER, Not Null)
- `unit_price` (DECIMAL(10,2), Not Null)
- `total_price` (DECIMAL(10,2), Not Null)
- `created_at` (TIMESTAMP WITH TIME ZONE)

### Sample Data

The database is initialized with sample data:

- 5 users with different statuses
- 10 products across various categories
- 5 orders with different statuses
- Order items linking orders to products

## API Documentation

Base URL: `http://localhost:8080/api`

### User Endpoints

#### Create User

```http
POST /api/users
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "age": 30,
  "status": "ACTIVE"
}
```

**Response (201 Created):**

```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "age": 30,
  "status": "ACTIVE",
  "createdAt": "2024-01-21T16:30:00Z",
  "updatedAt": "2024-01-21T16:30:00Z"
}
```

#### Get User by ID

```http
GET /api/users/{id}
```

**Response (200 OK):**

```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "age": 30,
  "status": "ACTIVE",
  "createdAt": "2024-01-21T16:30:00Z",
  "updatedAt": "2024-01-21T16:30:00Z"
}
```

#### Get All Users (Paginated)

```http
GET /api/users?page=0&size=10&sort=id,asc
```

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "fullName": "John Doe",
      "age": 30,
      "status": "ACTIVE",
      "createdAt": "2024-01-21T16:30:00Z",
      "updatedAt": "2024-01-21T16:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 5,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 5,
  "size": 10,
  "number": 0,
  "empty": false
}
```

#### Update User

```http
PUT /api/users/{id}
Content-Type: application/json

{
  "fullName": "John Updated Doe",
  "age": 31
}
```

**Response (200 OK):**

```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Updated Doe",
  "age": 31,
  "status": "ACTIVE",
  "createdAt": "2024-01-21T16:30:00Z",
  "updatedAt": "2024-01-21T16:35:00Z"
}
```

#### Delete User

```http
DELETE /api/users/{id}
```

**Response (204 No Content)**

---

### Additional User Endpoints

**Get User by Username:**

```http
GET /api/users/username/{username}
```

**Get User by Email:**

```http
GET /api/users/email/{email}
```

**Get Users by Status:**

```http
GET /api/users/status/{status}?page=0&size=10
```

**Search Users:**

```http
GET /api/users/search?query=john&page=0&size=10
```

**Check Username Exists:**

```http
GET /api/users/exists/username/{username}
```

**Check Email Exists:**

```http
GET /api/users/exists/email/{email}
```

---

### Product Endpoints

#### Create Product

```http
POST /api/products
Content-Type: application/json

{
  "name": "Wireless Keyboard",
  "description": "Mechanical keyboard with RGB lighting",
  "price": 89.99,
  "stockQuantity": 50,
  "category": "Electronics",
  "isActive": true
}
```

**Response (201 Created):**

```json
{
  "id": 1,
  "name": "Wireless Keyboard",
  "description": "Mechanical keyboard with RGB lighting",
  "price": 89.99,
  "stockQuantity": 50,
  "category": "Electronics",
  "isActive": true,
  "createdAt": "2024-01-21T16:30:00Z",
  "updatedAt": "2024-01-21T16:30:00Z"
}
```

#### Get Product by ID

```http
GET /api/products/{id}
```

**Response (200 OK):**

```json
{
  "id": 1,
  "name": "Wireless Keyboard",
  "description": "Mechanical keyboard with RGB lighting",
  "price": 89.99,
  "stockQuantity": 50,
  "category": "Electronics",
  "isActive": true,
  "createdAt": "2024-01-21T16:30:00Z",
  "updatedAt": "2024-01-21T16:30:00Z"
}
```

#### Get All Products (Paginated)

```http
GET /api/products?page=0&size=10&sort=id,asc
```

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Wireless Keyboard",
      "description": "Mechanical keyboard with RGB lighting",
      "price": 89.99,
      "stockQuantity": 50,
      "category": "Electronics",
      "isActive": true,
      "createdAt": "2024-01-21T16:30:00Z",
      "updatedAt": "2024-01-21T16:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 10,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 10,
  "size": 10,
  "number": 0,
  "empty": false
}
```

#### Update Product

```http
PUT /api/products/{id}
Content-Type: application/json

{
  "price": 79.99,
  "stockQuantity": 45,
  "isActive": true
}
```

**Response (200 OK):**

```json
{
  "id": 1,
  "name": "Wireless Keyboard",
  "description": "Mechanical keyboard with RGB lighting",
  "price": 79.99,
  "stockQuantity": 45,
  "category": "Electronics",
  "isActive": true,
  "createdAt": "2024-01-21T16:30:00Z",
  "updatedAt": "2024-01-21T16:35:00Z"
}
```

#### Delete Product

```http
DELETE /api/products/{id}
```

**Response (204 No Content)**

---

### Order Endpoints

#### Create Order

```http
POST /api/orders
Content-Type: application/json

{
  "userId": 1,
  "notes": "Please deliver before 5 PM",
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 1
    }
  ]
}
```

**Response (201 Created):**

```json
{
  "id": 1,
  "userId": 1,
  "orderNumber": "ORD-20240121163000",
  "totalAmount": 209.97,
  "status": "PENDING",
  "notes": "Please deliver before 5 PM",
  "createdAt": "2024-01-21T16:30:00Z",
  "updatedAt": "2024-01-21T16:30:00Z",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Wireless Keyboard",
      "quantity": 2,
      "unitPrice": 89.99,
      "totalPrice": 179.98,
      "createdAt": "2024-01-21T16:30:00Z"
    },
    {
      "id": 2,
      "productId": 2,
      "productName": "Wireless Mouse",
      "quantity": 1,
      "unitPrice": 29.99,
      "totalPrice": 29.99,
      "createdAt": "2024-01-21T16:30:00Z"
    }
  ]
}
```

#### Get Order by ID

```http
GET /api/orders/{id}
```

**Response (200 OK):**

```json
{
  "id": 1,
  "userId": 1,
  "orderNumber": "ORD-20240121163000",
  "totalAmount": 209.97,
  "status": "PENDING",
  "notes": "Please deliver before 5 PM",
  "createdAt": "2024-01-21T16:30:00Z",
  "updatedAt": "2024-01-21T16:30:00Z",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Wireless Keyboard",
      "quantity": 2,
      "unitPrice": 89.99,
      "totalPrice": 179.98,
      "createdAt": "2024-01-21T16:30:00Z"
    },
    {
      "id": 2,
      "productId": 2,
      "productName": "Wireless Mouse",
      "quantity": 1,
      "unitPrice": 29.99,
      "totalPrice": 29.99,
      "createdAt": "2024-01-21T16:30:00Z"
    }
  ]
}
```

#### Get All Orders (Paginated)

```http
GET /api/orders?page=0&size=10&sort=id,desc
```

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": 1,
      "userId": 1,
      "orderNumber": "ORD-20240121163000",
      "totalAmount": 209.97,
      "status": "PENDING",
      "notes": "Please deliver before 5 PM",
      "createdAt": "2024-01-21T16:30:00Z",
      "updatedAt": "2024-01-21T16:30:00Z",
      "items": [
        {
          "id": 1,
          "productId": 1,
          "productName": "Wireless Keyboard",
          "quantity": 2,
          "unitPrice": 89.99,
          "totalPrice": 179.98,
          "createdAt": "2024-01-21T16:30:00Z"
        }
      ]
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 5,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 5,
  "size": 10,
  "number": 0,
  "empty": false
}
```

#### Get Orders by User ID

```http
GET /api/orders/user/{userId}?page=0&size=10
```

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": 1,
      "userId": 1,
      "orderNumber": "ORD-20240121163000",
      "totalAmount": 209.97,
      "status": "PENDING",
      "notes": "Please deliver before 5 PM",
      "createdAt": "2024-01-21T16:30:00Z",
      "updatedAt": "2024-01-21T16:30:00Z",
      "items": []
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 3,
  "totalPages": 1
}
```

#### Update Order

```http
PUT /api/orders/{id}
Content-Type: application/json

{
  "status": "PROCESSING",
  "notes": "Updated delivery instructions"
}
```

**Response (200 OK):**

```json
{
  "id": 1,
  "userId": 1,
  "orderNumber": "ORD-20240121163000",
  "totalAmount": 209.97,
  "status": "PROCESSING",
  "notes": "Updated delivery instructions",
  "createdAt": "2024-01-21T16:30:00Z",
  "updatedAt": "2024-01-21T16:35:00Z",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Wireless Keyboard",
      "quantity": 2,
      "unitPrice": 89.99,
      "totalPrice": 179.98,
      "createdAt": "2024-01-21T16:30:00Z"
    }
  ]
}
```

#### Delete Order

```http
DELETE /api/orders/{id}
```

**Response (204 No Content)**

---

## Error Handling

The application provides structured error responses:

### Validation Error (400 Bad Request)

```json
{
  "timestamp": "2024-01-21T16:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users",
  "errors": {
    "username": "Username must be between 3 and 50 characters",
    "email": "Email must be valid"
  }
}
```

### Resource Not Found (404 Not Found)

```json
{
  "timestamp": "2024-01-21T16:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: 999",
  "path": "/api/users/999"
}
```

### Resource Already Exists (409 Conflict)

```json
{
  "timestamp": "2024-01-21T16:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Username already exists: john_doe",
  "path": "/api/users"
}
```

### Internal Server Error (500 Internal Server Error)

```json
{
  "timestamp": "2024-01-21T16:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/users"
}
```

## Testing with Postman

### Import Postman Collection

1. Open Postman
2. Click "Import" button
3. Select the `postman/PostgreSQL-Integration.postman_collection.json` file
4. Collection will be imported with all endpoints ready to test

### Environment Variables

Create a Postman environment with:

- `baseUrl`: `http://localhost:8080`
- `userId`: (will be set automatically after creating a user)
- `productId`: (will be set automatically after creating a product)
- `orderId`: (will be set automatically after creating an order)

## Running Profiles

The application supports two profiles:

### Development Profile (dev)

- Uses local PostgreSQL on port 5432
- Database: `postgres_integration_db`
- User: `fayupable`
- Password: `fayupable`

Run with:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Profile (docker)

- Uses containerized PostgreSQL
- Database: `postgres_integration_db`
- User: `appuser`
- Password: `apppass123`

Automatically activated when running with docker-compose.

## Configuration

### Application Properties

Located in `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: postgres-integration
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        default_schema: app_schema
    hibernate:
      ddl-auto: validate

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### Database Connection

**Development:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres_integration_db
    username: fayupable
    password: fayupable
```

**Docker:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/postgres_integration_db
    username: appuser
    password: apppass123
```

## Troubleshooting

### Application won't start

**Check if PostgreSQL is running:**

```bash
docker-compose ps
```

**View application logs:**

```bash
docker-compose logs -f app
```

**Restart containers:**

```bash
docker-compose down
docker-compose up -d
```

### Database connection errors

**Verify database is accessible:**

```bash
docker exec -it postgres_integration_postgres psql -U appuser -d postgres_integration_db
```

**Check database exists:**

```sql
\l
```

**Verify schema and tables:**

```sql
\c
postgres_integration_db
SET search_path TO app_schema;
\dt
```

### Port already in use

If port 8080 or 5433 is already in use, modify `docker/docker-compose.yml`:

```yaml
services:
  postgres:
    ports:
      - "5434:5432"  # Change 5433 to 5434

  app:
    ports:
      - "8081:8080"  # Change 8080 to 8081
```

## Development

### Adding New Entities

1. Create entity class in `entity/` package
2. Create repository interface in `repository/` package
3. Create DTOs in `dto/request/` and `dto/response/`
4. Create mapper in `mapper/` package
5. Create service interface and implementation
6. Create controller with REST endpoints
7. Update database init script if needed

### Code Style

The project follows standard Java conventions:

- Use Lombok annotations for boilerplate code
- Use constructor injection with `@RequiredArgsConstructor`
- Add logging with `@Slf4j`
- Use `@Transactional` for service methods
- Validate requests with Jakarta Bean Validation

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Hibernate Documentation](https://hibernate.org/orm/documentation/)
- [Docker Documentation](https://docs.docker.com/)
- [Jakarta Bean Validation](https://jakarta.ee/specifications/bean-validation/3.0/)

