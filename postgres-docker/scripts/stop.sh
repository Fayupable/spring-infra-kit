#!/bin/bash

set -e

echo "================================================"
echo "Stopping PostgreSQL Docker Setup"
echo "================================================"

echo "Stopping Docker containers..."
docker-compose down

echo ""
echo "All containers stopped successfully!"
echo ""
echo "To remove volumes as well, run:"
echo "   docker-compose down -v"
echo ""