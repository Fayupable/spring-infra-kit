#!/bin/bash

set -e

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "================================================"
echo "Testing Redis Connection"
echo "================================================"
echo ""

echo "Test 1: PING"
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} PING

echo ""
echo "Test 2: SET/GET"
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} SET test_key "Hello Redis"
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} GET test_key

echo ""
echo "Test 3: JSON (using native Redis commands)"
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} SET user:1 '{"name":"John","age":30}'
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} GET user:1

echo ""
echo "Test 4: Hash (alternative to JSON)"
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} HSET user:hash:1 name "Jane" age 25
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} HGETALL user:hash:1

echo ""
echo "Test 5: List"
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} LPUSH mylist "value1" "value2" "value3"
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} LRANGE mylist 0 -1

echo ""
echo "Test 6: Set"
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} SADD myset "member1" "member2" "member3"
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} SMEMBERS myset

echo ""
echo "Test 7: Info"
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} INFO server | grep redis_version

echo ""
echo "Cleaning up test data..."
docker exec redis-docker redis-cli -a ${REDIS_PASSWORD} DEL test_key user:1 user:hash:1 mylist myset

echo ""
echo "================================================"
echo "All tests passed!"
echo "================================================"