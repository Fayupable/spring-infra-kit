#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_DIR/docker/docker-compose.yml"

echo "========================================"
echo "Kafka Cleanup"
echo "========================================"
echo ""

COMPOSE_CMD="docker-compose"
if ! command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker compose"
fi

if [ "$1" == "--volumes" ]; then
    echo "Stopping containers and removing volumes..."
    $COMPOSE_CMD -f "$COMPOSE_FILE" down -v
    echo ""
    echo "Cleanup complete (data deleted)"
else
    echo "Stopping containers (keeping data)..."
    $COMPOSE_CMD -f "$COMPOSE_FILE" down
    echo ""
    echo "Cleanup complete (data preserved)"
    echo ""
    echo "To delete all data, run:"
    echo "  ./scripts/06-cleanup.sh --volumes"
fi

echo ""