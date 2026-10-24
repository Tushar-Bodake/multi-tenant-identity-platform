-- ==============================================================================
-- SecureGate PostgreSQL Database Initialization
-- Sets up separate Privileged Migration Role and Restricted Application Role
-- Ensures PostgreSQL Row-Level Security (RLS) is strictly enforced for the app role.
-- ==============================================================================

-- Create migration admin role if it does not exist
DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'securegate_admin') THEN
      CREATE ROLE securegate_admin WITH LOGIN PASSWORD 'secure_admin_password_change_me' CREATEDB;
   END IF;
END
$$;

-- Create application runtime role if it does not exist (NO SUPERUSER, NO BYPASSRLS)
DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'securegate_app') THEN
      CREATE ROLE securegate_app WITH LOGIN PASSWORD 'secure_app_password_change_me' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
   END IF;
END
$$;

-- Grant database connect permissions
GRANT CONNECT ON DATABASE securegate_db TO securegate_admin;
GRANT CONNECT ON DATABASE securegate_db TO securegate_app;

-- Ensure schema permissions
\connect securegate_db

GRANT ALL ON SCHEMA public TO securegate_admin;
GRANT USAGE ON SCHEMA public TO securegate_app;

-- Default privileges for future tables created by securegate_admin
ALTER DEFAULT PRIVILEGES FOR ROLE securegate_admin IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO securegate_app;

ALTER DEFAULT PRIVILEGES FOR ROLE securegate_admin IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO securegate_app;
