#!/bin/bash

set -e

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

BACKUP_DIR=${BACKUP_DIR:-./backups}

echo "================================================"
echo "PostgreSQL Database Restore"
echo "================================================"
echo ""

# List available backups
echo "Available backups:"
echo ""
ls -lh ${BACKUP_DIR}/backup_*.sql.gz 2>/dev/null || {
    echo "No backup files found in ${BACKUP_DIR}"
    exit 1
}

echo ""
read -p "Enter the backup filename to restore (e.g., backup_20240117_120000.sql.gz): " BACKUP_FILE

BACKUP_PATH="${BACKUP_DIR}/${BACKUP_FILE}"

if [ ! -f "${BACKUP_PATH}" ]; then
    echo "Backup file not found: ${BACKUP_PATH}"
    exit 1
fi

echo ""
echo "WARNING: This will drop and recreate the database!"
echo "   All current data will be lost."
echo ""
read -p "Are you sure you want to continue? (yes/no): " -r
echo ""

if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "Restore cancelled."
    exit 1
fi

echo "Decompressing backup..."
gunzip -c ${BACKUP_PATH} > /tmp/restore.sql

echo "Dropping existing database..."
docker exec postgres-docker-db psql -U ${POSTGRES_USER} -c "DROP DATABASE IF EXISTS appdb;"

echo "Creating fresh database..."
docker exec postgres-docker-db psql -U ${POSTGRES_USER} -c "CREATE DATABASE appdb;"

echo "Restoring backup..."
docker exec -i postgres-docker-db psql -U ${POSTGRES_USER} appdb < /tmp/restore.sql

echo "Cleaning up..."
rm -f /tmp/restore.sql

echo ""
echo "================================================"
echo "Database restored successfully!"
echo "================================================"