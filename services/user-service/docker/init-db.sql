-- ============================================================================
-- USER SERVICE - Database Schema
-- Database: user_service_db
-- Schema: user_service
--
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

-- ============================================================================
-- COMPLETE RESTAURANT SERVICE SCHEMA (Production Ready)
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS user_service;
SET search_path TO user_service, public;

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. CUSTOM ENUMS (Prevents typos in status columns)
CREATE TYPE user_status AS ENUM ('ACTIVE', 'SUSPENDED', 'DELETED', 'PENDING_VERIFICATION');
CREATE TYPE auth_provider AS ENUM ('PHONE_SMS', 'EMAIL_PASS', 'GOOGLE', 'APPLE');

-- 3. AUTOMATION FUNCTION (Updates 'updated_at' automatically)
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$ language 'plpgsql';

-- 4. USERS TABLE
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) UNIQUE,
    email VARCHAR(255) UNIQUE,
    full_name VARCHAR(255),
    profile_picture TEXT,
    status user_status NOT NULL DEFAULT 'ACTIVE',
    last_login TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT contact_exists CHECK (phone_number IS NOT NULL OR email IS NOT NULL)
);

-- 5. ROLES & PERMISSIONS
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

INSERT INTO roles (name) VALUES ('ADMIN'); -- for Admin panel
INSERT INTO roles (name) VALUES ('OWNER'); -- for Restaurant
INSERT INTO roles (name) VALUES ('MANAGER'); -- for Restaurant
INSERT INTO roles (name) VALUES ('HOSTESS'); -- for Restaurant
INSERT INTO roles (name) VALUES ('USER'); -- for Mobile user
INSERT INTO roles (name) VALUES ('GUEST'); -- for Mobile user, not signed


CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

-- 6. AUTHENTICATION LOGIC
CREATE TABLE user_auth_methods (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    auth_type auth_provider NOT NULL,
    credential TEXT, -- Hashed password or OAuth ID
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, auth_type)
);

CREATE TABLE phone_verification_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) NOT NULL,
    code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

-- 7. GEOSPATIAL DATA
CREATE TABLE user_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    location geography(Point, 4326) NOT NULL,
    accuracy_meters FLOAT,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 8. INDEXES (Speed Optimization)
CREATE INDEX idx_users_phone_email ON users(phone_number, email);
CREATE INDEX idx_user_locations_gist ON user_locations USING GIST(location);
CREATE INDEX idx_active_phone_codes ON phone_verification_codes(phone_number) WHERE used = FALSE;

-- 9. TRIGGERS (The "Magic" that keeps timestamps accurate)
CREATE TRIGGER trg_users_updated
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

