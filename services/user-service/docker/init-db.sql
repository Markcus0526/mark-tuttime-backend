-- ============================================================================
-- USER SERVICE - Full Database Schema without schema prefix (default schema)
-- ============================================================================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS postgis;

-- Set timezone and encoding for consistency (optional but recommended)
SET timezone = 'UTC';
SET client_encoding = 'UTF8';
SET client_min_messages TO WARNING;

-- === ENUM TYPES ===
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN
CREATE TYPE user_status AS ENUM ('ACTIVE', 'SUSPENDED', 'DELETED', 'PENDING_VERIFICATION');
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'auth_provider') THEN
CREATE TYPE auth_provider AS ENUM ('PHONE_SMS', 'EMAIL_PASS', 'GOOGLE', 'APPLE');
END IF;
END$$;

-- === Timestamp update function ===
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- === TABLES ===

-- Users table
CREATE TABLE users
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number    VARCHAR(20) UNIQUE,
    email           VARCHAR(255) UNIQUE,
    full_name       VARCHAR(255),
    profile_picture TEXT,
    status          user_status NOT NULL DEFAULT 'ACTIVE',
    last_login      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT contact_exists CHECK (phone_number IS NOT NULL OR email IS NOT NULL)
);

CREATE INDEX idx_users_phone_email ON users (phone_number, email);
CREATE INDEX idx_users_is_deleted ON users (is_deleted);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();

-- Enforce soft delete by preventing hard deletes
CREATE OR REPLACE FUNCTION prevent_user_delete()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Direct DELETE is forbidden. Use soft delete by setting is_deleted = TRUE and status = ''DELETED''';
RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_delete ON users;
CREATE TRIGGER trg_prevent_delete
    BEFORE DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION prevent_user_delete();

-- Roles Table
CREATE TABLE roles
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

-- Insert default roles
INSERT INTO roles (name) VALUES
                             ('ADMIN'),
                             ('OWNER'),
                             ('MANAGER'),
                             ('HOSTESS'),
                             ('USER'),
                             ('GUEST');

-- User Roles Table
CREATE TABLE user_roles
(
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     INT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Authentication Methods Table
CREATE TABLE user_auth_methods
(
    id         SERIAL PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    auth_type  auth_provider NOT NULL,
    credential TEXT, -- Hashed password or OAuth ID
    verified   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, auth_type)
);

CREATE INDEX idx_auth_methods_user_id ON user_auth_methods(user_id);

-- Phone Verification Codes Table
CREATE TABLE phone_verification_codes
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) NOT NULL,
    code         VARCHAR(10) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    used         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_phone_codes_phone_number ON phone_verification_codes(phone_number) WHERE used = FALSE;

-- Password Reset Tokens Table
CREATE TABLE password_reset_tokens
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);

-- User Locations Table with PostGIS geography type
CREATE TABLE user_locations
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    location        geography(Point, 4326) NOT NULL,
    accuracy_meters FLOAT,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_locations_gist ON user_locations USING GIST(location);

-- === COMMENTS ===
COMMENT ON TABLE users IS 'User accounts table with soft-delete implemented using is_deleted and status columns';
COMMENT ON COLUMN users.is_deleted IS 'Soft delete flag, true if user is deleted';
COMMENT ON COLUMN users.deleted_at IS 'Timestamp when user was soft deleted';

COMMENT ON TABLE roles IS 'User roles table';
COMMENT ON TABLE user_roles IS 'Mapping table between users and their roles';

COMMENT ON TABLE user_auth_methods IS 'Authentication methods for users (password, OAuth, etc.)';
COMMENT ON TABLE phone_verification_codes IS 'Temporary codes for phone number verification';
COMMENT ON TABLE password_reset_tokens IS 'Tokens for password reset functionality';

COMMENT ON TABLE user_locations IS 'Stores user geolocation data with accuracy and timestamp';

-- Insert default admin user
INSERT INTO users (id, email, full_name, status, created_at, updated_at)
VALUES (
           gen_random_uuid(),
           'admin@tuttime.com',
           'Administrator',
           'ACTIVE',
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       );

-- Ensure the pgcrypto extension is available for the crypt() function
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Get the ID of the newly created admin user and assign the ADMIN role
WITH admin_user AS (
    SELECT id FROM users WHERE email = 'admin@tuttime.com'
),
     admin_role AS (
         SELECT id AS role_id FROM roles WHERE name = 'ADMIN'
     )
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT admin_user.id, admin_role.role_id, CURRENT_TIMESTAMP
FROM admin_user
         CROSS JOIN admin_role; -- Explicitly states your intent to combine the single rows

-- Insert authentication method for admin user (tuttime with email/password)
WITH admin_user AS (
    SELECT id FROM users WHERE email = 'admin@tuttime.com'
)
INSERT INTO user_auth_methods (user_id, auth_type, credential, verified, created_at)
SELECT id, 'EMAIL_PASS', crypt('password', gen_salt('bf')), TRUE, CURRENT_TIMESTAMP
FROM admin_user;

-- ============================================================================
-- END OF USER SERVICE SCHEMA (no schema prefix)
-- ============================================================================