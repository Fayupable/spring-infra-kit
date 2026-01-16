# Kafka Plaintext Setup

A minimal Apache Kafka setup for local development and testing using Docker Compose.

---

## Purpose

This module demonstrates a basic Kafka deployment with:

- Single Zookeeper instance
- Single Kafka broker
- No authentication or encryption
- Command-line based management and testing

**WARNING: NOT for production use. No security configured.**

---

## What You'll Learn

- How to run Kafka + Zookeeper in Docker
- Kafka broker configuration basics
- Topic creation and management
- Message production and consumption via CLI
- Performance testing with Kafka tools
- Log monitoring and troubleshooting

---

## Architecture

```
┌─────────────────────────────────────────┐
│  Host Machine (localhost)               │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │  Zookeeper                        │ │
│  │  Port: 2181                       │ │
│  │  Role: Cluster coordination       │ │
│  └───────────────────────────────────┘ │
│              ↓                          │
│  ┌───────────────────────────────────┐ │
│  │  Kafka Broker                     │ │
│  │  Port: 9092 (external)            │ │
│  │  Port: 9093 (internal)            │ │
│  │  Protocol: PLAINTEXT              │ │
│  └───────────────────────────────────┘ │
│              ↓                          │
│  ┌───────────────────────────────────┐ │
│  │  Kafka UI (optional)              │ │
│  │  Port: 8080                       │ │
│  │  Web interface for monitoring     │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

---

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- bash shell
- curl (for REST API testing)
- jq (for JSON parsing, optional)

---

## Quick Start

### 0. Setup Permissions

Before running scripts, ensure they are executable:

```bash
chmod +x ./scripts/*.sh
```

### 1. Start Kafka Cluster

```bash
./scripts/01-start.sh
```

**What it does:**

- Starts Zookeeper
- Starts Kafka broker
- Waits for services to be healthy
- Verifies broker connectivity

**Expected output:**

```
Starting Zookeeper and Kafka...
Waiting for services to be ready...
Kafka is ready!

Zookeeper: localhost:2181
Kafka Broker: localhost:9092

Next: Run ./scripts/02-create-topics.sh
```

---

### 2. Create Topics

```bash
./scripts/02-create-topics.sh
```

**Creates:**

- `events` topic (3 partitions, replication factor 1)
- `logs` topic (5 partitions, replication factor 1)
- `transactions` topic (3 partitions, replication factor 1)

**Expected output:**

```
Creating topic: events (partitions=3, replication=1)
Created topic events.
Creating topic: logs (partitions=5, replication=1)
Created topic logs.
Topics created!
```

---

### 3. Produce Messages

```bash
./scripts/03-produce-messages.sh events
```

**Interactive producer:**

```
Type messages (Ctrl+C to exit):

>Hello Kafka
>This is a test message
>{"event": "user_login", "user_id": 123}
```

**Batch produce:**

```bash
./scripts/03-produce-messages.sh events < examples/sample-events.txt
```

---

### 4. Consume Messages

```bash
./scripts/04-consume-messages.sh events
```

**Output:**

```
Hello Kafka
This is a test message
{"event": "user_login", "user_id": 123}
```

**With keys:**

```bash
./scripts/04-consume-messages.sh events --with-keys
```

---

### 5. Performance Test

```bash
./scripts/05-test-throughput.sh
```

**Tests:**

- Producer throughput (messages/sec)
- Consumer throughput (messages/sec)
- End-to-end latency

---

### 6. View Logs

```bash
./scripts/07-logs.sh kafka
./scripts/07-logs.sh zookeeper
```

---

### 7. Cleanup

```bash
./scripts/06-cleanup.sh
```

---

## Project Structure

```
kafka-plaintext/
├── docker/
│   ├── docker-compose.yml       # Kafka + Zookeeper setup
│   └── healthcheck.sh           # Health check script
├── scripts/
│   ├── 01-start.sh              # Start Kafka cluster
│   ├── 02-create-topics.sh      # Create sample topics
│   ├── 03-produce-messages.sh   # Interactive producer
│   ├── 04-consume-messages.sh   # Interactive consumer
│   ├── 05-test-throughput.sh    # Performance benchmarks
│   ├── 06-cleanup.sh            # Stop and remove containers
│   ├── 07-logs.sh               # View container logs
│   └── 08-describe-cluster.sh   # Show cluster info
├── examples/
│   ├── sample-events.txt        # Sample messages
│   ├── curl-rest-proxy.sh       # REST API examples (if enabled)
│   └── kafka-cli-cheatsheet.md  # Common Kafka commands
└── README.md
```

---

## Scripts Explained

### 01-start.sh

Starts Docker Compose and verifies connectivity.

**Key operations:**

1. Check Docker availability
2. Start services with `docker-compose up -d`
3. Wait for broker to be ready
4. Test broker connectivity with `kafka-broker-api-versions`

---

### 02-create-topics.sh

Creates predefined topics with specific configurations.

**Configuration format:**

```bash
TOPICS=(
    "topic_name:partitions:replication_factor"
)
```

**Example:**

```bash
"events:3:1"  # 3 partitions, replication factor 1
```

---

### 03-produce-messages.sh

Interactive message producer using `kafka-console-producer`.

**Supports:**

- Plain text messages
- Key-value pairs (key:value format)
- Piped input from files
- JSON messages

**Usage:**

```bash
./scripts/03-produce-messages.sh <topic> [--with-keys]
```

---

### 04-consume-messages.sh

Message consumer with various options.

**Modes:**

- From beginning (`--from-beginning`)
- From latest
- With keys (`--with-keys`)
- Specific partition (`--partition N`)

**Usage:**

```bash
./scripts/04-consume-messages.sh <topic> [options]
```

---

### 05-test-throughput.sh

Runs performance benchmarks using Kafka's built-in tools.

**Tests:**

1. Producer throughput: `kafka-producer-perf-test`
2. Consumer throughput: `kafka-consumer-perf-test`

**Metrics:**

- Records per second
- MB per second
- Average latency
- Max latency
- 95th percentile latency

---

### 06-cleanup.sh

Stops containers and optionally removes volumes.

**Options:**

- Basic cleanup: `./scripts/06-cleanup.sh`
- Full cleanup with data: `./scripts/06-cleanup.sh --volumes`

---

## Common Operations

### List Topics

```bash
docker exec kafka kafka-topics \
    --bootstrap-server localhost:9092 \
    --list
```

---

### Describe Topic

```bash
docker exec kafka kafka-topics \
    --bootstrap-server localhost:9092 \
    --describe \
    --topic events
```

**Output:**

```
Topic: events   PartitionCount: 3   ReplicationFactor: 1
Topic: events   Partition: 0   Leader: 1   Replicas: 1   Isr: 1
Topic: events   Partition: 1   Leader: 1   Replicas: 1   Isr: 1
Topic: events   Partition: 2   Leader: 1   Replicas: 1   Isr: 1
```

---

### Delete Topic

```bash
docker exec kafka kafka-topics \
    --bootstrap-server localhost:9092 \
    --delete \
    --topic events
```

---

### Check Consumer Groups

```bash
docker exec kafka kafka-consumer-groups \
    --bootstrap-server localhost:9092 \
    --list
```

---

### Describe Consumer Group

```bash
docker exec kafka kafka-consumer-groups \
    --bootstrap-server localhost:9092 \
    --describe \
    --group my-consumer-group
```

---

### Change Topic Configuration

```bash
docker exec kafka kafka-configs \
    --bootstrap-server localhost:9092 \
    --entity-type topics \
    --entity-name events \
    --alter \
    --add-config retention.ms=86400000
```

---

## Configuration Files

### docker-compose.yml

Key configurations:

**Zookeeper:**

- `ZOOKEEPER_CLIENT_PORT`: 2181
- `ZOOKEEPER_TICK_TIME`: 2000

**Kafka:**

- `KAFKA_BROKER_ID`: 1
- `KAFKA_ZOOKEEPER_CONNECT`: zookeeper:2181
- `KAFKA_ADVERTISED_LISTENERS`: PLAINTEXT://localhost:9092
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE`: false (explicit topic creation)
- `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR`: 1

---

## Monitoring

### Via Logs

```bash
docker-compose -f docker/docker-compose.yml logs -f kafka
```

---

### Via Kafka UI

Access: `http://localhost:8080`

**Features:**

- Topic browser
- Message viewer
- Consumer group monitoring
- Broker metrics

---

### Via CLI

```bash
docker exec kafka kafka-run-class kafka.tools.GetOffsetShell \
    --broker-list localhost:9092 \
    --topic events \
    --time -1
```

---

## Troubleshooting

### Broker Not Starting

**Check logs:**

```bash
docker logs kafka
```

**Common issues:**

- Zookeeper not ready (wait longer)
- Port 9092 already in use
- Insufficient memory

**Solution:**

```bash
./scripts/06-cleanup.sh
./scripts/01-start.sh
```

---

### Cannot Connect to Broker

**Test connectivity:**

```bash
docker exec kafka kafka-broker-api-versions \
    --bootstrap-server localhost:9092
```

**Check advertised listeners:**

```bash
docker exec kafka cat /etc/kafka/server.properties | grep advertised
```

---

### Consumer Not Receiving Messages

**Check topic exists:**

```bash
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

**Check messages in topic:**

```bash
docker exec kafka kafka-run-class kafka.tools.GetOffsetShell \
    --broker-list localhost:9092 \
    --topic events
```

---

### High Memory Usage

**Adjust JVM settings in docker-compose.yml:**

```yaml
environment:
  KAFKA_HEAP_OPTS: "-Xmx512M -Xms512M"
```

---

## Performance Tuning

### Producer Optimization

```bash
docker exec kafka kafka-console-producer \
    --bootstrap-server localhost:9092 \
    --topic events \
    --compression-type lz4 \
    --batch-size 16384 \
    --linger-ms 10
```

---

### Consumer Optimization

```bash
docker exec kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic events \
    --max-messages 1000 \
    --fetch-min-bytes 1024
```

---

## What's NOT Included

This is a minimal setup. Missing features:

- **Security:**
    - No authentication (SASL)
    - No encryption (SSL/TLS)
    - No authorization (ACLs)

- **High Availability:**
    - Single broker (no replication)
    - Single Zookeeper (no quorum)
    - No rack awareness

- **Monitoring:**
    - No Prometheus metrics
    - No Grafana dashboards
    - No alerting

- **Production Features:**
    - No log retention policies
    - No backup strategies
    - No disaster recovery

**For production setup, see:** `kafka-sasl-ssl` module

---

## Next Steps

1. Experiment with different topic configurations
2. Test producer/consumer performance
3. Try different message formats (JSON, Avro)
4. Monitor logs during high throughput
5. Move to `kafka-sasl-ssl` for production setup

---

## Further Reading

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Kafka CLI Tools](https://kafka.apache.org/documentation/#basic_ops)
- [Confluent Platform Guide](https://docs.confluent.io/platform/current/kafka/introduction.html)
- [Kafka Performance Tuning](https://kafka.apache.org/documentation/#performance)

---

## Related Modules

- **kafka-sasl-ssl**: Production-ready Kafka with security
- **notification-service**: Event-driven notifications using Kafka
- **saga-pattern**: Distributed transactions with Kafka

---

## License

MIT License - See main repository LICENSE

---

**Status:** Ready for development and testing | NOT for production use