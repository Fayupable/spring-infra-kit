# Redis Docker Setup

Production-ready Redis setup with Docker, including Redis Commander web interface for easy management.

## Features

- Redis 7 Alpine (lightweight and fast)
- Password authentication
- Persistent storage with RDB and AOF
- Redis Commander web UI
- Health checks and auto-restart
- Optimized configuration for production
- Helper scripts for common operations

## Prerequisites

- Docker
- Docker Compose
- Bash shell (for helper scripts)

## Quick Start

```bash
# Navigate to project directory
cd redis-docker

# Make scripts executable
chmod +x scripts/*.sh

# Copy environment file and customize if needed
cp .env.example .env

# Start containers
./scripts/start.sh

# Test connection
./scripts/test-connection.sh
```

## Connection Information

### Direct Connection

```
Host: localhost
Port: 6379
Password: redis123
```

### Redis Commander Web UI

```
URL: http://localhost:8081
Username: admin
Password: admin
```

### Redis CLI

```bash
# Connect via docker exec
docker exec -it redis-docker redis-cli -a redis123

# Or use the script
./scripts/connect.sh
```

## Configuration

### Environment Variables (.env)

```bash
# Redis Configuration
REDIS_PORT=6379
REDIS_PASSWORD=redis123

# Redis Commander
COMMANDER_PORT=8081
COMMANDER_USER=admin
COMMANDER_PASSWORD=admin
```

### Redis Configuration (redis.conf)

Key settings in `redis.conf`:

- **Persistence**: RDB + AOF enabled
- **Max Memory**: 256MB (configurable)
- **Eviction Policy**: allkeys-lru
- **Password**: Required
- **Databases**: 16 databases (0-15)

## Available Scripts

### Start/Stop

```bash
./scripts/start.sh     # Start all containers
./scripts/stop.sh      # Stop all containers
./scripts/reset.sh     # Reset Redis (delete all data)
```

### Connection

```bash
./scripts/connect.sh          # Connect via Redis CLI
./scripts/test-connection.sh  # Run connection tests
```

### Manual Commands

```bash
# View logs
docker-compose logs -f redis

# Monitor Redis commands in real-time
docker exec -it redis-docker redis-cli -a redis123 MONITOR

# Check Redis info
docker exec redis-docker redis-cli -a redis123 INFO

# Get memory stats
docker exec redis-docker redis-cli -a redis123 INFO memory

# Flush all databases (BE CAREFUL!)
docker exec redis-docker redis-cli -a redis123 FLUSHALL
```

## Basic Redis Commands

### String Operations

```bash
# Set a value
SET key "value"

# Get a value
GET key

# Set with expiration (in seconds)
SETEX key 3600 "value"

# Set if not exists
SETNX key "value"

# Increment/Decrement
INCR counter
DECR counter
INCRBY counter 5

# Multiple get/set
MSET key1 "value1" key2 "value2"
MGET key1 key2
```

### JSON Storage (as String)

```bash
# Store JSON object
SET user:1 '{"id":"1","name":"John","email":"john@example.com"}'

# Get JSON object
GET user:1

# Store with expiration (1 hour)
SETEX user:2 3600 '{"id":"2","name":"Jane","email":"jane@example.com"}'
```

### Hash Operations

```bash
# Set hash fields
HSET user:1 name "John" email "john@example.com" age "30"

# Get single field
HGET user:1 name

# Get all fields
HGETALL user:1

# Get multiple fields
HMGET user:1 name email

# Check if field exists
HEXISTS user:1 name

# Delete field
HDEL user:1 age

# Increment field
HINCRBY user:1 login_count 1
```

### List Operations

```bash
# Push to list (left/right)
LPUSH mylist "value1"
RPUSH mylist "value2" "value3"

# Pop from list
LPOP mylist
RPOP mylist

# Get range
LRANGE mylist 0 -1

# Get list length
LLEN mylist

# Trim list
LTRIM mylist 0 99

# Insert before/after
LINSERT mylist BEFORE "value2" "new_value"
```

### Set Operations

```bash
# Add members to set
SADD myset "member1" "member2" "member3"

# Get all members
SMEMBERS myset

# Check membership
SISMEMBER myset "member1"

# Remove member
SREM myset "member2"

# Set operations
SINTER set1 set2        # Intersection
SUNION set1 set2        # Union
SDIFF set1 set2         # Difference

# Random member
SRANDMEMBER myset
```

### Sorted Set Operations

```bash
# Add members with scores
ZADD leaderboard 100 "player1" 200 "player2" 150 "player3"

# Get range by rank
ZRANGE leaderboard 0 -1
ZRANGE leaderboard 0 -1 WITHSCORES

# Get range by score
ZRANGEBYSCORE leaderboard 100 200

# Get reverse range (highest to lowest)
ZREVRANGE leaderboard 0 10 WITHSCORES

# Get rank
ZRANK leaderboard "player1"
ZREVRANK leaderboard "player1"

# Get score
ZSCORE leaderboard "player1"

# Increment score
ZINCRBY leaderboard 10 "player1"

# Remove member
ZREM leaderboard "player2"
```

### Key Management

```bash
# Check if key exists
EXISTS key

# Delete keys
DEL key1 key2 key3

# Get key type
TYPE key

# Set expiration
EXPIRE key 3600
EXPIREAT key 1735689600

# Check TTL (time to live)
TTL key

# Remove expiration
PERSIST key

# Rename key
RENAME oldkey newkey

# Get all keys (USE WITH CAUTION in production)
KEYS *
KEYS user:*

# Scan keys (better for production)
SCAN 0 MATCH user:* COUNT 100
```

### Database Operations

```bash
# Select database (0-15)
SELECT 0

# Flush current database
FLUSHDB

# Flush all databases
FLUSHALL

# Get database size
DBSIZE

# Save database
SAVE         # Synchronous
BGSAVE       # Background
```

## Common Usage Patterns

### 1. Cache with Expiration

```bash
# Cache data for 1 hour (3600 seconds)
SETEX cache:user:123 3600 '{"name":"John","email":"john@example.com"}'

# Get cached data
GET cache:user:123

# Check remaining time
TTL cache:user:123
```

### 2. Counter with Expiration

```bash
# Page view counter (expire after 24 hours)
INCR page:views:home
EXPIRE page:views:home 86400

# Rate limiting (max 100 requests per minute)
INCR rate_limit:user:123
EXPIRE rate_limit:user:123 60
GET rate_limit:user:123
```

### 3. Session Storage

```bash
# Store session (30 minutes)
SETEX session:abc123 1800 '{"userId":"123","role":"admin"}'

# Extend session
EXPIRE session:abc123 1800

# Delete session
DEL session:abc123
```

### 4. Leaderboard

```bash
# Add scores
ZADD game:leaderboard 1000 "player1"
ZADD game:leaderboard 1500 "player2"
ZADD game:leaderboard 800 "player3"

# Top 10 players
ZREVRANGE game:leaderboard 0 9 WITHSCORES

# Player rank
ZREVRANK game:leaderboard "player1"

# Update score
ZINCRBY game:leaderboard 100 "player1"
```

### 5. Queue System

```bash
# Add to queue
RPUSH queue:tasks '{"id":"1","type":"email"}'
RPUSH queue:tasks '{"id":"2","type":"sms"}'

# Process from queue
LPOP queue:tasks

# Queue size
LLEN queue:tasks
```

## Persistence

Redis supports two persistence mechanisms:

### RDB (Redis Database)

- Point-in-time snapshots
- Good for backups
- Configured in redis.conf:
  ```
  save 900 1      # Save after 900 seconds if 1 key changed
  save 300 10     # Save after 300 seconds if 10 keys changed
  save 60 10000   # Save after 60 seconds if 10000 keys changed
  ```

### AOF (Append Only File)

- Logs every write operation
- Better durability
- Configured in redis.conf:
  ```
  appendonly yes
  appendfsync everysec  # fsync every second
  ```

### Backup Files Location

```bash
# Inside container
/data/dump.rdb
/data/appendonly.aof

# On host (docker volume)
docker volume inspect redis-docker_redis_data
```

### Manual Backup

```bash
# Create backup
docker exec redis-docker redis-cli -a redis123 BGSAVE

# Copy backup file
docker cp redis-docker:/data/dump.rdb ./backup-$(date +%Y%m%d).rdb
```

### Restore from Backup

```bash
# Stop Redis
docker-compose down

# Copy backup file
docker cp backup-20240117.rdb redis-docker:/data/dump.rdb

# Start Redis
docker-compose up -d
```

## Memory Management

### Check Memory Usage

```bash
# Overall memory stats
docker exec redis-docker redis-cli -a redis123 INFO memory

# Memory usage of specific key
docker exec redis-docker redis-cli -a redis123 MEMORY USAGE key

# Get memory stats
docker exec redis-docker redis-cli -a redis123 INFO stats
```

### Eviction Policies

Configured in `redis.conf`:

```
maxmemory 256mb
maxmemory-policy allkeys-lru
```

Available policies:

- `allkeys-lru` - Remove least recently used keys (recommended)
- `allkeys-lfu` - Remove least frequently used keys
- `volatile-lru` - Remove LRU keys with expire set
- `volatile-ttl` - Remove keys with shortest TTL
- `noeviction` - Return errors when memory limit reached

## Monitoring

### Real-time Monitoring

```bash
# Monitor all commands
docker exec -it redis-docker redis-cli -a redis123 MONITOR

# Stats
docker exec redis-docker redis-cli -a redis123 INFO stats

# Clients
docker exec redis-docker redis-cli -a redis123 CLIENT LIST

# Slow log
docker exec redis-docker redis-cli -a redis123 SLOWLOG GET 10
```

### Key Statistics

```bash
# Number of keys
docker exec redis-docker redis-cli -a redis123 DBSIZE

# Key distribution by type
docker exec redis-docker redis-cli -a redis123 --scan | \
  xargs -L 1 docker exec redis-docker redis-cli -a redis123 TYPE | \
  sort | uniq -c
```

## Security

### Production Recommendations

1. **Change Default Password**
   ```bash
   # Edit .env file
   REDIS_PASSWORD=your-strong-password-here
   ```

2. **Disable Dangerous Commands**
   ```conf
   # Add to redis.conf
   rename-command FLUSHDB ""
   rename-command FLUSHALL ""
   rename-command CONFIG ""
   ```

3. **Bind to Specific Interface**
   ```conf
   # Edit redis.conf
   bind 127.0.0.1
   ```

4. **Use SSL/TLS (for production)**
   ```yaml
   # In docker-compose.yml
   command: >
     redis-server /usr/local/etc/redis/redis.conf
     --tls-port 6380
     --port 0
     --tls-cert-file /path/to/cert.pem
     --tls-key-file /path/to/key.pem
     --tls-ca-cert-file /path/to/ca.pem
   ```

## Troubleshooting

### Container won't start

```bash
# Check logs
docker-compose logs redis

# Check if port is in use
lsof -i :6379
netstat -an | grep 6379

# Remove old containers
docker-compose down -v
./scripts/start.sh
```

### Connection refused

```bash
# Check if Redis is running
docker ps | grep redis

# Check health
docker inspect redis-docker | grep Health

# Test connection
docker exec redis-docker redis-cli -a redis123 PING
```

### Permission denied

```bash
# Make scripts executable
chmod +x scripts/*.sh

# Check file ownership
ls -la redis.conf
```

### Memory issues

```bash
# Check current memory usage
docker exec redis-docker redis-cli -a redis123 INFO memory

# Flush database if needed
docker exec redis-docker redis-cli -a redis123 FLUSHALL

# Restart with more memory
# Edit redis.conf: maxmemory 512mb
docker-compose restart redis
```

## Cleanup

```bash
# Stop containers (keep data)
./scripts/stop.sh

# Stop and remove containers (keep data)
docker-compose down

# Stop and remove everything including data
docker-compose down -v

# Remove only this project's volume
docker volume rm redis-docker_redis_data
```

## Project Structure

```
redis-docker/
├── docker-compose.yml          # Main Docker Compose configuration
├── redis.conf                  # Redis configuration file
├── .env.example                # Environment variables template
├── .env                        # Actual environment variables (git-ignored)
├── .gitignore                  # Git ignore rules
├── README.md                   # This file
└── scripts/                    # Helper scripts
    ├── start.sh
    ├── stop.sh
    ├── reset.sh
    ├── connect.sh
    └── test-connection.sh
```

## Useful Links

- [Redis Documentation](https://redis.io/documentation)
- [Redis Commands](https://redis.io/commands)
- [Redis Docker Image](https://hub.docker.com/_/redis)
- [Redis Commander](https://github.com/joeferner/redis-commander)