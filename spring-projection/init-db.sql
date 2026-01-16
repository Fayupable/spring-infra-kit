-- ============================================================================
-- Projection Demo Database Initialization Script
-- ============================================================================

-- Create database if not exists
SELECT 'CREATE DATABASE projection_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'projection_db')\gexec

-- Create user if not exists
DO
$$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE rolname = 'projuser') THEN
      CREATE USER projuser WITH PASSWORD 'projpass123';
END IF;
END
$$;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE projection_db TO projuser;

-- Connect to the database
\c projection_db

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO projuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO projuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO projuser;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\echo 'Database projection_db initialized successfully!'