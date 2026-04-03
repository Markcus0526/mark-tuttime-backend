-- ============================================================================
-- PAYMENT SERVICE - Unified Database Schema (PostgreSQL)
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS payment_service;
SET
search_path TO payment_service, public;

-- Enable UUID extension
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Payments Table
-- Handles initial Stripe Checkout Sessions and resulting Payment Intents
CREATE TABLE payments
(
    id                  UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    booking_id          UUID           NOT NULL, -- Matches Booking Service UUID
    user_id             UUID           NOT NULL, -- For cross-service verification

    -- Stripe Identifiers
    stripe_session_id   VARCHAR(255) UNIQUE,     -- ID from Checkout Session creation
    payment_intent_id   VARCHAR(255) UNIQUE,     -- ID returned after user completes payment

    -- Transaction Details
    amount              NUMERIC(10, 2) NOT NULL,
    currency            VARCHAR(10)    NOT NULL DEFAULT 'USD',
    payment_method_type VARCHAR(50),             -- e.g., 'card', 'apple_pay'
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    -- Statuses: PENDING, COMPLETED, FAILED, EXPIRED

    -- Metadata & Receipts
    receipt_url         VARCHAR(500),
    failure_message     TEXT,

    created_at          TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP
);

-- 2. Refunds Table
CREATE TABLE refunds
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id       UUID           NOT NULL REFERENCES payments (id) ON DELETE CASCADE,
    stripe_refund_id VARCHAR(255)   NOT NULL UNIQUE,
    amount           NUMERIC(10, 2) NOT NULL,
    status           VARCHAR(20)    NOT NULL, -- SUCCEEDED, FAILED, PENDING
    reason           VARCHAR(255),
    created_at       TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

-- 3. Webhook Idempotency & Audit Log
-- Crucial for handling Stripe Webhook retries and Apple Pay events
CREATE TABLE payment_audit_logs
(
    id              BIGSERIAL PRIMARY KEY,
    payment_id      UUID         REFERENCES payments (id) ON DELETE SET NULL,
    stripe_event_id VARCHAR(255) UNIQUE,   -- PREVENTS DUPLICATE PROCESSING
    event_type      VARCHAR(100) NOT NULL, -- e.g., checkout.session.completed
    event_data      JSONB,                 -- Full raw Stripe payload for debugging
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Performance & Reliability Indices
CREATE INDEX idx_payments_booking_id ON payments (booking_id);
CREATE INDEX idx_payments_session_id ON payments (stripe_session_id);
CREATE INDEX idx_audit_event_id ON payment_audit_logs (stripe_event_id);
CREATE INDEX idx_audit_created_at ON payment_audit_logs (created_at);

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
