#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_DIR/docker/docker-compose.yml"
CERTS_DIR="$PROJECT_DIR/certs"

echo "========================================"
echo "Starting Kafka SSL Cluster"
echo "========================================"
echo ""

if [ ! -d "$CERTS_DIR" ] || [ ! -f "$CERTS_DIR/kafka.server.keystore.jks" ]; then
    echo "Error: Certificates not found"
    echo "Run: ./scripts/01-generate-certs.sh"
    exit 1
fi

if ! command -v docker &> /dev/null; then
    echo "Error: Docker not installed"
    exit 1
fi

COMPOSE_CMD="docker-compose"
if ! command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker compose"
fi

echo "Copying client.properties to certs directory..."
cp "$PROJECT_DIR/configs/client.properties" "$CERTS_DIR/client.properties"

echo ""
echo "Starting Zookeeper and Kafka with SSL..."
$COMPOSE_CMD -f "$COMPOSE_FILE" up -d --build

echo ""
echo "Waiting for services to be ready..."
echo "This may take 30-60 seconds..."
echo ""

MAX_WAIT=60
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    if docker exec kafka-broker-ssl kafka-broker-api-versions \
        --bootstrap-server localhost:9093 \
        --command-config /etc/kafka/secrets/client.properties &> /dev/null; then
        echo ""
        echo "========================================"
        echo "Kafka SSL Cluster is Ready"
        echo "========================================"
        echo ""
        echo "Services:"
        echo "  Zookeeper: localhost:2181"
        echo "  Kafka SSL: localhost:9093"
        echo ""
        echo "SSL Configuration:"
        echo "  Protocol: SSL/TLS"
        echo "  Keystore: kafka.server.keystore.jks"
        echo "  Truststore: kafka.server.truststore.jks"
        echo ""
        echo "Next steps:"
        echo "  1. Create topics: ./scripts/03-create-topics.sh"
        echo "  2. Test SSL:      ./scripts/04-test-ssl.sh"
        echo "  3. View logs:     ./scripts/06-logs.sh kafka"
        echo ""
        exit 0
    fi

    echo -n "."
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done

echo ""
echo "========================================"
echo "Kafka failed to start within ${MAX_WAIT}s"
echo "========================================"
echo ""
echo "Check logs:"
echo "  docker logs kafka-broker-ssl"
echo "  docker logs kafka-zookeeper-ssl"
echo ""
exit 1