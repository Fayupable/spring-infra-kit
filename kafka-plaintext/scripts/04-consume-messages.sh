#!/bin/bash

TOPIC=${1:-events}
shift

echo "========================================"
echo "Kafka Consumer - Topic: $TOPIC"
echo "========================================"
echo ""
echo "Press Ctrl+C to exit"
echo ""

docker exec -it kafka-broker kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic "$TOPIC" \
    --from-beginning \
    --property print.key=true \
    --property key.separator=" => " \
    "$@"