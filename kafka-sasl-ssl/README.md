# Kafka SSL Setup

Complete Apache Kafka deployment with SSL/TLS encryption using Docker Compose.

## Overview

This project provides a production-ready Kafka cluster with SSL security:

- Apache Kafka 7.6.0 with SSL/TLS encryption
- Apache Zookeeper for cluster coordination
- Kafka UI for cluster management and monitoring
- Automated SSL certificate generation
- Pre-configured topics and test scripts

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Zookeeper     │────▶│  Kafka Broker    │◀────│   Kafka UI      │
│   Port: 2181    │     │   SSL Port: 9093 │     │  Port: 8080     │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                               ▲
                               │ SSL/TLS
                               │
                        ┌──────────────┐
                        │   Clients    │
                        └──────────────┘
```

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- Bash shell
- OpenSSL
- Java keytool

## Important: Certificates Not Included

SSL certificates are generated locally and NOT included in the repository.

Before starting the cluster, you must generate certificates:
```bash
./scripts/01-generate-certs.sh
```

This creates the `certs/` directory with:
- CA certificate and key
- Server keystore and truststore
- Client truststore

The `certs/` directory is ignored by git for security.

## Quick Start

### 0. Setup Permissions
Before running scripts, ensure they are executable:
```bash
chmod +x ./scripts/*.sh
```

### 1. Generate SSL Certificates

```bash
cd kafka-ssl
./scripts/01-generate-certs.sh
```

This creates:
- Certificate Authority (CA)
- Server keystore and truststore
- Client truststore
- All credentials stored in `certs/` directory

### 2. Start Kafka Cluster

```bash
./scripts/02-start.sh
```

Services will start in this order:
1. Zookeeper (waits for health check)
2. Kafka Broker (waits for Zookeeper)
3. Kafka UI (waits for Kafka)

Startup time: 30-60 seconds

### 3. Create Topics

```bash
./scripts/03-create-topics.sh
```

Creates default topics:
- events (3 partitions)
- logs (3 partitions)
- transactions (3 partitions)

### 4. Test SSL Connection

```bash
./scripts/04-test-ssl.sh
```

Runs connectivity tests:
- SSL handshake verification
- Topic listing
- Message production
- Message consumption

### 5. Access Kafka UI

```bash
./scripts/07-ui.sh
```

Opens web browser at: http://localhost:8080

Features:
- Browse topics and messages
- View consumer groups
- Monitor broker metrics
- Produce test messages
- View broker configuration

## Directory Structure

```
kafka-ssl/
├── certs/                          # SSL certificates (generated)
│   ├── ca-cert                     # Certificate Authority
│   ├── ca-key                      # CA private key
│   ├── kafka.server.keystore.jks   # Server keystore
│   ├── kafka.server.truststore.jks # Server truststore
│   └── kafka.client.truststore.jks # Client truststore
├── configs/                        # Configuration files
│   ├── client.properties           # Client SSL config
│   ├── server.properties           # Broker config
│   └── zookeeper.properties        # Zookeeper config
├── docker/                         # Docker configurations
│   ├── docker-compose.yml          # Service definitions
│   ├── kafka/
│   │   ├── Dockerfile
│   │   └── server.properties
│   └── zookeeper/
│       ├── Dockerfile
│       └── zookeeper.properties
└── scripts/                        # Management scripts
    ├── 01-generate-certs.sh        # Certificate generation
    ├── 02-start.sh                 # Start cluster
    ├── 03-create-topics.sh         # Create topics
    ├── 04-test-ssl.sh              # Test SSL
    ├── 05-cleanup.sh               # Stop and cleanup
    ├── 06-logs.sh                  # View logs
    └── 07-ui.sh                    # Open UI
```

## SSL Configuration

### Server Configuration

Location: `docker/kafka/server.properties`

Key SSL settings:
```properties
listeners=SSL://0.0.0.0:9093
advertised.listeners=SSL://kafka:9093
inter.broker.listener.name=SSL

ssl.keystore.location=/etc/kafka/secrets/kafka.server.keystore.jks
ssl.keystore.password=yourSecretKey
ssl.key.password=yourSecretKey
ssl.truststore.location=/etc/kafka/secrets/kafka.server.truststore.jks
ssl.truststore.password=yourSecretKey
ssl.client.auth=none
ssl.enabled.protocols=TLSv1.2,TLSv1.3
ssl.endpoint.identification.algorithm=
```

### Client Configuration

Location: `configs/client.properties`

```properties
security.protocol=SSL
ssl.truststore.location=/etc/kafka/secrets/kafka.client.truststore.jks
ssl.truststore.password=yourSecretKey
ssl.endpoint.identification.algorithm=
```

### Certificate Details

The certificate generation script creates:

**Certificate Authority:**
- Subject: CN=kafka-ca, OU=IT, O=KafkaSSL, L=SanFrancisco, ST=CA, C=US
- Validity: 365 days
- Key size: 2048 bits

**Server Certificate:**
- Subject: CN=localhost, OU=IT, O=KafkaSSL, L=SanFrancisco, ST=CA, C=US
- SAN: DNS:kafka, DNS:localhost, IP:127.0.0.1
- Validity: 365 days
- Key size: 2048 bits

All passwords: `yourSecretKey`

## Management Scripts

### 01-generate-certs.sh

Generates SSL certificates and keystores.

```bash
./scripts/01-generate-certs.sh
```

Output location: `certs/`

### 02-start.sh

Starts all services with health checks.

```bash
./scripts/02-start.sh
```

Validates:
- Certificates exist
- Docker is installed
- Services start successfully
- Kafka is responsive

### 03-create-topics.sh

Creates predefined topics.

```bash
./scripts/03-create-topics.sh
```

Default topics:
- events
- logs
- transactions

### 04-test-ssl.sh

Tests SSL connectivity and message flow.

```bash
./scripts/04-test-ssl.sh [topic-name]
```

Default topic: events

Tests performed:
1. SSL handshake
2. Topic listing
3. Message production
4. Message consumption

### 05-cleanup.sh

Stops services with optional data cleanup.

```bash
# Stop containers, keep data
./scripts/05-cleanup.sh

# Stop containers, delete data volumes
./scripts/05-cleanup.sh --volumes

# Stop containers, delete data and certificates
./scripts/05-cleanup.sh --all
```

### 06-logs.sh

View service logs.

```bash
# View Kafka logs (last 100 lines)
./scripts/06-logs.sh kafka

# View Zookeeper logs
./scripts/06-logs.sh zookeeper

# Follow logs in real-time
./scripts/06-logs.sh kafka --follow
```

### 07-ui.sh

Opens Kafka UI in default browser.

```bash
./scripts/07-ui.sh
```

URL: http://localhost:8080

## Docker Services

### Zookeeper

- **Image:** confluentinc/cp-zookeeper:7.6.0
- **Port:** 2181
- **Memory:** 128M - 384M
- **CPU:** 0.5 cores

Configuration:
- Client port: 2181
- Tick time: 2000ms
- Init limit: 5
- Sync limit: 2

### Kafka Broker

- **Image:** confluentinc/cp-kafka:7.6.0
- **Port:** 9093 (SSL)
- **Memory:** 256M - 512M
- **CPU:** 1.0 cores

Configuration:
- Broker ID: 1
- Partitions: 3 (default)
- Replication factor: 1
- Log retention: 1 hour
- SSL enabled on port 9093

### Kafka UI

- **Image:** provectuslabs/kafka-ui:latest
- **Port:** 8080
- **Memory:** 256M - 512M
- **CPU:** 0.5 cores

Features:
- Topic management
- Message browsing
- Consumer group monitoring
- Broker health status

## Client Integration

### Java/Spring Boot

Add dependencies:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Configuration:
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9093
    properties:
      security.protocol: SSL
      ssl.truststore.location: classpath:kafka.client.truststore.jks
      ssl.truststore.password: yourSecretKey
      ssl.endpoint.identification.algorithm: ""
```

### Command Line Producer

```bash
docker exec -it kafka-broker-ssl kafka-console-producer \
  --bootstrap-server localhost:9093 \
  --producer.config /etc/kafka/secrets/client.properties \
  --topic events
```

### Command Line Consumer

```bash
docker exec -it kafka-broker-ssl kafka-console-consumer \
  --bootstrap-server localhost:9093 \
  --consumer.config /etc/kafka/secrets/client.properties \
  --topic events \
  --from-beginning
```

## Troubleshooting

### SSL Handshake Failure

Check certificate validity:
```bash
keytool -list -v -keystore certs/kafka.server.keystore.jks \
  -storepass yourSecretKey
```

Verify SAN includes hostname:
```bash
openssl x509 -in certs/cert-signed -text -noout | grep DNS
```

### Connection Refused

Check if services are running:
```bash
docker ps
```

View logs:
```bash
./scripts/06-logs.sh kafka
```

Test connectivity:
```bash
docker exec kafka-broker-ssl kafka-broker-api-versions \
  --bootstrap-server localhost:9093 \
  --command-config /etc/kafka/secrets/client.properties
```

### Kafka UI Not Connecting

Verify Kafka is healthy:
```bash
docker exec kafka-broker-ssl kafka-topics \
  --bootstrap-server localhost:9093 \
  --command-config /etc/kafka/secrets/client.properties \
  --list
```

Check UI logs:
```bash
docker logs kafka-ui-ssl
```

### Certificate Expired

Regenerate certificates:
```bash
./scripts/05-cleanup.sh --all
./scripts/01-generate-certs.sh
./scripts/02-start.sh
```

## Security Considerations

### Transport Security

This setup provides:
- TLS 1.2/1.3 encryption for all broker communication
- Certificate-based broker authentication
- Protection against man-in-the-middle attacks

### Authentication

SSL mode provides transport encryption but no client authentication.

For authentication, consider:
- SASL_SSL (username/password + SSL)
- mTLS (mutual TLS with client certificates)

### Message Encryption

Messages are encrypted in transit but stored in plaintext on disk.

For message-level encryption:
- Implement application-level encryption
- Encrypt before producing
- Decrypt after consuming

### Access Control

Configure Kafka ACLs for authorization:
```bash
docker exec kafka-broker-ssl kafka-acls \
  --bootstrap-server localhost:9093 \
  --command-config /etc/kafka/secrets/client.properties \
  --add \
  --allow-principal User:CN=client \
  --operation Read \
  --topic events
```

## Performance Tuning

### Broker Configuration

Adjust in `docker/kafka/server.properties`:

```properties
# Network threads
num.network.threads=3

# IO threads
num.io.threads=8

# Socket buffers
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

# Log retention
log.retention.hours=168
log.segment.bytes=1073741824
```

### Producer Configuration

```properties
# Compression
compression.type=lz4

# Batching
batch.size=16384
linger.ms=10

# Acknowledgments
acks=all
```

### Consumer Configuration

```properties
# Fetch size
fetch.min.bytes=1
fetch.max.wait.ms=500

# Session timeout
session.timeout.ms=10000
heartbeat.interval.ms=3000
```

## Monitoring

### Health Checks

Docker Compose includes health checks:

Kafka:
```yaml
healthcheck:
  test: ["CMD", "kafka-broker-api-versions", 
         "--bootstrap-server", "localhost:9093",
         "--command-config", "/etc/kafka/secrets/client.properties"]
  interval: 10s
  timeout: 5s
  retries: 5
```

Zookeeper:
```yaml
healthcheck:
  test: ["CMD", "nc", "-z", "localhost", "2181"]
  interval: 10s
  timeout: 5s
  retries: 3
```

### Metrics

Access JMX metrics:
```bash
docker exec kafka-broker-ssl kafka-run-class kafka.tools.JmxTool \
  --object-name kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec \
  --jmx-url service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi
```

### Kafka UI Metrics

Available at: http://localhost:8080

Provides:
- Broker CPU and memory usage
- Topic partition distribution
- Consumer lag monitoring
- Message throughput graphs

## Backup and Recovery

### Backup Topics

Export topic data:
```bash
docker exec kafka-broker-ssl kafka-console-consumer \
  --bootstrap-server localhost:9093 \
  --consumer.config /etc/kafka/secrets/client.properties \
  --topic events \
  --from-beginning \
  --max-messages 1000 > backup.json
```

### Backup Zookeeper

Export Zookeeper data:
```bash
docker exec kafka-zookeeper-ssl zkCli.sh -server localhost:2181 get /brokers/ids/1
```

### Volume Backup

Backup Docker volumes:
```bash
docker run --rm \
  -v kafka-ssl_kafka_data:/data \
  -v $(pwd):/backup \
  busybox tar czf /backup/kafka-data.tar.gz /data
```

### Restore

Stop services:
```bash
./scripts/05-cleanup.sh --volumes
```

Restore volume:
```bash
docker run --rm \
  -v kafka-ssl_kafka_data:/data \
  -v $(pwd):/backup \
  busybox tar xzf /backup/kafka-data.tar.gz -C /
```

Start services:
```bash
./scripts/02-start.sh
```

## Production Deployment

### Multi-Broker Setup

Add to `docker-compose.yml`:
```yaml
kafka-2:
  build:
    context: ./kafka
  container_name: kafka-broker-ssl-2
  hostname: kafka-2
  ports:
    - "9094:9094"
  environment:
    KAFKA_BROKER_ID: 2
  volumes:
    - kafka_data_2:/var/lib/kafka/data
    - ../certs:/etc/kafka/secrets:ro
```

Update `server.properties`:
```properties
broker.id=2
listeners=SSL://0.0.0.0:9094
advertised.listeners=SSL://kafka-2:9094
default.replication.factor=3
min.insync.replicas=2
```

### External Access

For external clients, add port mappings:
```yaml
kafka:
  ports:
    - "9093:9093"
  environment:
    KAFKA_ADVERTISED_LISTENERS: SSL://your-domain.com:9093
```

### Resource Limits

Adjust for production:
```yaml
kafka:
  deploy:
    resources:
      limits:
        cpus: '4.0'
        memory: 4G
      reservations:
        cpus: '2.0'
        memory: 2G
```

### Persistence

Use named volumes or bind mounts:
```yaml
volumes:
  kafka_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/kafka
```


## Support

For issues and questions:
1. Check logs: `./scripts/06-logs.sh kafka`
2. Review troubleshooting section
3. Verify SSL certificates are valid
4. Ensure Docker resources are sufficient

## References

- Apache Kafka Documentation: https://kafka.apache.org/documentation/
- Confluent Platform: https://docs.confluent.io/
- Kafka UI: https://github.com/provectus/kafka-ui
- SSL/TLS Configuration: https://kafka.apache.org/documentation/#security_ssl