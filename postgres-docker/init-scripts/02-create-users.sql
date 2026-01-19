-- Create application user with password
CREATE USER appuser WITH PASSWORD 'apppass123';

-- Create read-only user
CREATE USER readonly_user WITH PASSWORD 'readonly123';

-- Create admin user for management operations
CREATE USER dbadmin WITH PASSWORD 'admin123' CREATEDB CREATEROLE;

-- Add comments
COMMENT ON ROLE appuser IS 'Application service account';
COMMENT ON ROLE readonly_user IS 'Read-only access for reporting';
COMMENT ON ROLE dbadmin IS 'Database administrator';