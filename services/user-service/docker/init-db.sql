-- ============================================================================
-- USER SERVICE - Full Database Schema (Optimized)
-- ============================================================================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS postgis;

-- Set settings
SET timezone = 'UTC';
SET client_encoding = 'UTF8';

-- === ENUM TYPES ===
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN
CREATE TYPE user_status AS ENUM ('ACTIVE', 'BLOCKED', 'PENDING_VERIFICATION');
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'auth_provider') THEN
CREATE TYPE auth_provider AS ENUM ('PHONE_SMS', 'EMAIL_PASS', 'GOOGLE', 'APPLE');
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'verification_type') THEN
CREATE TYPE verification_type AS ENUM ('EMAIL_VERIFY', 'PHONE_VERIFY', 'PASSWORD_RESET', 'MFA');
END IF;
END $$;

-- === Timestamp update function ===
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- === TABLES ===

-- Users Table
CREATE TABLE users (
                       id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
                       phone_number    VARCHAR(20) UNIQUE,
                       email           VARCHAR(255) UNIQUE,
                       full_name       VARCHAR(255),
                       profile_picture TEXT,
                       status          user_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
                       last_login      TIMESTAMPTZ,
                       deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
                       deleted_at      TIMESTAMPTZ,
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT contact_exists CHECK (phone_number IS NOT NULL OR email IS NOT NULL)
);

-- Roles Table
CREATE TABLE roles (
                       id          SERIAL PRIMARY KEY,
                       name        VARCHAR(50) UNIQUE NOT NULL,
                       description TEXT
);

-- User Roles Mapping Table
CREATE TABLE user_roles (
                            user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                            role_id     INT         NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
                            created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, role_id)
);

-- Authentication Methods Table
CREATE TABLE user_auth_methods (
                                   id         SERIAL        PRIMARY KEY,
                                   user_id    UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                   auth_type  auth_provider NOT NULL,
                                   credential TEXT, -- Hashed password (NULL for OAuth)
                                   provider_id TEXT, -- Google/Apple Subject ID (NULL for Email)
                                   verified   BOOLEAN       NOT NULL DEFAULT FALSE,
                                   created_at TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   UNIQUE (user_id, auth_type),
                                   CONSTRAINT unique_provider_id UNIQUE (auth_type, provider_id)
);

-- Verification Codes Table
CREATE TABLE verification_codes (
                                    id         UUID PRIMARY KEY           DEFAULT gen_random_uuid(),
                                    target     VARCHAR(255)      NOT NULL, -- email or phone
                                    code_hash  TEXT              NOT NULL, -- Securely stored hash
                                    type       verification_type NOT NULL,
                                    expires_at TIMESTAMPTZ       NOT NULL,
                                    used       BOOLEAN           NOT NULL DEFAULT FALSE,
                                    created_at TIMESTAMPTZ       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMPTZ       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User Locations Table (PostGIS)
CREATE TABLE user_locations (
                                id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
                                user_id         UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                location        geography(Point, 4326) NOT NULL,
                                accuracy_meters FLOAT,
                                recorded_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- === INDEXES ===
CREATE INDEX idx_users_email ON users (email) WHERE email IS NOT NULL;
CREATE INDEX idx_users_phone ON users (phone_number) WHERE phone_number IS NOT NULL;
CREATE INDEX idx_auth_methods_user_id ON user_auth_methods (user_id);
-- Partial index for active codes (optimizes verification lookups)
CREATE INDEX idx_codes_active_lookup ON verification_codes (target, type) WHERE used = FALSE;
CREATE INDEX idx_user_locations_gist ON user_locations USING GIST(location);

-- === TRIGGERS ===
CREATE TRIGGER trg_users_upd BEFORE UPDATE ON users FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_user_roles_upd BEFORE UPDATE ON user_roles FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_auth_methods_upd BEFORE UPDATE ON user_auth_methods FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_codes_upd BEFORE UPDATE ON verification_codes FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_locations_upd BEFORE UPDATE ON user_locations FOR EACH ROW EXECUTE PROCEDURE update_modified_column();

-- === INITIAL DATA SEEDING ===
INSERT INTO roles (name) VALUES
                             ('ADMIN'), ('OWNER'), ('MANAGER'), ('HOSTESS'), ('USER'), ('GUEST')
    ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE
v_admin_id UUID;
    v_role_id  INT;
BEGIN
    -- 1. Create Admin User (Corrected RETURNING clause)
INSERT INTO users (email, full_name, status)
VALUES ('admin@tuttime.com', 'Administrator', 'ACTIVE')
    ON CONFLICT (email) DO UPDATE SET full_name = EXCLUDED.full_name
                               RETURNING id INTO v_admin_id;

-- 2. Assign ADMIN Role (Corrected column names)
SELECT id INTO v_role_id FROM roles WHERE name = 'ADMIN';

INSERT INTO user_roles (user_id, role_id)
VALUES (v_admin_id, v_role_id)
    ON CONFLICT DO NOTHING;

-- 3. Set Auth Credentials (Corrected logic)
INSERT INTO user_auth_methods (user_id, auth_type, credential, verified)
VALUES (v_admin_id, 'EMAIL_PASS', crypt('password', gen_salt('bf')), TRUE)
    ON CONFLICT (user_id, auth_type) DO NOTHING;
END $$;
