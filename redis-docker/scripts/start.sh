#!/bin/bash

set -e

echo "================================================"
echo "Starting Redis Docker Setup"
echo "================================================"

# Check if .env file exists, if not copy from .env.example
if [ ! -f .env ]; then
    echo "Creating .env file from .env.example..."
    cp .env.example .env
    echo "Done. .env file created. Please update it with your credentials."
fi

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

echo "Starting Docker containers..."
docker-compose up -d

echo ""
echo "Waiting for Redis to be healthy..."
sleep 3

# Wait for Redis to be ready
until docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} ping > /dev/null 2>&1; do
    echo "   Waiting for Redis..."
    sleep 2
done

echo ""
echo "================================================"
echo "Redis is ready!"
echo "================================================"
echo ""
echo "Connection Information:"
echo "   Host: localhost"
echo "   Port: ${REDIS_PORT}"
echo "   Password: ${REDIS_PASSWORD}"
echo ""
echo "Redis Commander Web UI:"
echo "   URL: http://localhost:${COMMANDER_PORT}"
echo "   Username: ${COMMANDER_USER}"
echo "   Password: ${COMMANDER_PASSWORD}"
echo ""
echo "================================================"
echo "Useful Commands:"
echo "================================================"
echo "  View logs:         docker-compose logs -f redis"
echo "  Connect via CLI:   docker exec -it redis-docker redis-cli -a ${REDIS_PASSWORD}"
echo "  Monitor commands:  docker exec -it redis-docker redis-cli -a ${REDIS_PASSWORD} MONITOR"
echo "  Stop containers:   ./scripts/stop.sh"
echo "  Reset Redis:       ./scripts/reset.sh"
echo "================================================"