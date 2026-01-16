#!/bin/bash

set -e

echo "========================================"
echo "Creating Kafka Topics"
echo "========================================"
echo ""

TOPICS=(
    "events:3:1"
    "logs:5:1"
    "transactions:3:1"
)

for topic_config in "${TOPICS[@]}"; do
    IFS=':' read -r topic partitions replication <<< "$topic_config"

    echo "Creating topic: $topic"
    echo "  Partitions: $partitions"
    echo "  Replication Factor: $replication"

    docker exec kafka-broker kafka-topics \
        --bootstrap-server localhost:9092 \
        --create \
        --topic "$topic" \
        --partitions "$partitions" \
        --replication-factor "$replication" \
        --if-not-exists \
        --config retention.ms=3600000 \
        --config segment.bytes=1073741824

    echo "Topic '$topic' created"
    echo ""
done

echo "========================================"
echo "Topic Creation Complete"
echo "========================================"
echo ""
echo "List all topics:"
echo "  docker exec kafka-broker kafka-topics --bootstrap-server localhost:9092 --list"
echo ""
echo "Describe a topic:"
echo "  docker exec kafka-broker kafka-topics --bootstrap-server localhost:9092 --describe --topic events"
echo ""
echo "Next: Produce messages with ./scripts/03-produce-messages.sh"
echo ""