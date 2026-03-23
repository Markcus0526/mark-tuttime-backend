-- ============================================================================
-- ADMIN SERVICE - Database Schema
-- Database: restaurant_service_db
-- Schema: restaurant_service
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS restaurant_service;
SET search_path TO restaurant_service, public;

-- Set timezone to UTC for all timestamp operations
SET timezone = 'UTC';

-- Set client encoding to UTF-8 to handle Unicode characters
SET client_encoding = 'UTF8';

-- Suppress NOTICE messages
SET client_min_messages TO WARNING;

-- Create schema
CREATE SCHEMA IF NOT EXISTS restaurant_service;

-- Set search_path after schema is created
SET search_path TO restaurant_service, public;

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

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

CREATE TYPE warning_category AS ENUM (
    'MALICIOUS',
    'SUSPICIOUS',
    'BRUTE_FORCE',
    'SYSTEM_STABILITY',
    'FRAUD',
    'MULTI_ACCOUNT',
    'MULTI_DEVICE',
    'UNAUTHORIZED_ACCESS',
    'RATE_LIMIT_EXCEEDED',
    'OTHER'
);

-- ============================================================================
-- CORE TABLES
-- ============================================================================

-- Admin Users Table
CREATE TABLE IF NOT EXISTS restaurants (
    restaurant_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    telegram VARCHAR(100),
    instagram VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    country VARCHAR(100),
    city VARCHAR(100),
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'en',
    about_image_url VARCHAR(500),
    about_image_thumbnail_url VARCHAR(500),
    about_image_small_url VARCHAR(500),
    about_image_medium_url VARCHAR(500),
    about TEXT,
    websites TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    registration_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active TIMESTAMPTZ,
    last_login TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_reason TEXT,
    blocked_at TIMESTAMPTZ,
    is_paused BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    -- Location fields for map feature
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),

    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_name_length CHECK (char_length(name) >= 3),
    CONSTRAINT chk_latitude_range CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    CONSTRAINT chk_longitude_range CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180)),
    );

-- Admin Audit Logs Table
CREATE TABLE IF NOT EXISTS restaurant_audit_logs (
    log_id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT,
    restaurant_username VARCHAR(100),
    action_type restaurant_action_type NOT NULL,
    resource_type VARCHAR(50),
    resource_id BIGINT,
    description TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_method VARCHAR(10),
    request_path VARCHAR(500),
    request_body TEXT,
    response_status INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Reserve Alarms Table
CREATE TABLE IF NOT EXISTS reserve_alarms (
    alarm_id BIGSERIAL PRIMARY KEY,
    network VARCHAR(50),
    alarm_type reserve_alarm_type NOT NULL,
    status reserve_alarm_status NOT NULL DEFAULT 'ACTIVE',
    threshold_value NUMERIC(36, 18) NOT NULL,
    comparison_operator VARCHAR(10) NOT NULL DEFAULT 'LT', -- LT (less than), GT (greater than), EQ (equal)
    metric_type VARCHAR(50) NOT NULL, -- BALANCE, PRICE, SUPPLY, NAV
    description TEXT,
    triggered_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_comparison_operator CHECK (comparison_operator IN ('LT', 'GT', 'EQ', 'LTE', 'GTE'))
    );

-- ============================================================================
-- INDEXES - Basic Indexes
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_restaurants_name ON restaurants(name);
CREATE INDEX IF NOT EXISTS idx_restaurants_email ON restaurant_users(email);
CREATE INDEX IF NOT EXISTS idx_restaurants_is_active ON restaurant_users(is_active);

CREATE INDEX IF NOT EXISTS idx_restaurant_audit_logs_restaurant_id ON restaurant_audit_logs(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_restaurant_audit_logs_action_type ON restaurant_audit_logs(action_type);
CREATE INDEX IF NOT EXISTS idx_restaurant_audit_logs_resource ON restaurant_audit_logs(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_restaurant_audit_logs_created_at ON restaurant_audit_logs(created_at DESC);

-- Languages Table
CREATE TABLE IF NOT EXISTS languages (
    language_id BIGSERIAL PRIMARY KEY,
    language_code VARCHAR(10) NOT NULL UNIQUE, -- e.g., 'en', 'ko', 'ja', 'zh'
    language_name VARCHAR(100) NOT NULL, -- e.g., 'English', 'Korean', 'Japanese', 'Chinese'
    native_name VARCHAR(100), -- e.g., 'English', '한국어', '日本語', '中文'
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_default BOOLEAN NOT NULL DEFAULT false,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,

    CONSTRAINT chk_language_code_format CHECK (language_code ~ '^[a-z]{2}(-[A-Z]{2})?$')
    -- Note: Only one default language constraint is enforced at application level
    -- PostgreSQL doesn't allow subqueries in check constraints
    );

-- System Configuration Table (Singleton - only one row with config_id = 1)
-- Note: This table now includes all mining formula configuration fields
-- (previously in mining_formula_config table which has been merged)
CREATE TABLE IF NOT EXISTS system_config (
    config_id BIGINT PRIMARY KEY DEFAULT 1, -- Singleton - always use ID 1

    -- Mining Formula Parameters (merged from mining_formula_config)
    -- Base Speed Parameters
    base_speed_unverified NUMERIC(10, 4) NOT NULL DEFAULT 0.3, -- AP/h for unverified users
    base_speed_verified NUMERIC(10, 4) NOT NULL DEFAULT 1.0, -- AP/h for KYC verified users

-- Legacy mining speed field (deprecated - use base_speed_verified instead)
    base_mining_speed NUMERIC(10, 3),

    -- Friend Bonus Parameters
    friend_bonus_per_friend NUMERIC(10, 4) NOT NULL DEFAULT 0.5, -- AP/h per active friend
    max_friends INTEGER NOT NULL DEFAULT 10, -- Maximum number of friends that count
    max_friend_bonus NUMERIC(10, 4) NOT NULL DEFAULT 5.0, -- Maximum friend bonus (10 * 0.5)

-- Referral Boost Parameters
    referral_boost_per_invitation NUMERIC(10, 4) NOT NULL DEFAULT 0.5, -- AP/h per invited user

-- Booking Bonus Parameters
    booking_bonus_per_booking NUMERIC(10, 4) NOT NULL DEFAULT 2.0, -- AP/h per active booking
    booking_boost_duration_days INTEGER NOT NULL DEFAULT 15, -- Duration of booking boost

-- Legacy booking boost fields (deprecated)
    booking_boost_base NUMERIC(10, 3),

    -- Speed Cap
    max_mining_speed NUMERIC(10, 4) NOT NULL DEFAULT 27777.0, -- Maximum mining speed cap

-- Mining Interval Parameters
    min_mining_interval_minutes INTEGER NOT NULL DEFAULT 1, -- Minimum mining interval
    max_mining_interval_minutes INTEGER NOT NULL DEFAULT 5, -- Maximum mining interval

-- Monthly Pool
    monthly_pool_amount NUMERIC(36, 18) NOT NULL DEFAULT 50000000000.0, -- 50B APARTs

-- Metadata
    description TEXT, -- Description of current configuration
    is_active BOOLEAN NOT NULL DEFAULT true, -- Whether this configuration is active

-- Rewards Boost Parameters (other)
    max_referral_boosts INTEGER,

    -- Rental Boost Parameters
    rental_boost_multiplier NUMERIC(10, 3),
    rental_boost_duration_days INTEGER,

    -- Extra Parameters
    withdrawal_platform_fee_percent NUMERIC(5, 2),
    deposit_minimum_amount NUMERIC(36, 18),
    withdrawal_minimum_amount NUMERIC(36, 18),
    withdrawal_maximum_amount NUMERIC(36, 18),
    withdrawal_daily_limit INTEGER,

    -- Legacy fields for backward compatibility
    monthly_pool NUMERIC(36, 18),
    max_referral_bonus INTEGER,
    referral_bonus_per_active NUMERIC(10, 3),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,

    CONSTRAINT chk_config_id_singleton CHECK (config_id = 1),
    CONSTRAINT chk_base_speed_unverified_positive CHECK (base_speed_unverified >= 0),
    CONSTRAINT chk_base_speed_verified_positive CHECK (base_speed_verified >= 0),
    CONSTRAINT chk_friend_bonus_positive CHECK (friend_bonus_per_friend >= 0),
    CONSTRAINT chk_max_friends_positive CHECK (max_friends > 0),
    CONSTRAINT chk_max_friend_bonus_positive CHECK (max_friend_bonus >= 0),
    CONSTRAINT chk_booking_bonus_positive CHECK (booking_bonus_per_booking >= 0),
    CONSTRAINT chk_max_speed_positive CHECK (max_mining_speed > 0),
    CONSTRAINT chk_min_interval_positive CHECK (min_mining_interval_minutes > 0),
    CONSTRAINT chk_max_interval_positive CHECK (max_mining_interval_minutes > 0),
    CONSTRAINT chk_max_interval_gte_min CHECK (max_mining_interval_minutes >= min_mining_interval_minutes),
    CONSTRAINT chk_monthly_pool_positive CHECK (monthly_pool_amount >= 0)
    );

CREATE INDEX IF NOT EXISTS idx_system_config_active ON system_config(is_active) WHERE is_active = true;

-- Insert default languages
-- Note: Using CHR() function with Unicode code points to avoid encoding issues
INSERT INTO languages (language_code, language_name, native_name, is_active, is_default, display_order)
VALUES
    ('en', 'English', 'English', true, true, 1),
    ('ko', 'Korean', CHR(54620) || CHR(44397) || CHR(50612), true, false, 2),  -- 한국어 (U+D55C U+AD6D U+C5B4)
    ('ja', 'Japanese', CHR(26085) || CHR(26412) || CHR(35486), true, false, 3),  -- 日本語 (U+65E5 U+672C U+8A9E)
    ('zh', 'Chinese', CHR(20013) || CHR(25991), true, false, 4),  -- 中文 (U+4E2D U+6587)
    ('es', 'Spanish', 'Espa' || CHR(241) || 'ol', true, false, 5),  -- Español
    ('fr', 'French', 'Fran' || CHR(231) || 'ais', true, false, 6),  -- Français
    ('th', 'Thai', CHR(3652) || CHR(3617) || CHR(3634), true, false, 7),  -- ไทย (U+0E44 U+0E17 U+0E22)
    ('ru', 'Russian', CHR(1056) || CHR(1091) || CHR(1089) || CHR(1089) || CHR(1082) || CHR(1080) || CHR(1081), true, false, 8),  -- Русский
    ('pl', 'Polish', 'Polski', true, false, 9)
    ON CONFLICT (language_code) DO UPDATE
                                       SET
                                           language_name = EXCLUDED.language_name,
                                       native_name = EXCLUDED.native_name,
                                       is_active = EXCLUDED.is_active,
                                       display_order = EXCLUDED.display_order,
                                       updated_at = CURRENT_TIMESTAMP;



-- ============================================================================
-- ANALYZE TABLES - Update Statistics for Query Optimizer
-- ============================================================================

ANALYZE restaurants;
ANALYZE restaurant_audit_logs;
ANALYZE languages;
ANALYZE system_config;

-- ============================================================================
-- PERFORMANCE MONITORING QUERIES (Reference Only - Not Executed)
-- ============================================================================

-- Check index usage:
-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'restaurant_service'
-- ORDER BY idx_scan DESC;

-- Check table sizes:
-- SELECT schemaname, tablename,
--        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
-- FROM pg_tables
-- WHERE schemaname = 'restaurant_service'
-- ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

