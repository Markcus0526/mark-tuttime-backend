-- ============================================================================
-- ADMIN SERVICE - Database Schema
-- Database: admin_service_db
-- Schema: admin_service
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS admin_service;
SET search_path TO admin_service, public;

-- Set timezone to UTC for all timestamp operations
SET timezone = 'UTC';

-- Set client encoding to UTF-8 to handle Unicode characters
SET client_encoding = 'UTF8';

-- Suppress NOTICE messages
SET client_min_messages TO WARNING;

-- Create schema
CREATE SCHEMA IF NOT EXISTS admin_service;

-- Set search_path after schema is created
SET search_path TO admin_service, public;

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUMS
-- ============================================================================

CREATE TYPE admin_role AS ENUM (
    'SUPER_ADMIN',
    'ADMIN',
    'MODERATOR',
    'VIEWER'
);

CREATE TYPE admin_action_type AS ENUM (
    'LOGIN',
    'LOGOUT',
    'LOGIN_FAILED',
    'USER_VIEWED',
    'USER_UPDATED',
    'USER_BLOCKED',
    'USER_UNBLOCKED',
    'USER_DELETED',
    'MINING_BALANCE_ADJUSTED',
    'MINING_SESSION_VIEWED',
    'MINING_SPEED_ADJUSTED',
    'TOKEN_BALANCE_ADJUSTED',
    'TOKEN_TRANSACTION_VIEWED',
    'TOKEN_BURNED',
    'STAKING_VIEWED',
    'STAKING_ADJUSTED',
    'REFERRAL_VIEWED',
    'REFERRAL_CREATED',
    'REFERRAL_ADJUSTED',
    'STATUS_UPDATED',
    'CONVERSION_VIEWED',
    'CONVERSION_TRIGGERED',
    'FRAUD_FLAG_ADDED',
    'FRAUD_FLAG_REMOVED',
    'DEVICE_BLOCKED',
    'IP_BLOCKED',
    'MAP_MARKER_CREATED',
    'MAP_MARKER_UPDATED',
    'MAP_MARKER_DELETED',
    'MINER_RATING_UPDATED',
    'TOKEN_WHITELISTED',
    'TOKEN_BLACKLISTED',
    'CONFIG_UPDATED',
    'EXPORT_DATA',
    'BULK_OPERATION',
    'TOKEN_RESERVE_VIEWED',
    'TOKEN_RESERVE_ALARM_CREATED',
    'TOKEN_RESERVE_ALARM_UPDATED',
    'TOKEN_RESERVE_ALARM_DELETED',
    'LANGUAGE_CREATED',
    'LANGUAGE_UPDATED',
    'LANGUAGE_DELETED',
    'TRANSLATION_UPLOADED',
    'TRANSLATION_UPDATED',
    'STAKING_TIER_CREATED',
    'STAKING_TIER_UPDATED',
    'STAKING_TIER_DELETED',
    'STAKING_TIERS_REORDERED',
    'STATUS_LEVEL_CREATED',
    'STATUS_LEVEL_UPDATED',
    'STATUS_LEVEL_DELETED',
    'NOTIFICATION_VIEWED',
    'NOTIFICATION_CREATED',
    'NOTIFICATION_UPDATED',
    'NOTIFICATION_DELETED',
    'NOTIFICATION_PUBLISHED',
    'NOTIFICATION_CANCELLED',
    'NOTIFICATION_SCHEDULED',
    'TASK_VIEWED',
    'TASK_CREATED',
    'TASK_UPDATED',
    'TASK_DELETED',
    'TASK_ACTIVATED',
    'TASK_DEACTIVATED',
    'AD_VIEWED',
    'AD_CREATED',
    'AD_UPDATED',
    'AD_DELETED',
    'AD_APPROVED',
    'AD_REJECTED',
    'VESTING_POOL_VIEWED',
    'VESTING_POOL_CREATED',
    'VESTING_POOL_UPDATED',
    'VESTING_POOL_BALANCE_VIEWED',
    'VESTING_POOL_FUNDED',
    'VESTING_INVESTOR_VIEWED',
    'VESTING_INVESTOR_ADDED',
    'VESTING_CLAIM_STATUS_VIEWED',
    'VIDEO_VIEWED',
    'VIDEO_CREATED',
    'VIDEO_UPDATED',
    'VIDEO_DELETED',
    'VIDEO_APPROVED',
    'VIDEO_REJECTED',
    'VIDEO_PUBLISHED',
    'VIDEO_SCHEDULED',
    'BOOST_VIEWED',
    'DEVICE_WHITELISTED',
    'IP_WHITELISTED',
    'FRAUD_FLAG_VIEWED',
    'USER_BLOCKLIST_VIEWED',
    'ADDRESS_BLOCKLIST_VIEWED',
    'ADDRESS_WHITELIST_VIEWED',
    'ADDRESS_BLOCKED',
    'ADDRESS_UNBLOCKED',
    'ADDRESS_WHITELISTED',
    'ADDRESS_WHITELIST_REMOVED',
    'ADMIN_USER_VIEWED',
    'ADMIN_USER_CREATED',
    'ADMIN_USER_UPDATED',
    'ADMIN_USER_DELETED',
    'ADMIN_USER_ACTIVATED',
    'ADMIN_USER_DEACTIVATED',
    'ADMIN_USER_UNLOCKED',
    'ADMIN_USER_PASSWORD_CHANGED',
    'ADMIN_USER_PASSWORD_RESET',
    'ADMIN_USER_LOGIN_ATTEMPTS_RESET'
);

CREATE TYPE reserve_alarm_type AS ENUM (
    'LOW_BALANCE',
    'PRICE_DROP',
    'PRICE_INCREASE',
    'HIGH_VOLATILITY',
    'SUPPLY_THRESHOLD',
    'NAV_THRESHOLD'
);

CREATE TYPE reserve_alarm_status AS ENUM (
    'ACTIVE',
    'TRIGGERED',
    'RESOLVED',
    'DISABLED'
);

CREATE TYPE warning_severity AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
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
CREATE TABLE IF NOT EXISTS admin_users (
    admin_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role admin_role NOT NULL DEFAULT 'VIEWER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_login_at TIMESTAMPTZ,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    
    CONSTRAINT chk_failed_attempts_non_negative CHECK (failed_login_attempts >= 0)
);

-- Admin Audit Logs Table
CREATE TABLE IF NOT EXISTS admin_audit_logs (
    log_id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT,
    admin_username VARCHAR(100),
    action_type admin_action_type NOT NULL,
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

-- Token Reserves Table
CREATE TABLE IF NOT EXISTS token_reserves (
    reserve_id BIGSERIAL PRIMARY KEY,
    network VARCHAR(50) NOT NULL,
    treasury_balance NUMERIC(36, 18) NOT NULL DEFAULT 0,
    hot_wallet_balance NUMERIC(36, 18) NOT NULL DEFAULT 0,
    token_price NUMERIC(36, 18),
    total_supply NUMERIC(36, 18),
    nav NUMERIC(36, 18),
    last_updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_token_reserves_network UNIQUE (network)
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

CREATE INDEX IF NOT EXISTS idx_admin_users_username ON admin_users(username);
CREATE INDEX IF NOT EXISTS idx_admin_users_email ON admin_users(email);
CREATE INDEX IF NOT EXISTS idx_admin_users_role ON admin_users(role);
CREATE INDEX IF NOT EXISTS idx_admin_users_is_active ON admin_users(is_active);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_admin_id ON admin_audit_logs(admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_action_type ON admin_audit_logs(action_type);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_resource ON admin_audit_logs(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created_at ON admin_audit_logs(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_token_reserves_network ON token_reserves(network);
CREATE INDEX IF NOT EXISTS idx_token_reserves_last_updated ON token_reserves(last_updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_reserve_alarms_network ON reserve_alarms(network);
CREATE INDEX IF NOT EXISTS idx_reserve_alarms_status ON reserve_alarms(status);
CREATE INDEX IF NOT EXISTS idx_reserve_alarms_type ON reserve_alarms(alarm_type);
CREATE INDEX IF NOT EXISTS idx_reserve_alarms_enabled ON reserve_alarms(is_enabled) WHERE is_enabled = true;
CREATE INDEX IF NOT EXISTS idx_reserve_alarms_active ON reserve_alarms(status, is_enabled) WHERE status = 'ACTIVE' AND is_enabled = true;

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

-- Translations Table
CREATE TABLE IF NOT EXISTS translations (
    translation_id BIGSERIAL PRIMARY KEY,
    language_code VARCHAR(10) NOT NULL,
    translation_key VARCHAR(255) NOT NULL, -- e.g., 'common.welcome', 'error.invalid_input'
    translation_value TEXT NOT NULL, -- The actual translated text
    namespace VARCHAR(100), -- Optional: group translations (e.g., 'common', 'error', 'ui')
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    
    CONSTRAINT fk_translations_language FOREIGN KEY (language_code) 
        REFERENCES languages(language_code) ON DELETE CASCADE,
    CONSTRAINT uq_translations_language_key UNIQUE (language_code, translation_key)
);

-- Staking Tiers Table
CREATE TABLE IF NOT EXISTS staking_tiers (
    tier_id BIGSERIAL PRIMARY KEY,
    tier_name VARCHAR(100) NOT NULL, -- e.g., "Bronze", "Silver", "Gold", "Platinum"
    min_stake_amount NUMERIC(36, 18) NOT NULL UNIQUE, -- Minimum APART staked to qualify
    ap_boost_per_hour NUMERIC(10, 3) NOT NULL, -- AP boost per hour (e.g., 10, 20, 35, 50)
    display_order INTEGER NOT NULL DEFAULT 0, -- Order for display (lower = higher priority)
    is_active BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    
    CONSTRAINT chk_min_stake_positive CHECK (min_stake_amount >= 0),
    CONSTRAINT chk_ap_boost_positive CHECK (ap_boost_per_hour >= 0)
);

-- Status Levels Table
CREATE TABLE IF NOT EXISTS status_levels (
    level_id BIGSERIAL PRIMARY KEY,
    level_name VARCHAR(50) NOT NULL UNIQUE, -- e.g., "SILVER", "GOLD", "PLATINUM", "ELITE", "XXX"
    min_apart_balance NUMERIC(36, 18) NOT NULL UNIQUE, -- Minimum APART balance to qualify
    ap_boost_per_hour NUMERIC(10, 3) NOT NULL, -- AP boost per hour (e.g., 2, 3, 5, 7.77, 10)
    includes_pro_auto_mining BOOLEAN NOT NULL DEFAULT false, -- Whether this level includes pro auto-mining
    display_order INTEGER NOT NULL DEFAULT 0, -- Order for display (lower = higher priority)
    is_active BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    
    CONSTRAINT chk_min_balance_positive CHECK (min_apart_balance >= 0),
    CONSTRAINT chk_ap_boost_positive_status CHECK (ap_boost_per_hour >= 0)
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

-- Fraud Flags Table
CREATE TABLE IF NOT EXISTS fraud_flags (
    flag_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT, -- References user_service.users.user_id (no FK)
    flag_type VARCHAR(50) NOT NULL, -- MULTI_DEVICE, MULTI_ACCOUNT_IP, SUSPICIOUS_ACTIVITY, etc.
    severity warning_severity NOT NULL DEFAULT 'MEDIUM',
    category warning_category NOT NULL DEFAULT 'SUSPICIOUS',
    description TEXT,
    device_id VARCHAR(255),
    ip_address VARCHAR(45),
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    resolved_by BIGINT, -- References admin_users.admin_id (no FK)
    metadata JSONB, -- Additional fraud detection metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_flag_type_not_empty CHECK (flag_type IS NOT NULL AND flag_type != '')
);

CREATE INDEX IF NOT EXISTS idx_fraud_flags_user_id ON fraud_flags(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_fraud_flags_category ON fraud_flags(category);
CREATE INDEX IF NOT EXISTS idx_fraud_flags_severity ON fraud_flags(severity);
CREATE INDEX IF NOT EXISTS idx_fraud_flags_resolved ON fraud_flags(resolved) WHERE resolved = FALSE;
CREATE INDEX IF NOT EXISTS idx_fraud_flags_device_id ON fraud_flags(device_id) WHERE device_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_fraud_flags_ip_address ON fraud_flags(ip_address) WHERE ip_address IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_fraud_flags_created_at ON fraud_flags(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_flags_unresolved ON fraud_flags(resolved, created_at DESC) WHERE resolved = FALSE;

-- App Version Table
CREATE TABLE IF NOT EXISTS app_version (
    version_id BIGSERIAL PRIMARY KEY,
    android_version VARCHAR(50) NOT NULL,
    android_install_link TEXT NOT NULL,
    ios_version VARCHAR(50) NOT NULL,
    ios_install_link TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT
);

-- Create unique partial index to ensure only one active version at a time
CREATE UNIQUE INDEX IF NOT EXISTS uk_app_version_active ON app_version(is_active) WHERE is_active = TRUE;

-- Create other indexes
CREATE INDEX IF NOT EXISTS idx_app_version_updated_at ON app_version(updated_at DESC);

-- Insert default system configuration with mining formula defaults
-- This will insert if the row doesn't exist, or update NULL values with defaults if it does
INSERT INTO system_config (
    config_id,
    base_speed_unverified,
    base_speed_verified,
    base_mining_speed,
    friend_bonus_per_friend,
    max_friends,
    max_friend_bonus,
    referral_boost_per_invitation,
    booking_bonus_per_booking,
    booking_boost_duration_days,
    booking_boost_base,
    max_mining_speed,
    min_mining_interval_minutes,
    max_mining_interval_minutes,
    monthly_pool_amount,
    max_referral_boosts,
    rental_boost_multiplier,
    rental_boost_duration_days,
    withdrawal_platform_fee_percent,
    deposit_minimum_amount,
    withdrawal_minimum_amount,
    withdrawal_maximum_amount,
    withdrawal_daily_limit,
    monthly_pool,
    max_referral_bonus,
    referral_bonus_per_active,
    description,
    is_active
)
VALUES (
    1,
    0.3,    -- base_speed_unverified
    1.0,    -- base_speed_verified
    1.0,    -- base_mining_speed (legacy, set to same as base_speed_verified)
    0.5,    -- friend_bonus_per_friend
    10,     -- max_friends
    5.0,    -- max_friend_bonus
    0.5,    -- referral_boost_per_invitation
    2.0,    -- booking_bonus_per_booking
    15,     -- booking_boost_duration_days
    2.0,    -- booking_boost_base (legacy, set to same as booking_bonus_per_booking)
    27777.0, -- max_mining_speed
    1,      -- min_mining_interval_minutes
    5,      -- max_mining_interval_minutes
    50000000000.0, -- monthly_pool_amount (50B APARTs)
    10,     -- max_referral_boosts
    1.5,    -- rental_boost_multiplier
    30,     -- rental_boost_duration_days
    0.0,    -- withdrawal_platform_fee_percent
    0.0,    -- deposit_minimum_amount
    0.0,    -- withdrawal_minimum_amount
    0.0,    -- withdrawal_maximum_amount
    0,      -- withdrawal_daily_limit
    50000000000.0, -- monthly_pool (legacy, set to same as monthly_pool_amount)
    10,     -- max_referral_bonus (legacy)
    0.5,    -- referral_bonus_per_active (legacy, set to same as referral_boost_per_invitation)
    'Default system configuration with mining formula parameters',
    true    -- is_active
)
ON CONFLICT (config_id) DO UPDATE
SET
    -- Update NULL values with defaults, but preserve existing non-NULL values
    base_speed_unverified = COALESCE(system_config.base_speed_unverified, EXCLUDED.base_speed_unverified),
    base_speed_verified = COALESCE(system_config.base_speed_verified, EXCLUDED.base_speed_verified),
    base_mining_speed = COALESCE(system_config.base_mining_speed, EXCLUDED.base_mining_speed),
    friend_bonus_per_friend = COALESCE(system_config.friend_bonus_per_friend, EXCLUDED.friend_bonus_per_friend),
    max_friends = COALESCE(system_config.max_friends, EXCLUDED.max_friends),
    max_friend_bonus = COALESCE(system_config.max_friend_bonus, EXCLUDED.max_friend_bonus),
    referral_boost_per_invitation = COALESCE(system_config.referral_boost_per_invitation, EXCLUDED.referral_boost_per_invitation),
    booking_bonus_per_booking = COALESCE(system_config.booking_bonus_per_booking, EXCLUDED.booking_bonus_per_booking),
    booking_boost_duration_days = COALESCE(system_config.booking_boost_duration_days, EXCLUDED.booking_boost_duration_days),
    booking_boost_base = COALESCE(system_config.booking_boost_base, EXCLUDED.booking_boost_base),
    max_mining_speed = COALESCE(system_config.max_mining_speed, EXCLUDED.max_mining_speed),
    min_mining_interval_minutes = COALESCE(system_config.min_mining_interval_minutes, EXCLUDED.min_mining_interval_minutes),
    max_mining_interval_minutes = COALESCE(system_config.max_mining_interval_minutes, EXCLUDED.max_mining_interval_minutes),
    monthly_pool_amount = COALESCE(system_config.monthly_pool_amount, EXCLUDED.monthly_pool_amount),
    max_referral_boosts = COALESCE(system_config.max_referral_boosts, EXCLUDED.max_referral_boosts),
    rental_boost_multiplier = COALESCE(system_config.rental_boost_multiplier, EXCLUDED.rental_boost_multiplier),
    rental_boost_duration_days = COALESCE(system_config.rental_boost_duration_days, EXCLUDED.rental_boost_duration_days),
    withdrawal_platform_fee_percent = COALESCE(system_config.withdrawal_platform_fee_percent, EXCLUDED.withdrawal_platform_fee_percent),
    deposit_minimum_amount = COALESCE(system_config.deposit_minimum_amount, EXCLUDED.deposit_minimum_amount),
    withdrawal_minimum_amount = COALESCE(system_config.withdrawal_minimum_amount, EXCLUDED.withdrawal_minimum_amount),
    withdrawal_maximum_amount = COALESCE(system_config.withdrawal_maximum_amount, EXCLUDED.withdrawal_maximum_amount),
    withdrawal_daily_limit = COALESCE(system_config.withdrawal_daily_limit, EXCLUDED.withdrawal_daily_limit),
    monthly_pool = COALESCE(system_config.monthly_pool, EXCLUDED.monthly_pool),
    max_referral_bonus = COALESCE(system_config.max_referral_bonus, EXCLUDED.max_referral_bonus),
    referral_bonus_per_active = COALESCE(system_config.referral_bonus_per_active, EXCLUDED.referral_bonus_per_active),
    description = COALESCE(system_config.description, EXCLUDED.description),
    is_active = COALESCE(system_config.is_active, EXCLUDED.is_active),
    updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- INDEXES - Additional Indexes for 10M User Scale Optimization
-- ============================================================================

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_admin_users_active_role 
    ON admin_users(is_active, role) 
    WHERE is_active = true;

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_admin_action 
    ON admin_audit_logs(admin_id, action_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_resource_lookup 
    ON admin_audit_logs(resource_type, resource_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_languages_code ON languages(language_code);
CREATE INDEX IF NOT EXISTS idx_languages_active ON languages(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_languages_default ON languages(is_default) WHERE is_default = true;

CREATE INDEX IF NOT EXISTS idx_translations_language_code ON translations(language_code);
CREATE INDEX IF NOT EXISTS idx_translations_key ON translations(translation_key);
CREATE INDEX IF NOT EXISTS idx_translations_namespace ON translations(namespace);
CREATE INDEX IF NOT EXISTS idx_translations_language_key ON translations(language_code, translation_key);

CREATE INDEX IF NOT EXISTS idx_staking_tiers_min_stake ON staking_tiers(min_stake_amount);
CREATE INDEX IF NOT EXISTS idx_staking_tiers_active ON staking_tiers(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_staking_tiers_display_order ON staking_tiers(display_order);

CREATE INDEX IF NOT EXISTS idx_status_levels_name ON status_levels(level_name);
CREATE INDEX IF NOT EXISTS idx_status_levels_min_balance ON status_levels(min_apart_balance);
CREATE INDEX IF NOT EXISTS idx_status_levels_active ON status_levels(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_status_levels_display_order ON status_levels(display_order);

-- Partial indexes for filtered queries
CREATE INDEX IF NOT EXISTS idx_admin_users_active_only 
    ON admin_users(admin_id) 
    WHERE is_active = true;

-- Note: Partial index on recent records removed because NOW() is not IMMUTABLE
-- PostgreSQL requires functions in index predicates to be IMMUTABLE
-- Use idx_admin_audit_logs_created_at (already created above) for date-based queries
-- For recent records, PostgreSQL will efficiently use the DESC index on created_at

-- Index for full-text search (commented out - enable if needed)
-- CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_description_fts 
--     ON admin_audit_logs USING gin(to_tsvector('english', description));

-- ============================================================================
-- INITIAL DATA
-- ============================================================================

-- Create default super admin user (password: admin123 - CHANGE IN PRODUCTION!)
-- Password hash is for "admin123" using BCrypt
INSERT INTO admin_users (username, email, password_hash, role, is_active, created_by)
VALUES (
    'admin',
    'admin@tuttime.com',
    '$2a$10$Jo03LJFq2GalWjtbwpgJpeFTardfToDlYx/G8anaKkKx/1Yy6x4fu', -- admin123
    'SUPER_ADMIN',
    true,
    NULL
) ON CONFLICT (username) DO NOTHING;

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

-- Insert default status levels
-- Note: BRONZE status is implicit (users with balance < 5,000) - no database entry needed
INSERT INTO status_levels (level_name, min_apart_balance, ap_boost_per_hour, includes_pro_auto_mining, display_order, is_active, description)
VALUES
    ('SILVER', 5000, 2.0, false, 1, true, 'Silver / 5k -> +2.0 AP/hr'),
    ('GOLD', 20000, 3.0, false, 2, true, 'Gold / 20k -> +3.0 AP/hr'),
    ('PLATINUM', 50000, 5.0, true, 3, true, 'Platinum / 50k -> +5.0 AP/hr + pro auto-mining'),
    ('ELITE', 100000, 7.77, true, 4, true, 'Elite / 100k -> +7.77 AP/hr + pro auto-mining'),
    ('XXX', 500000, 10.0, true, 5, true, 'XXX -> +10.0 AP/hr + pro auto-mining')
ON CONFLICT (level_name) DO UPDATE
SET
    min_apart_balance = EXCLUDED.min_apart_balance,
    ap_boost_per_hour = EXCLUDED.ap_boost_per_hour,
    includes_pro_auto_mining = EXCLUDED.includes_pro_auto_mining,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- Insert default staking tiers (if needed)
-- Note: Adjust values based on your business requirements
INSERT INTO staking_tiers (tier_name, min_stake_amount, ap_boost_per_hour, display_order, is_active, description)
VALUES
    ('Bronze', 0, 0, 1, true, 'Bronze tier - No minimum stake, no AP boost'),
    ('Silver', 1000, 10, 2, true, 'Silver tier - 1k+ staked -> +10 AP/hr'),
    ('Gold', 5000, 20, 3, true, 'Gold tier - 5k+ staked -> +20 AP/hr'),
    ('Platinum', 10000, 35, 4, true, 'Platinum tier - 10k+ staked -> +35 AP/hr'),
    ('Diamond', 50000, 50, 5, true, 'Diamond tier - 50k+ staked -> +50 AP/hr')
ON CONFLICT (min_stake_amount) DO UPDATE
SET
    tier_name = EXCLUDED.tier_name,
    ap_boost_per_hour = EXCLUDED.ap_boost_per_hour,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- Insert default token reserves for common networks (if needed)
-- Note: These are placeholder values - update with actual initial values
INSERT INTO token_reserves (network, treasury_balance, hot_wallet_balance, token_price, total_supply, nav)
VALUES
    ('ETHEREUM', 0, 0, NULL, NULL, NULL),
    ('BSC', 0, 0, NULL, NULL, NULL),
    ('POLYGON', 0, 0, NULL, NULL, NULL)
ON CONFLICT (network) DO UPDATE
SET
    last_updated_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP;

-- Insert default app version
INSERT INTO app_version (
    android_version,
    android_install_link,
    ios_version,
    ios_install_link,
    is_active
)
VALUES (
    '1.0.0',
    'https://play.google.com/store/apps/details?id=com.tuttime.app',
    '1.0.0',
    'https://apps.apple.com/app/id123456789',
    TRUE
)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- ANALYZE TABLES - Update Statistics for Query Optimizer
-- ============================================================================

ANALYZE admin_users;
ANALYZE admin_audit_logs;
ANALYZE token_reserves;
ANALYZE reserve_alarms;
ANALYZE languages;
ANALYZE translations;
ANALYZE staking_tiers;
ANALYZE status_levels;
ANALYZE system_config;
ANALYZE fraud_flags;
ANALYZE app_version;

-- ============================================================================
-- PERFORMANCE MONITORING QUERIES (Reference Only - Not Executed)
-- ============================================================================

-- Check index usage:
-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'admin_service'
-- ORDER BY idx_scan DESC;

-- Check table sizes:
-- SELECT schemaname, tablename, 
--        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
-- FROM pg_tables
-- WHERE schemaname = 'admin_service'
-- ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

