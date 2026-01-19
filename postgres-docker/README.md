# PostgreSQL Docker Setup

Production-ready PostgreSQL setup with Docker, complete with user management, permissions, and sample data.

## Features

- PostgreSQL 16 Alpine (lightweight)
- Multiple user roles with different permissions
- Health checks and auto-restart
- pgAdmin web interface
- Automated backup/restore scripts
- Sample database with realistic data
- Init scripts for database setup
- Connection pooling ready

## Prerequisites

- Docker
- Docker Compose
- Bash shell (for helper scripts)

## Quick Start

```bash
# Clone or navigate to the project directory
cd postgres-docker

# Make scripts executable
chmod +x scripts/*.sh

# Copy environment file and customize if needed
cp .env.example .env

# Start containers
./scripts/start.sh

# Check logs
docker-compose logs -f postgres
```

## Database Structure

### Databases

- `maindb` - Main PostgreSQL database
- `appdb` - Application database (created by init scripts)
- `testdb` - Test database

### Users & Permissions

| User          | Password    | Permissions             | Use Case                    |
|---------------|-------------|-------------------------|-----------------------------|
| postgres      | postgres    | Superuser               | Database administration     |
| appuser       | apppass123  | Full CRUD on app_schema | Application service account |
| readonly_user | readonly123 | SELECT only             | Reporting, analytics        |
| dbadmin       | admin123    | Database admin          | DB management operations    |

### Schema: app_schema

**Tables:**

- `users` - User accounts with status management
- `products` - Product catalog
- `orders` - Customer orders
- `order_items` - Order line items

**Custom Types:**

- `user_status` - ENUM: ACTIVE, INACTIVE, SUSPENDED, DELETED
- `order_status` - ENUM: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED

**Features:**

- Auto-updated `updated_at` timestamps via triggers
- Foreign key constraints with CASCADE/RESTRICT
- Check constraints for data validation
- Indexes on frequently queried columns
- Email format validation

## Connection Information

### Direct Connection

```
Host: localhost
Port: 5432
Database: appdb
Username: appuser
Password: apppass123
```

### JDBC URL

```
jdbc:postgresql://localhost:5432/appdb
```

### Spring Boot Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/appdb
    username: appuser
    password: apppass123
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: app_schema
```

### Node.js Connection (pg)

```javascript
const {Pool} = require('pg');

const pool = new Pool({
    host: 'localhost',
    port: 5432,
    database: 'appdb',
    user: 'appuser',
    password: 'apppass123',
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 2000,
});
```

### Python Connection (psycopg2)

```python
import psycopg2

conn = psycopg2.connect(
    host="localhost",
    port=5432,
    database="appdb",
    user="appuser",
    password="apppass123"
)
```

### pgAdmin Web Interface

```
URL: http://localhost:5050
Email: admin@admin.com
Password: admin
```

**Adding Server in pgAdmin:**

1. Right-click "Servers" > Create > Server
2. General Tab: Name = "Local PostgreSQL"
3. Connection Tab:
    - Host: postgres-docker-db
    - Port: 5432
    - Maintenance database: appdb
    - Username: appuser
    - Password: apppass123

## Available Scripts

### Start/Stop

```bash
./scripts/start.sh    # Start all containers
./scripts/stop.sh     # Stop all containers
./scripts/reset.sh    # Reset database (delete all data)
```

### Database Operations

```bash
./scripts/connect.sh           # Interactive connection selector
./scripts/backup.sh            # Create database backup
./scripts/restore.sh           # Restore from backup
./scripts/test-permissions.sh  # Test user permissions
```

### Manual Commands

```bash
# View logs
docker-compose logs -f postgres

# Connect via psql (as appuser)
docker exec -it postgres-docker-db psql -U appuser -d appdb

# Connect as postgres superuser
docker exec -it postgres-docker-db psql -U postgres -d appdb

# Run SQL file
docker exec -i postgres-docker-db psql -U appuser -d appdb < your-script.sql

# Check database size
docker exec postgres-docker-db psql -U postgres -c "SELECT pg_size_pretty(pg_database_size('appdb'));"
```

## Sample Data

The database comes pre-populated with sample data:

- **5 Users** - Different statuses (ACTIVE, INACTIVE, SUSPENDED)
- **10 Products** - Various categories and prices
- **5 Orders** - Different statuses (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
- **Order Items** - Linking orders to products

Default password for all sample users: `password` (BCrypt hash included)

### Example Queries

```sql
-- Set schema
SET
search_path TO app_schema;

-- Get active users
SELECT *
FROM users
WHERE status = 'ACTIVE';

-- Get orders with user details
SELECT o.order_number, u.username, o.total_amount, o.status
FROM orders o
         JOIN users u ON o.user_id = u.id;

-- Get product sales summary
SELECT p.name, SUM(oi.quantity) as total_sold, SUM(oi.total_price) as revenue
FROM products p
         JOIN order_items oi ON p.id = oi.product_id
GROUP BY p.id, p.name
ORDER BY revenue DESC;

-- Get user order history
SELECT u.username, COUNT(o.id) as order_count, SUM(o.total_amount) as total_spent
FROM users u
         LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.username;
```

## Backup & Restore

### Automatic Backups

```bash
# Create backup (stored in ./backups/)
./scripts/backup.sh

# Backups are automatically compressed (.gz)
# Old backups are cleaned (7 days retention by default)
```

### Manual Backup

```bash
# Full database backup
docker exec postgres-docker-db pg_dump -U postgres appdb > backup.sql

# Specific schema only
docker exec postgres-docker-db pg_dump -U postgres -n app_schema appdb > schema_backup.sql

# Data only (no schema)
docker exec postgres-docker-db pg_dump -U postgres --data-only appdb > data_backup.sql
```

### Restore

```bash
# Using restore script
./scripts/restore.sh

# Manual restore
docker exec -i postgres-docker-db psql -U postgres appdb < backup.sql
```

## Environment Variables

All configurable options are in `.env` file:

```bash
# PostgreSQL Configuration
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=maindb
POSTGRES_PORT=5432

# Application Users (created by init scripts)
APP_USER=appuser
APP_PASSWORD=apppass123
READONLY_USER=readonly_user
READONLY_PASSWORD=readonly123
DB_ADMIN_USER=dbadmin
DB_ADMIN_PASSWORD=admin123

# pgAdmin
PGADMIN_EMAIL=admin@admin.com
PGADMIN_PASSWORD=admin
PGADMIN_PORT=5050

# Backup
BACKUP_RETENTION_DAYS=7
```

## Security Considerations

### Production Deployment

1. **Change Default Passwords**

```bash
   # Edit .env file with strong passwords
   POSTGRES_PASSWORD=<strong-random-password>
   APP_PASSWORD=<strong-random-password>
```

2. **Restrict Network Access**

```yaml
   # In docker-compose.yml, remove port exposure
   # ports:
   #   - "5432:5432"  # Comment this out
```

3. **Use Secrets Management**

```bash
   # Use Docker secrets instead of environment variables
   # Or use external secret managers (AWS Secrets Manager, Vault, etc.)
```

4. **Enable SSL/TLS**

```yaml
   # Add to postgres service
   command: >
     -c ssl=on
     -c ssl_cert_file=/etc/ssl/certs/server.crt
     -c ssl_key_file=/etc/ssl/private/server.key
```

5. **Regular Backups**

```bash
   # Set up cron job for automated backups
   0 2 * * * cd /path/to/postgres-docker && ./scripts/backup.sh
```

## Troubleshooting

### Container won't start

```bash
# Check logs
docker-compose logs postgres

# Check if port is already in use
lsof -i :5432
netstat -an | grep 5432

# Remove old containers and volumes
docker-compose down -v
./scripts/start.sh
```

### Permission denied errors

```bash
# Ensure scripts are executable
chmod +x scripts/*.sh

# Check file ownership
ls -la init-scripts/
```

### Connection refused

```bash
# Wait for health check
docker-compose ps

# Check if PostgreSQL is ready
docker exec postgres-docker-db pg_isready -U postgres

# Verify network
docker network ls
docker network inspect postgres-docker_postgres-network
```

### Init scripts not running

```bash
# Init scripts only run on first container creation
# To re-run, you must remove the volume:
docker-compose down -v
docker-compose up -d
```

### pgAdmin connection issues

```bash
# Use container name as host: postgres-docker-db
# NOT localhost (when connecting from pgAdmin container)
```

## Performance Tuning

### For Development

```yaml
# Add to postgres service environment
POSTGRES_INITDB_ARGS: "-E UTF8 --locale=C"
# Faster but less safe (for dev only)
command: >
  -c fsync=off
  -c synchronous_commit=off
  -c full_page_writes=off
```

### For Production

```yaml
# Add to postgres service
command: >
  -c shared_buffers=256MB
  -c effective_cache_size=1GB
  -c maintenance_work_mem=64MB
  -c checkpoint_completion_target=0.9
  -c wal_buffers=16MB
  -c default_statistics_target=100
  -c random_page_cost=1.1
  -c effective_io_concurrency=200
  -c work_mem=4MB
  -c min_wal_size=1GB
  -c max_wal_size=4GB
```

## Monitoring

### Check Database Size

```bash
docker exec postgres-docker-db psql -U postgres -c "
  SELECT 
    pg_database.datname,
    pg_size_pretty(pg_database_size(pg_database.datname)) AS size
  FROM pg_database
  ORDER BY pg_database_size(pg_database.datname) DESC;
"
```

### Check Table Sizes

```bash
docker exec postgres-docker-db psql -U appuser -d appdb -c "
  SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
  FROM pg_tables
  WHERE schemaname = 'app_schema'
  ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
"
```

### Active Connections

```bash
docker exec postgres-docker-db psql -U postgres -c "
  SELECT 
    datname,
    usename,
    count(*) as connections
  FROM pg_stat_activity
  GROUP BY datname, usename;
"
```

## Cleanup

```bash
# Stop containers (keep data)
./scripts/stop.sh

# Stop and remove containers (keep data)
docker-compose down

# Stop and remove everything including data
docker-compose down -v

# Remove only this project's volumes
docker volume rm postgres-docker_postgres_data postgres-docker_pgadmin_data

# Remove old backups
rm -rf backups/*
```

## Project Structure

```
postgres-docker/
├── docker-compose.yml          # Main Docker Compose configuration
├── .env.example                # Environment variables template
├── .env                        # Actual environment variables (git-ignored)
├── README.md                   # This file
├── init-scripts/               # Database initialization scripts
│   ├── 01-create-database.sql
│   ├── 02-create-users.sql
│   ├── 03-grant-permissions.sql
│   ├── 04-create-schema.sql
│   ├── 05-create-tables.sql
│   └── 06-insert-sample-data.sql
├── scripts/                    # Helper scripts
│   ├── start.sh
│   ├── stop.sh
│   ├── reset.sh
│   ├── backup.sh
│   ├── restore.sh
│   ├── connect.sh
│   └── test-permissions.sh
└── backups/                    # Backup files (auto-created)
```

## Integration Examples

### Docker Compose Integration

```yaml
# In another project's docker-compose.yml
services:
  myapp:
    image: myapp:latest
    depends_on:
      - postgres
    environment:
      DATABASE_URL: postgresql://appuser:apppass123@postgres:5432/appdb

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: appuser
      POSTGRES_PASSWORD: apppass123
      POSTGRES_DB: appdb
```

### Kubernetes ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-config
data:
  POSTGRES_HOST: postgres-service
  POSTGRES_PORT: "5432"
  POSTGRES_DB: appdb
  POSTGRES_USER: appuser
```

## Contributing

Feel free to submit issues or pull requests for improvements.

## Useful Links

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker PostgreSQL Image](https://hub.docker.com/_/postgres)
- [pgAdmin Documentation](https://www.pgadmin.org/docs/)
- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [PostgreSQL Docker-Library GitHub](https://github.com/docker-library/docs/blob/master/postgres/README.md)