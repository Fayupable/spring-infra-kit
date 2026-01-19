#!/bin/bash

set -e

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "================================================"
echo "PostgreSQL Connection Options"
echo "================================================"
echo ""
echo "1. Connect as superuser (postgres)"
echo "2. Connect as application user (appuser)"
echo "3. Connect as read-only user (readonly_user)"
echo "4. Connect as database admin (dbadmin)"
echo ""
read -p "Select option (1-4): " option

case $option in
    1)
        echo "Connecting as ${POSTGRES_USER}..."
        docker exec -it postgres-docker-db psql -U ${POSTGRES_USER} -d appdb
        ;;
    2)
        echo "Connecting as ${APP_USER}..."
        docker exec -it postgres-docker-db psql -U ${APP_USER} -d appdb
        ;;
    3)
        echo "Connecting as ${READONLY_USER}..."
        docker exec -it postgres-docker-db psql -U ${READONLY_USER} -d appdb
        ;;
    4)
        echo "Connecting as ${DB_ADMIN_USER}..."
        docker exec -it postgres-docker-db psql -U ${DB_ADMIN_USER} -d appdb
        ;;
    *)
        echo "Invalid option"
        exit 1
        ;;
esac