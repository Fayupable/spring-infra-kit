#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CLIENT_CONFIG="$PROJECT_DIR/certs/client.properties"

TOPIC=${1:-events}

echo "========================================"
echo "Kafka SSL Connection Test"
echo "========================================"
echo ""

if [ ! -f "$CLIENT_CONFIG" ]; then
    echo "Error: client.properties not found"
    echo "Run: ./scripts/02-start.sh"
    exit 1
fi

echo "Test 1: Verify SSL connection to broker..."
if docker exec kafka-broker-ssl kafka-broker-api-versions \
    --bootstrap-server localhost:9093 \
    --command-config /etc/kafka/secrets/client.properties > /dev/null 2>&1; then
    echo "  SSL connection successful"
else
    echo "  SSL connection failed"
    exit 1
fi

echo ""
echo "Test 2: List topics via SSL..."
docker exec kafka-broker-ssl kafka-topics \
    --bootstrap-server localhost:9093 \
    --command-config /etc/kafka/secrets/client.properties \
    --list

echo ""
echo "Test 3: Produce test message via SSL..."
echo "test-message-$(date +%s)" | docker exec -i kafka-broker-ssl kafka-console-producer \
    --bootstrap-server localhost:9093 \
    --producer.config /etc/kafka/secrets/client.properties \
    --topic "$TOPIC"

echo "  Message sent successfully"

echo ""
echo "Test 4: Consume test message via SSL..."
docker exec kafka-broker-ssl kafka-console-consumer \
    --bootstrap-server localhost:9093 \
    --consumer.config /etc/kafka/secrets/client.properties \
    --topic "$TOPIC" \
    --from-beginning \
    --max-messages 1 \
    --timeout-ms 5000

echo ""
echo "========================================"
echo "SSL Connection Test Complete"
echo "========================================"
echo ""
echo "All SSL tests passed successfully"
echo ""
echo "Interactive producer:"
echo "  docker exec -it kafka-broker-ssl kafka-console-producer --bootstrap-server localhost:9093 --producer.config /etc/kafka/secrets/client.properties --topic $TOPIC"
echo ""
echo "Interactive consumer:"
echo "  docker exec -it kafka-broker-ssl kafka-console-consumer --bootstrap-server localhost:9093 --consumer.config /etc/kafka/secrets/client.properties --topic $TOPIC --from-beginning"
echo ""