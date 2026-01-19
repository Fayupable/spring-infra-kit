#!/bin/bash

set -e

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "Connecting to Redis..."
docker exec -it redis-docker redis-cli -a ${REDIS_PASSWORD}