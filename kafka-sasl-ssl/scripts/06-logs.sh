#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_DIR/docker/docker-compose.yml"

SERVICE=${1:-kafka}
FOLLOW=${2}

COMPOSE_CMD="docker-compose"
if ! command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker compose"
fi

if [ "$SERVICE" == "kafka" ]; then
    CONTAINER="kafka-broker-ssl"
elif [ "$SERVICE" == "zookeeper" ]; then
    CONTAINER="kafka-zookeeper-ssl"
else
    echo "Usage: $0 [kafka|zookeeper] [--follow]"
    exit 1
fi

if [ "$FOLLOW" == "--follow" ] || [ "$FOLLOW" == "-f" ]; then
    docker logs -f "$CONTAINER"
else
    docker logs --tail 100 "$CONTAINER"
fi