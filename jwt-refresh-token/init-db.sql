-- ============================================================================
-- JWT Refresh Token Database Initialization Script
-- ============================================================================
-- Purpose: Create database and user if they don't exist
-- Usage: Used by docker-compose PostgreSQL initialization
-- ============================================================================
-- Create database if not exists
SELECT 'CREATE DATABASE jwt_refresh_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'jwt_refresh_db')\gexec

-- Create user if not exists
DO
$$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE rolname = 'jwtuser') THEN
      CREATE USER jwtuser WITH PASSWORD 'jwtpass123';
END IF;
END
$$;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE jwt_refresh_db TO jwtuser;

-- Connect to the database
\c jwt_refresh_db

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO jwtuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO jwtuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO jwtuser;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


\echo 'Database jwt_refresh_db initialized successfully!'
