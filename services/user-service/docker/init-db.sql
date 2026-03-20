-- ============================================================================
-- USER SERVICE - Database Schema
-- Database: user_service_db
-- Schema: user_service
-- 
-- IMPORTANT: PostGIS must be installed on PostgreSQL server.
-- Docker: Use postgis/postgis:15-3.4-alpine image
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

-- Set timezone to UTC for all timestamp operations
SET timezone = 'UTC';

-- Suppress NOTICE messages for cleaner output
SET client_min_messages TO WARNING;

-- Enable required extensions (must be created BEFORE creating schema)
-- Extensions are created in public schema, then can be used in user_service schema
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- PostGIS extensions - MUST be created in public schema first
-- If PostGIS is not installed, these will fail with "extension does not exist"
-- PostGIS provides geometry and geography types needed for location_point column
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Verify PostGIS installation (will fail if PostGIS is not installed)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'postgis') THEN
        RAISE EXCEPTION 'PostGIS extension is not installed. Please install PostGIS first.';
    END IF;
    
    -- Verify geometry and geography types exist
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'geometry') THEN
        RAISE EXCEPTION 'PostGIS geometry type not found. PostGIS may not be properly installed.';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'geography') THEN
        RAISE EXCEPTION 'PostGIS geography type not found. PostGIS may not be properly installed.';
    END IF;
END $$;

-- Create schema after extensions are created
CREATE SCHEMA IF NOT EXISTS user_service;

-- Set search_path after schema is created
-- IMPORTANT: public schema must be in search_path for PostGIS types (geometry, geography) to be accessible
SET search_path TO user_service, public;

-- ============================================================================
-- ENUMS
-- ============================================================================

CREATE TYPE device_type AS ENUM (
    'iOS',
    'Android',
    'Web'
);

CREATE TYPE user_status_level AS ENUM (
    'BRONZE',
    'GOLD',
    'GOLD_PLUS',
    'PLATINUM',
    'PLATINUM_PLUS',
    'ELITE'
);

CREATE TYPE audit_action AS ENUM (
    'USER_CREATED', 
    'USER_UPDATED', 
    'USER_DELETED',
    'USER_LOGIN',
    'USER_LOGOUT',
    'FRAUD_DETECTED'
);

CREATE TYPE saga_status AS ENUM (
    'PENDING',
    'IN_PROGRESS',
    'COMPLETED',
    'FAILED',
    'COMPENSATING',
    'COMPENSATED'
);

-- ============================================================================
-- CORE TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- users - Core user table (PRIMARY SOURCE OF USER_ID)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    telegram VARCHAR(100),
    instagram VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    country VARCHAR(100),
    city VARCHAR(100),
    date_of_birth DATE,
    gender VARCHAR(20),
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'en',
    profile_image_url VARCHAR(500),
    profile_image_thumbnail_url VARCHAR(500),
    profile_image_small_url VARCHAR(500),
    profile_image_medium_url VARCHAR(500),
    about TEXT,
    websites TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    kyc_completed BOOLEAN NOT NULL DEFAULT FALSE,
    kyc_verified_at TIMESTAMPTZ,
    registration_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active TIMESTAMPTZ,
    last_login TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_reason TEXT,
    blocked_at TIMESTAMPTZ,
    is_paused BOOLEAN NOT NULL DEFAULT FALSE,
    withdrawal_frozen BOOLEAN NOT NULL DEFAULT FALSE,
    anti_fraud_watch BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    -- Location fields for map feature
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    location_point GEOGRAPHY(POINT, 4326),
    location_accuracy INTEGER,
    location_updated_at TIMESTAMPTZ,
    location_sharing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    location_privacy_level VARCHAR(20) NOT NULL DEFAULT 'exact',
    -- Apart-sharing integration fields
    apart_sharing_registered BOOLEAN,
    apart_sharing_verified BOOLEAN,
    apart_sharing_status VARCHAR(50),
    apart_sharing_status_checked_at TIMESTAMPTZ,
    apart_sharing_status_data JSONB,

    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_username_length CHECK (char_length(username) >= 3),
    CONSTRAINT chk_latitude_range CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    CONSTRAINT chk_longitude_range CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180)),
    CONSTRAINT chk_location_privacy_level CHECK (location_privacy_level IN ('exact', 'private', 'static')),
    CONSTRAINT chk_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'))
);

-- Indexes for users table
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_unique ON users(email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_unique ON users(username) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_first_name ON users(first_name) WHERE first_name IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_last_name ON users(last_name) WHERE last_name IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone) WHERE phone IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_kyc_completed ON users(kyc_completed) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active, last_active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_registration_date ON users(registration_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_country_city_location ON users(country, city, location_sharing_enabled) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_gender ON users(gender) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_preferred_language ON users(preferred_language) WHERE preferred_language IS NOT NULL AND deleted_at IS NULL;

-- Spatial indexes for location queries
CREATE INDEX IF NOT EXISTS idx_users_location_point ON users 
USING GIST(location_point) WHERE location_point IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_location_enabled ON users 
(location_sharing_enabled, location_privacy_level, location_updated_at DESC) 
WHERE location_sharing_enabled = TRUE AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_location_accuracy ON users 
(location_accuracy) WHERE location_accuracy IS NOT NULL AND deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- devices - Device tracking for anti-fraud
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS devices (
    device_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    client_device_id VARCHAR(255),
    device_fingerprint VARCHAR(255) NOT NULL,
    device_type device_type NOT NULL DEFAULT 'Web',
    device_model VARCHAR(100),
    os_version VARCHAR(50),
    app_version VARCHAR(50),
    ip_address INET,
    fcm_token VARCHAR(500),
    last_seen TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_trusted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_user_device UNIQUE(user_id, device_fingerprint)
);

CREATE INDEX IF NOT EXISTS idx_devices_user_id ON devices(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_devices_fingerprint ON devices(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_devices_ip_address ON devices(ip_address) WHERE ip_address IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_devices_last_seen ON devices(last_seen DESC) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_devices_client_device_id ON devices(client_device_id) WHERE client_device_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_devices_client_device_id_unique ON devices(client_device_id) WHERE client_device_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_devices_fcm_token ON devices(fcm_token) WHERE fcm_token IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_devices_user_id_fcm_token ON devices(user_id, fcm_token) WHERE fcm_token IS NOT NULL AND is_active = TRUE;

-- ----------------------------------------------------------------------------
-- user_blocks - User-to-user blocking relationships
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_blocks (
    block_id BIGSERIAL PRIMARY KEY,
    blocker_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    blocked_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    
    -- Prevent self-blocking at database level
    CONSTRAINT chk_no_self_block CHECK (blocker_user_id != blocked_user_id)
);

-- Unique constraint for active blocks only (PostgreSQL partial unique index)
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_blocks_active 
    ON user_blocks(blocker_user_id, blocked_user_id) 
    WHERE deleted_at IS NULL;

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_user_blocks_blocker 
    ON user_blocks(blocker_user_id) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_user_blocks_blocked 
    ON user_blocks(blocked_user_id) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_user_blocks_bidirectional 
    ON user_blocks(blocker_user_id, blocked_user_id) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_user_blocks_created_at 
    ON user_blocks(created_at DESC) 
    WHERE deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- user_activity_logs - User activity tracking (partitioned)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_activity_logs (
    activity_id BIGSERIAL NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    device_id BIGINT REFERENCES devices(device_id) ON DELETE SET NULL,
    ip_address INET,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB,
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (activity_id, event_timestamp)
) PARTITION BY RANGE (event_timestamp);

-- Partitioning for user_activity_logs (monthly partitions)
-- Create partitions for current and next month dynamically
DO $$
DECLARE
    current_month DATE := DATE_TRUNC('month', CURRENT_DATE);
    next_month DATE := current_month + INTERVAL '1 month';
    partition_name TEXT;
BEGIN
    partition_name := 'user_activity_logs_' || TO_CHAR(current_month, 'YYYY_MM');
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF user_activity_logs FOR VALUES FROM (%L) TO (%L)',
                   partition_name, current_month, next_month);

    partition_name := 'user_activity_logs_' || TO_CHAR(next_month, 'YYYY_MM');
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF user_activity_logs FOR VALUES FROM (%L) TO (%L)',
                   partition_name, next_month, next_month + INTERVAL '1 month');
END $$;

CREATE INDEX IF NOT EXISTS idx_activity_logs_user_id ON user_activity_logs(user_id, event_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_activity_logs_event_type ON user_activity_logs(event_type, event_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_activity_logs_ip_address ON user_activity_logs(ip_address) WHERE ip_address IS NOT NULL;

-- ----------------------------------------------------------------------------
-- user_statuses table removed - status is now managed by token-service
-- Use UserReadModel (user_read_model table) for read operations
-- Use token-service gRPC for real-time status queries
-- ----------------------------------------------------------------------------

-- ----------------------------------------------------------------------------
-- audit_logs - Audit trail (partitioned)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id BIGSERIAL NOT NULL,
    user_id BIGINT REFERENCES users(user_id) ON DELETE SET NULL,
    device_id BIGINT REFERENCES devices(device_id) ON DELETE SET NULL,
    action audit_action NOT NULL,
    details JSONB,
    ip_address INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (audit_id, created_at)
) PARTITION BY RANGE (created_at);

-- Partitioning for audit_logs (monthly partitions)
DO $$
DECLARE
    current_month DATE := DATE_TRUNC('month', CURRENT_DATE);
    next_month DATE := current_month + INTERVAL '1 month';
    partition_name TEXT;
BEGIN
    partition_name := 'audit_logs_' || TO_CHAR(current_month, 'YYYY_MM');
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_logs FOR VALUES FROM (%L) TO (%L)',
                   partition_name, current_month, next_month);

    partition_name := 'audit_logs_' || TO_CHAR(next_month, 'YYYY_MM');
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_logs FOR VALUES FROM (%L) TO (%L)',
                   partition_name, next_month, next_month + INTERVAL '1 month');
END $$;

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action, created_at DESC);

-- ----------------------------------------------------------------------------
-- saga_instances - Saga orchestration state tracking
-- Tracks distributed transaction sagas (e.g., user registration)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saga_instances (
    saga_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    saga_type VARCHAR(100) NOT NULL,
    status saga_status NOT NULL DEFAULT 'PENDING',
    user_id BIGINT,
    correlation_id VARCHAR(255),
    input_data JSONB,
    output_data JSONB,
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    compensated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_saga_instances_type_status ON saga_instances(saga_type, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_saga_instances_user_id ON saga_instances(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_saga_instances_correlation_id ON saga_instances(correlation_id) WHERE correlation_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_saga_instances_status ON saga_instances(status, created_at DESC);

-- ----------------------------------------------------------------------------
-- saga_steps - Individual saga step execution tracking
-- Tracks each step in a saga execution
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saga_steps (
    step_id BIGSERIAL PRIMARY KEY,
    saga_id UUID NOT NULL REFERENCES saga_instances(saga_id) ON DELETE CASCADE,
    step_name VARCHAR(100) NOT NULL,
    step_order INTEGER NOT NULL,
    status saga_status NOT NULL DEFAULT 'PENDING',
    input_data JSONB,
    output_data JSONB,
    error_message TEXT,
    executed_at TIMESTAMPTZ,
    compensated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_saga_step_order UNIQUE(saga_id, step_order)
);

CREATE INDEX IF NOT EXISTS idx_saga_steps_saga_id ON saga_steps(saga_id, step_order);
CREATE INDEX IF NOT EXISTS idx_saga_steps_status ON saga_steps(status, created_at DESC);

-- ----------------------------------------------------------------------------
-- user_read_model - CQRS read model for optimized user queries
-- Denormalized view optimized for read operations
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_read_model (
    user_id BIGINT PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    profile_image_url VARCHAR(500),
    about TEXT,
    websites TEXT,
    instagram VARCHAR(100),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    kyc_completed BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    status_level user_status_level NOT NULL DEFAULT 'BRONZE',
    total_apart_balance NUMERIC(36, 18) NOT NULL DEFAULT 0,
    total_ap_balance NUMERIC(36, 18) NOT NULL DEFAULT 0,
    last_active TIMESTAMPTZ,
    last_login TIMESTAMPTZ,
    registration_date TIMESTAMPTZ NOT NULL,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    -- Location fields for map feature
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    location_point GEOGRAPHY(POINT, 4326),
    location_accuracy INTEGER,
    location_updated_at TIMESTAMPTZ,
    location_sharing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    location_privacy_level VARCHAR(20) NOT NULL DEFAULT 'exact',
    gender VARCHAR(20),
    CONSTRAINT chk_read_model_latitude_range CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    CONSTRAINT chk_read_model_longitude_range CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180)),
    CONSTRAINT chk_read_model_location_privacy_level CHECK (location_privacy_level IN ('exact', 'private', 'static')),
    CONSTRAINT chk_read_model_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'))
);

CREATE INDEX IF NOT EXISTS idx_user_read_model_email ON user_read_model(email);
CREATE INDEX IF NOT EXISTS idx_user_read_model_first_name ON user_read_model(first_name) WHERE first_name IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_read_model_last_name ON user_read_model(last_name) WHERE last_name IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_read_model_status ON user_read_model(status_level, is_active);
CREATE INDEX IF NOT EXISTS idx_user_read_model_kyc ON user_read_model(kyc_completed, is_active);
CREATE INDEX IF NOT EXISTS idx_user_read_model_deleted_at ON user_read_model(deleted_at) WHERE deleted_at IS NULL;
-- Spatial indexes for location queries
CREATE INDEX IF NOT EXISTS idx_user_read_model_location_point ON user_read_model 
USING GIST(location_point) WHERE location_point IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_read_model_location_enabled ON user_read_model 
(location_sharing_enabled, location_privacy_level, location_updated_at DESC) 
WHERE location_sharing_enabled = TRUE;
CREATE INDEX IF NOT EXISTS idx_user_read_model_gender ON user_read_model(gender) WHERE deleted_at IS NULL;

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Auto-update updated_at timestamp function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers to all tables with updated_at column
DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trg_devices_updated_at ON devices;
CREATE TRIGGER trg_devices_updated_at BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trg_user_blocks_updated_at ON user_blocks;
CREATE TRIGGER trg_user_blocks_updated_at BEFORE UPDATE ON user_blocks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trg_saga_instances_updated_at ON saga_instances;
CREATE TRIGGER trg_saga_instances_updated_at BEFORE UPDATE ON saga_instances
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trg_saga_steps_updated_at ON saga_steps;
CREATE TRIGGER trg_saga_steps_updated_at BEFORE UPDATE ON saga_steps
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trg_user_read_model_updated_at ON user_read_model;
CREATE TRIGGER trg_user_read_model_updated_at BEFORE UPDATE ON user_read_model
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Trigger to sync user_read_model when users table changes
CREATE OR REPLACE FUNCTION sync_user_read_model()
RETURNS TRIGGER AS $$
BEGIN
    -- If user is deleted, update read model's deleted_at
    IF NEW.deleted_at IS NOT NULL THEN
        UPDATE user_read_model
        SET deleted_at = NEW.deleted_at,
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = NEW.user_id;
        RETURN NEW;
    END IF;
    
    -- Otherwise, sync all fields including deleted_at (which will be NULL)
    INSERT INTO user_read_model (
        user_id, email, username, first_name, last_name, profile_image_url, about, websites, instagram, is_verified, email_verified, kyc_completed, 
        is_active, is_blocked, registration_date, last_active, last_login,
        latitude, longitude, location_point, location_accuracy, location_updated_at,
        location_sharing_enabled, location_privacy_level, deleted_at, gender
    )
    VALUES (
        NEW.user_id, NEW.email, NEW.username, NEW.first_name, NEW.last_name, NEW.profile_image_url, NEW.about, NEW.websites, NEW.instagram, 
        NEW.is_verified, NEW.email_verified, NEW.kyc_completed, NEW.is_active, NEW.is_blocked, 
        NEW.registration_date, NEW.last_active, NEW.last_login,
        NEW.latitude, NEW.longitude, NEW.location_point, NEW.location_accuracy, 
        NEW.location_updated_at, NEW.location_sharing_enabled, NEW.location_privacy_level,
        NEW.deleted_at, NEW.gender
    )
    ON CONFLICT (user_id) DO UPDATE SET
        email = EXCLUDED.email,
        username = EXCLUDED.username,
        first_name = EXCLUDED.first_name,
        last_name = EXCLUDED.last_name,
        profile_image_url = EXCLUDED.profile_image_url,
        about = EXCLUDED.about,
        websites = EXCLUDED.websites,
        instagram = EXCLUDED.instagram,
        is_verified = EXCLUDED.is_verified,
        email_verified = EXCLUDED.email_verified,
        kyc_completed = EXCLUDED.kyc_completed,
        is_active = EXCLUDED.is_active,
        is_blocked = EXCLUDED.is_blocked,
        last_active = EXCLUDED.last_active,
        last_login = EXCLUDED.last_login,
        latitude = EXCLUDED.latitude,
        longitude = EXCLUDED.longitude,
        location_point = EXCLUDED.location_point,
        location_accuracy = EXCLUDED.location_accuracy,
        location_updated_at = EXCLUDED.location_updated_at,
        location_sharing_enabled = EXCLUDED.location_sharing_enabled,
        location_privacy_level = EXCLUDED.location_privacy_level,
        deleted_at = EXCLUDED.deleted_at,
        gender = EXCLUDED.gender,
        updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_user_read_model ON users;
CREATE TRIGGER trg_sync_user_read_model AFTER INSERT OR UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION sync_user_read_model();

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON SCHEMA user_service IS 'User Service database schema - Primary source of user_id';
COMMENT ON TABLE users IS 'Core user accounts table - PRIMARY SOURCE OF USER_ID for all services';
COMMENT ON COLUMN users.latitude IS 'Latitude coordinate (-90 to 90)';
COMMENT ON COLUMN users.longitude IS 'Longitude coordinate (-180 to 180)';
COMMENT ON COLUMN users.location_point IS 'PostGIS geography point for spatial queries (SRID 4326)';
COMMENT ON COLUMN users.location_accuracy IS 'Location accuracy in meters';
COMMENT ON COLUMN users.location_updated_at IS 'Timestamp when location was last updated';
COMMENT ON COLUMN users.location_sharing_enabled IS 'Whether user has enabled location sharing';
COMMENT ON COLUMN users.location_privacy_level IS 'Privacy level: exact (auto-detect mode), private, or static';
COMMENT ON COLUMN users.preferred_language IS 'User preferred language code (e.g., en, ko, ja, zh) for notification translation';
COMMENT ON TABLE devices IS 'Device tracking for anti-fraud and security';
COMMENT ON COLUMN devices.client_device_id IS 'Client-provided device ID (UUID from mobile app). Used for device tracking and anti-fraud detection.';
COMMENT ON COLUMN devices.fcm_token IS 'Firebase Cloud Messaging (FCM) registration token for push notifications. Updated when user logs in or app refreshes token.';
COMMENT ON TABLE user_blocks IS 'Stores user-to-user blocking relationships. Uses soft delete (deleted_at) for unblocking.';
COMMENT ON COLUMN user_blocks.blocker_user_id IS 'User ID who is blocking';
COMMENT ON COLUMN user_blocks.blocked_user_id IS 'User ID who is being blocked';
COMMENT ON COLUMN user_blocks.deleted_at IS 'Timestamp when block was removed (soft delete for unblocking)';
COMMENT ON TABLE user_activity_logs IS 'User activity tracking (partitioned by month)';
COMMENT ON TABLE audit_logs IS 'Audit trail for user service operations';
COMMENT ON TABLE saga_instances IS 'Saga orchestration state tracking for distributed transactions';
COMMENT ON TABLE saga_steps IS 'Individual saga step execution tracking';
COMMENT ON TABLE user_read_model IS 'CQRS read model - Denormalized view optimized for read operations';
COMMENT ON COLUMN user_read_model.deleted_at IS 'Timestamp when user was soft-deleted. NULL means user is active.';

-- ============================================================================
-- POSTGIS VERIFICATION
-- ============================================================================

-- Verify PostGIS is working correctly
DO $$
DECLARE
    postgis_version TEXT;
    geometry_exists BOOLEAN;
    geography_exists BOOLEAN;
BEGIN
    -- Get PostGIS version
    SELECT PostGIS_version() INTO postgis_version;
    
    -- Check if types exist
    SELECT EXISTS(SELECT 1 FROM pg_type WHERE typname = 'geometry') INTO geometry_exists;
    SELECT EXISTS(SELECT 1 FROM pg_type WHERE typname = 'geography') INTO geography_exists;
    
    -- Log verification results
    RAISE NOTICE 'PostGIS Version: %', postgis_version;
    RAISE NOTICE 'Geometry type exists: %', geometry_exists;
    RAISE NOTICE 'Geography type exists: %', geography_exists;
    
    IF NOT geometry_exists OR NOT geography_exists THEN
        RAISE WARNING 'PostGIS types may not be fully available. Check PostGIS installation.';
    END IF;
END $$;

-- ============================================================================
-- Initialization Complete
-- ============================================================================

SELECT 'User Service database schema initialized successfully!' AS status;
SELECT 'PostGIS is enabled and ready for spatial queries.' AS postgis_status;

