-- ============================================================================
-- Specification Demo Database Initialization Script
-- ============================================================================

-- Create database if not exists
SELECT 'CREATE DATABASE specification_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'specification_db')\gexec

-- Create user if not exists
DO
$$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE rolname = 'specuser') THEN
      CREATE USER specuser WITH PASSWORD 'specpass123';
END IF;
END
$$;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE specification_db TO specuser;

-- Connect to the database
\c specification_db

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO specuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO specuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO specuser;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\echo 'Database specification_db initialized successfully!'