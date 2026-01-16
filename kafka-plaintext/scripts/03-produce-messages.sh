#!/bin/bash

TOPIC=${1:-events}
WITH_KEYS=${2}

echo "========================================"
echo "Kafka Producer - Topic: $TOPIC"
echo "========================================"
echo ""

if [ "$WITH_KEYS" == "--with-keys" ]; then
    echo "Mode: Key-Value Messages"
    echo "Format: key:value"
    echo ""
    echo "Example:"
    echo "  user123:login_event"
    echo "  order456:{\"status\":\"completed\"}"
    echo ""
    echo "Type messages (Ctrl+C to exit):"
    echo ""

    docker exec -it kafka-broker kafka-console-producer \
        --bootstrap-server localhost:9092 \
        --topic "$TOPIC" \
        --property "parse.key=true" \
        --property "key.separator=:"
else
    echo "Mode: Plain Text Messages"
    echo ""
    echo "Type messages (Ctrl+C to exit):"
    echo ""

    docker exec -it kafka-broker kafka-console-producer \
        --bootstrap-server localhost:9092 \
        --topic "$TOPIC"
fi