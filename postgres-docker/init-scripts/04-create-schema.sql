\c appdb

-- Create custom schema
CREATE SCHEMA IF NOT EXISTS app_schema;

-- Grant usage on custom schema
GRANT USAGE ON SCHEMA app_schema TO appuser;
GRANT USAGE ON SCHEMA app_schema TO readonly_user;
GRANT ALL ON SCHEMA app_schema TO dbadmin;

-- Set default privileges for app_schema
ALTER DEFAULT PRIVILEGES IN SCHEMA app_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO appuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA app_schema GRANT USAGE, SELECT ON SEQUENCES TO appuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA app_schema GRANT SELECT ON TABLES TO readonly_user;

-- Create custom types
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED');
CREATE TYPE order_status AS ENUM ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED');

COMMENT ON SCHEMA app_schema IS 'Application schema for business logic tables';