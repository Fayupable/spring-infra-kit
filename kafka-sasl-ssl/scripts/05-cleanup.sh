#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_DIR/docker/docker-compose.yml"

echo "========================================"
echo "Kafka SSL Cleanup"
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
elif [ "$1" == "--all" ]; then
    echo "Stopping containers, removing volumes and certificates..."
    $COMPOSE_CMD -f "$COMPOSE_FILE" down -v
    rm -rf "$PROJECT_DIR/certs"
    echo ""
    echo "Cleanup complete (data and certificates deleted)"
    echo ""
    echo "To regenerate certificates, run:"
    echo "  ./scripts/01-generate-certs.sh"
else
    echo "Stopping containers (keeping data and certificates)..."
    $COMPOSE_CMD -f "$COMPOSE_FILE" down
    echo ""
    echo "Cleanup complete (data and certificates preserved)"
    echo ""
    echo "Options:"
    echo "  --volumes   Delete data volumes"
    echo "  --all       Delete data volumes and certificates"
fi

echo ""