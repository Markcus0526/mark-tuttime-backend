-- ============================================================================
-- Flyway Migration: V1__init_analytics_schema.sql
-- Description: Core Schema Initialization for Analytics Service
-- Database Engine: PostgreSQL
-- ============================================================================

-- 1. EXTENSIONS SETUP (Requires DB Superuser or appropriate cloud privileges)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. CENTRAL EVENT LOGS (Fact Table)
CREATE TABLE IF NOT EXISTS system_events
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id      UUID         NOT NULL,
    user_id        UUID,
    event_type     VARCHAR(100) NOT NULL,
    service_source VARCHAR(50),
    payload        JSONB        NOT NULL,
    occurred_at    TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- 3. BRANCH YIELD MANAGEMENTS (For gRPC Discount Engine)
CREATE TABLE IF NOT EXISTS branch_yield_managements
(
    branch_id                UUID        NOT NULL,
    time_slot                TIMESTAMPTZ NOT NULL,
    total_capacity           INT         NOT NULL,
    current_bookings         INT          DEFAULT 0,
    occupancy_rate           DECIMAL(3, 2),
    demand_level             VARCHAR(10),
    recommended_discount_pct FLOAT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (branch_id, time_slot)
    );

-- 4. USER RELIABILITY INDEXES (For Anti-No-Show Logic)
CREATE TABLE IF NOT EXISTS user_reliability_indexes
(
    user_id           UUID PRIMARY KEY,
    reliability_score INT          DEFAULT 50,
    total_bookings    INT          DEFAULT 0,
    no_show_count     INT          DEFAULT 0,
    late_cancel_count     INT          DEFAULT 0,
    risk_level        VARCHAR(20)  DEFAULT 'STANDARD',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- 5. FINANCIAL LEDGERS (Unit Economics)
CREATE TABLE IF NOT EXISTS financial_ledgers
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id             UUID NOT NULL,
    report_date           DATE NOT NULL,
    order_count           INT  NOT NULL DEFAULT 0,
    total_gmv             DECIMAL(15, 2)   DEFAULT 0,
    commission_earned     DECIMAL(15, 2)   DEFAULT 0,
    payout_due_restaurant DECIMAL(15, 2)   DEFAULT 0,
    refund_volume         DECIMAL(15, 2)   DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (branch_id, report_date)
    );

-- 6. BRANCH PERFORMANCE STATS (POS & Kitchen Health)
CREATE TABLE IF NOT EXISTS branch_performance_stats
(
    branch_id           UUID PRIMARY KEY,
    avg_prep_time_sec   INT,
    pos_sync_health_pct DECIMAL(5, 2),
    avg_guest_rating    DECIMAL(3, 2),
    popular_dish_id     UUID,
    last_event_id       UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- 7. PERFORMANCE INDEXES (Ensures gRPC sub-millisecond response)
CREATE INDEX IF NOT EXISTS idx_analytics_events_time ON system_events (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_yield_lookup ON branch_yield_managements (branch_id, demand_level);
CREATE INDEX IF NOT EXISTS idx_user_risk_score ON user_reliability_indexes (reliability_score);
