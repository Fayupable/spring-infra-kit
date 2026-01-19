-- Switch to appdb database
\c appdb

-- Grant connection privileges
GRANT CONNECT ON DATABASE appdb TO appuser;
GRANT CONNECT ON DATABASE appdb TO readonly_user;

-- Grant schema usage (will be created in next script)
GRANT USAGE ON SCHEMA public TO appuser;
GRANT USAGE ON SCHEMA public TO readonly_user;

-- Grant table privileges to appuser (all future tables)
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO appuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO appuser;

-- Grant read-only privileges to readonly_user
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO readonly_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO readonly_user;

-- Grant all privileges on public schema to dbadmin
GRANT ALL PRIVILEGES ON DATABASE appdb TO dbadmin;
GRANT ALL PRIVILEGES ON SCHEMA public TO dbadmin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO dbadmin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO dbadmin;