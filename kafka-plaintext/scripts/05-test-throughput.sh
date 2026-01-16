#!/bin/bash

set -e

TOPIC="perf-test"
NUM_RECORDS=100000
RECORD_SIZE=1024
THROUGHPUT=-1

echo "========================================"
echo "Kafka Performance Test"
echo "========================================"
echo ""
echo "Test Configuration:"
echo "  Topic: $TOPIC"
echo "  Records: $NUM_RECORDS"
echo "  Record Size: $RECORD_SIZE bytes"
echo "  Throughput: Unlimited"
echo ""

echo "Creating test topic..."
docker exec kafka-broker kafka-topics \
    --bootstrap-server localhost:9092 \
    --create \
    --topic "$TOPIC" \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists \
    --config retention.ms=3600000 > /dev/null 2>&1

echo ""
echo "========================================"
echo "Producer Performance Test"
echo "========================================"
echo ""

docker exec kafka-broker kafka-producer-perf-test \
    --topic "$TOPIC" \
    --num-records "$NUM_RECORDS" \
    --record-size "$RECORD_SIZE" \
    --throughput "$THROUGHPUT" \
    --producer-props bootstrap.servers=localhost:9092

echo ""
echo "========================================"
echo "Consumer Performance Test"
echo "========================================"
echo ""

docker exec kafka-broker kafka-consumer-perf-test \
    --bootstrap-server localhost:9092 \
    --topic "$TOPIC" \
    --messages "$NUM_RECORDS" \
    --threads 1 \
    --show-detailed-stats

echo ""
echo "========================================"
echo "Performance Test Complete"
echo "========================================"
echo ""
echo "Clean up test topic:"
echo "  docker exec kafka-broker kafka-topics --bootstrap-server localhost:9092 --delete --topic $TOPIC"
echo ""