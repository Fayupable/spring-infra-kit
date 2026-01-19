-- Create additional databases
CREATE DATABASE appdb WITH ENCODING 'UTF8' LC_COLLATE='en_US.utf8' LC_CTYPE='en_US.utf8';
CREATE DATABASE testdb WITH ENCODING 'UTF8' LC_COLLATE='en_US.utf8' LC_CTYPE='en_US.utf8';

-- Add comment
COMMENT ON DATABASE appdb IS 'Main application database';
COMMENT ON DATABASE testdb IS 'Test database for development';