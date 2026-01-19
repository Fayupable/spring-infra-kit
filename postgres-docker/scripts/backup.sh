#!/bin/bash

set -e

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

BACKUP_DIR=${BACKUP_DIR:-./backups}
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/backup_${TIMESTAMP}.sql"

echo "================================================"
echo "Creating PostgreSQL Backup"
echo "================================================"

# Create backup directory if it doesn't exist
mkdir -p ${BACKUP_DIR}

echo "Creating backup: ${BACKUP_FILE}"

# Create backup
docker exec postgres-docker-db pg_dump -U ${POSTGRES_USER} appdb > ${BACKUP_FILE}

# Compress backup
echo "Compressing backup..."
gzip ${BACKUP_FILE}

BACKUP_FILE_GZ="${BACKUP_FILE}.gz"

echo ""
echo "Backup created successfully!"
echo "   File: ${BACKUP_FILE_GZ}"
echo "   Size: $(du -h ${BACKUP_FILE_GZ} | cut -f1)"
echo ""

# Clean old backups (keep last 7 days by default)
RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-7}
echo "Cleaning backups older than ${RETENTION_DAYS} days..."
find ${BACKUP_DIR} -name "backup_*.sql.gz" -type f -mtime +${RETENTION_DAYS} -delete

echo ""
echo "================================================"
echo "Backup Complete!"
echo "================================================"