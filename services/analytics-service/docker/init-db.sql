-- ============================================================================
-- ANALYTICS SERVICE - Database Schema
-- Database: analytics_service_db
-- Schema: analytics_service
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS analytics_service;
SET
search_path TO analytics_service, public;

-- Set timezone to UTC for all timestamp operations
SET
timezone = 'UTC';

-- Set client encoding to UTF-8 to handle Unicode characters
SET
client_encoding = 'UTF8';

-- Suppress NOTICE messages
SET
client_min_messages TO WARNING;

-- Set search_path after schema is created
SET
search_path TO analytics_service, public;

-- Enable required PostgreSQL extensions
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

CREATE SCHEMA IF NOT EXISTS analytics_service;
SET
search_path TO analytics_service, public;

-- TutTime Analytics Service - Full Support Database Schema
-- Optimized for Spring Boot 3.3.13, gRPC, and Kafka

CREATE SCHEMA IF NOT EXISTS analytics_service;

-- 1. CENTRAL EVENT LOGS (Fact Table)
CREATE TABLE system_events
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id      UUID         NOT NULL,
    user_id        UUID,
    event_type     VARCHAR(100) NOT NULL,
    service_source VARCHAR(50),
    payload        JSONB        NOT NULL,
    occurred_at    TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. BRANCH YIELD MANAGEMENTS (For gRPC Discount Engine)
CREATE TABLE branch_yield_managements
(
    branch_id                UUID        NOT NULL,
    time_slot                TIMESTAMPTZ NOT NULL,
    total_capacity           INT         NOT NULL,
    current_bookings         INT DEFAULT 0,
    occupancy_rate           DECIMAL(3, 2),
    demand_level             VARCHAR(10),
    recommended_discount_pct FLOAT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (branch_id, time_slot)
);

-- 3. USER RELIABILITY INDEXES (For Anti-No-Show Logic)
CREATE TABLE user_reliability_indexes
(
    user_id           UUID PRIMARY KEY,
    reliability_score INT         DEFAULT 50,
    total_bookings    INT         DEFAULT 0,
    no_show_count     INT         DEFAULT 0,
    late_cancel_count INT         DEFAULT 0,
    risk_level        VARCHAR(20) DEFAULT 'STANDARD',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. FINANCIAL LEDGERS (Unit Economics)
CREATE TABLE financial_ledgers
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id             UUID NOT NULL,
    report_date           DATE NOT NULL,
    order_count INT NOT NULL DEFAULT 0,
    total_gmv             DECIMAL(15, 2)   DEFAULT 0,
    commission_earned     DECIMAL(15, 2)   DEFAULT 0,
    payout_due_restaurant DECIMAL(15, 2)   DEFAULT 0,
    refund_volume         DECIMAL(15, 2)   DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (branch_id, report_date)
);

-- 5. BRANCH PERFORMANCE STATS (POS & Kitchen Health)
CREATE TABLE branch_performance_stats
(
    branch_id           UUID PRIMARY KEY,
    avg_prep_time_sec   INT,
    pos_sync_health_pct DECIMAL(5, 2),
    avg_guest_rating    DECIMAL(3, 2),
    popular_dish_id     UUID,
    last_event_id       UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- PERFORMANCE INDEXES (Ensures gRPC sub-millisecond response)
CREATE INDEX idx_analytics_events_time ON system_events (occurred_at DESC);
CREATE INDEX idx_yield_lookup ON branch_yield_managements (branch_id, demand_level);
CREATE INDEX idx_user_risk_score ON user_reliability_indexes (reliability_score);