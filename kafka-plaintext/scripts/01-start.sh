#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_DIR/docker/docker-compose.yml"

echo "========================================"
echo "Starting Kafka Plaintext Setup"
echo "========================================"
echo ""

if ! command -v docker &> /dev/null; then
    echo "Error: Docker not installed"
    echo "Install Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "Error: Docker Compose not installed"
    echo "Install Docker Compose: https://docs.docker.com/compose/install/"
    exit 1
fi

COMPOSE_CMD="docker-compose"
if ! command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker compose"
fi

echo "Starting Zookeeper and Kafka..."
$COMPOSE_CMD -f "$COMPOSE_FILE" up -d

echo ""
echo "Waiting for services to be ready..."
echo "This may take 30-60 seconds..."
echo ""

MAX_WAIT=60
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    if docker exec kafka-broker kafka-broker-api-versions --bootstrap-server localhost:9092 &> /dev/null; then
        echo ""
        echo "========================================"
        echo "Kafka Cluster is Ready"
        echo "========================================"
        echo ""
        echo "Services:"
        echo "  Zookeeper:  localhost:2181"
        echo "  Kafka:      localhost:9092"
        echo "  Kafka UI:   http://localhost:8080"
        echo ""
        echo "Next steps:"
        echo "  1. Create topics:    ./scripts/02-create-topics.sh"
        echo "  2. View logs:        ./scripts/07-logs.sh kafka"
        echo "  3. View UI:          open http://localhost:8080"
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
echo "  docker logs kafka-broker"
echo "  docker logs kafka-zookeeper"
echo ""
exit 1