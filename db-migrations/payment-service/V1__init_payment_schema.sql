-- ============================================================================
-- Flyway Migration: V1__payment_pos_schema.sql
-- Description: Core Schema Initialization for Payment Service
-- Database Engine: PostgreSQL
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS payment_service;
SET
    search_path TO payment_service, public;

-- Enable UUID extension
CREATE
    EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Payments Table
-- Handles initial Yoomoney Checkout Sessions and resulting Payment Intents
CREATE TABLE payments
(
    id                  UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    booking_id          UUID           NOT NULL, -- Matches Booking Service UUID
    user_id             UUID           NOT NULL, -- For cross-service verification
    branch_id           UUID           NOT NULL, -- For cross-service verification

    -- YooKassa Identifiers
    yookassa_payment_id VARCHAR(255) UNIQUE,     -- 'id' from YooKassa response
    idempotency_key     UUID UNIQUE,             -- Crucial for YooKassa API retries

    -- Transaction Details
    amount              NUMERIC(10, 2) NOT NULL,
    currency            VARCHAR(10)    NOT NULL DEFAULT 'RUB',
    payment_method_type VARCHAR(50),             -- e.g., 'card', 'apple_pay'
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    -- Statuses: PENDING, WAITING_FOR_CAPTURE, SUCCEEDED, CANCELED

    -- Confirmation & Metadata
    confirmation_url    VARCHAR(500),            -- The URL your React Native app opens
    failure_message     TEXT,

    created_at          TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP
);

-- 2. Refunds Table
CREATE TABLE refunds
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id       UUID           NOT NULL REFERENCES payments (id) ON DELETE CASCADE,
    yookassa_refund_id VARCHAR(255)   NOT NULL UNIQUE,
    amount           NUMERIC(10, 2) NOT NULL,
    status           VARCHAR(20)    NOT NULL, -- SUCCEEDED, FAILED, PENDING
    reason           VARCHAR(255),
    created_at       TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

-- 3. Webhook Idempotency & Audit Log
CREATE TABLE payment_audit_logs
(
    id         BIGSERIAL PRIMARY KEY,
    payment_id UUID         REFERENCES payments (id) ON DELETE SET NULL,
    event_id   VARCHAR(255) UNIQUE,   -- Maps to YooKassa event notification ID
    event_type VARCHAR(100) NOT NULL, -- payment.succeeded, refund.succeeded, etc.
    event_data JSONB,                 -- Raw YooKassa JSON payload
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_payment_methods
(
    id                   UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id              UUID         NOT NULL,                  -- Link to the User Profile
    payment_method_token VARCHAR(255) NOT NULL,                  -- The 'payment_method.id' from YooKassa (e.g., pm-27...)
    card_type            VARCHAR(50),                            -- e.g., 'Visa', 'MasterCard', 'MIR'
    last4                VARCHAR(4),                             -- Last 4 digits to show on Figma "Settings" page
    expiry_month         VARCHAR(2),
    expiry_year          VARCHAR(4),
    default_method       BOOLEAN                  DEFAULT false, -- Primary card for "Select Payment" page
    active               BOOLEAN                  DEFAULT true,  -- For soft-deletion
    created_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indices for YooKassa
CREATE INDEX idx_payments_booking_id ON payments (booking_id);
CREATE INDEX idx_payments_yoo_id ON payments (yookassa_payment_id);
CREATE INDEX idx_audit_event_id ON payment_audit_logs (event_id);
CREATE INDEX idx_payment_methods_user_id ON user_payment_methods (user_id);

-- Automatic Timestamp Trigger
CREATE
    OR REPLACE FUNCTION update_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
        = NOW();
    RETURN NEW;
END;
$$
    LANGUAGE plpgsql;

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE
    ON payments
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
CREATE TRIGGER trg_refunds_updated_at
    BEFORE UPDATE
    ON refunds
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
CREATE TRIGGER trg_user_payment_methods_updated_at
    BEFORE UPDATE
    ON user_payment_methods
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
