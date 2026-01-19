#!/bin/bash

set -e

echo "================================================"
echo "Starting PostgreSQL Docker Setup"
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
echo "Waiting for PostgreSQL to be healthy..."
sleep 5

# Wait for PostgreSQL to be ready
until docker exec postgres-docker-db pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB} > /dev/null 2>&1; do
    echo "   Waiting for PostgreSQL..."
    sleep 2
done

echo ""
echo "================================================"
echo "PostgreSQL is ready!"
echo "================================================"
echo ""
echo "Connection Information:"
echo "   Host: localhost"
echo "   Port: ${POSTGRES_PORT}"
echo "   Database: ${POSTGRES_DB}"
echo "   Username: ${POSTGRES_USER}"
echo "   Password: ${POSTGRES_PASSWORD}"
echo ""
echo "Application Database (appdb):"
echo "   App User: ${APP_USER}"
echo "   Password: ${APP_PASSWORD}"
echo "   Read-only User: ${READONLY_USER}"
echo "   Password: ${READONLY_PASSWORD}"
echo ""
echo "pgAdmin Web UI:"
echo "   URL: http://localhost:${PGADMIN_PORT}"
echo "   Email: ${PGADMIN_EMAIL}"
echo "   Password: ${PGADMIN_PASSWORD}"
echo ""
echo "================================================"
echo "Useful Commands:"
echo "================================================"
echo "  View logs:        docker-compose logs -f postgres"
echo "  Connect via psql: docker exec -it postgres-docker-db psql -U ${POSTGRES_USER} -d appdb"
echo "  Stop containers:  ./scripts/stop.sh"
echo "  Reset database:   ./scripts/reset.sh"
echo "================================================"