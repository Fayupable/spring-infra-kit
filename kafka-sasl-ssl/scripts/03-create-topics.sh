#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "========================================"
echo "Creating Kafka Topics"
echo "========================================"
echo ""

TOPICS=("events" "logs" "transactions")

for TOPIC in "${TOPICS[@]}"; do
    echo "Creating topic: $TOPIC"
    docker exec kafka-broker-ssl kafka-topics \
        --bootstrap-server localhost:9093 \
        --command-config /etc/kafka/secrets/client.properties \
        --create \
        --topic "$TOPIC" \
        --partitions 3 \
        --replication-factor 1 \
        --if-not-exists
done

echo ""
echo "========================================"
echo "Topics Created Successfully"
echo "========================================"
echo ""
echo "List topics:"
echo "  docker exec kafka-broker-ssl kafka-topics --bootstrap-server localhost:9093 --command-config /etc/kafka/secrets/client.properties --list"
echo ""