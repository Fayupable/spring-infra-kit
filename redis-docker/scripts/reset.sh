#!/bin/bash

set -e

echo "================================================"
echo "WARNING: RESETTING Redis"
echo "================================================"
echo ""
echo "This will:"
echo "  1. Stop all containers"
echo "  2. Remove all volumes (DELETE ALL DATA)"
echo "  3. Restart containers with fresh Redis"
echo ""
read -p "Are you sure you want to continue? (yes/no): " -r
echo ""

if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "Reset cancelled."
    exit 1
fi

echo "Stopping containers and removing volumes..."
docker-compose down -v

echo "Starting fresh containers..."
./scripts/start.sh

echo ""
echo "================================================"
echo "Redis reset complete!"
echo "================================================"