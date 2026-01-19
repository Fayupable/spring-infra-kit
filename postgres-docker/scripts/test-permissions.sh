#!/bin/bash

set -e

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "================================================"
echo "Testing PostgreSQL User Permissions"
echo "================================================"
echo ""

# Test appuser permissions
echo "Testing appuser (should have full CRUD access)..."
docker exec postgres-docker-db psql -U ${APP_USER} -d appdb -c "
    SELECT 'SELECT test' AS test;
    INSERT INTO app_schema.users (username, email, password_hash, full_name)
    VALUES ('test_user', 'test@example.com', 'hash', 'Test User');
    UPDATE app_schema.users SET full_name = 'Updated User' WHERE username = 'test_user';
    DELETE FROM app_schema.users WHERE username = 'test_user';
" && echo "PASS: appuser - All operations successful!" || echo "FAIL: appuser operations failed!"

echo ""

# Test readonly_user permissions
echo "Testing readonly_user (should only SELECT)..."
docker exec postgres-docker-db psql -U ${READONLY_USER} -d appdb -c "
    SELECT 'SELECT test' AS test, COUNT(*) as user_count FROM app_schema.users;
" && echo "PASS: readonly_user - SELECT successful!"

echo ""
echo "Testing readonly_user INSERT (should fail)..."
docker exec postgres-docker-db psql -U ${READONLY_USER} -d appdb -c "
    INSERT INTO app_schema.users (username, email, password_hash)
    VALUES ('should_fail', 'fail@example.com', 'hash');
" 2>&1 | grep -q "permission denied" && echo "PASS: readonly_user - INSERT correctly denied!" || echo "WARNING: readonly_user - INSERT should have failed!"

echo ""
echo "================================================"
echo "Permission Tests Complete!"
echo "================================================"