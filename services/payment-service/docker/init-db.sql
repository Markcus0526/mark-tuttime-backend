-- ============================================================================
-- PAYMENT SERVICE - Database Schema
-- Database: order_service_db
-- Schema: order_service
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS order_service;
SET
search_path TO order_service, public;

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
search_path TO order_service, public;

-- Enable required PostgreSQL extensions
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE payments
(
    id                BIGSERIAL PRIMARY KEY,
    payment_intent_id VARCHAR(255)   NOT NULL UNIQUE,
    booking_id        BIGINT NULL,
    amount            NUMERIC(10, 2) NOT NULL,
    currency          VARCHAR(10)    NOT NULL,
    payment_method    VARCHAR(50)    NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    description       TEXT NULL,
    receipt_url       VARCHAR(500) NULL
    -- Uncomment and adjust if you have a booking table:
    -- , CONSTRAINT fk_booking FOREIGN KEY (booking_id) REFERENCES booking(id)
);

CREATE INDEX idx_payment_booking_id ON payments (booking_id);
CREATE INDEX idx_payment_status ON payments (status);

CREATE TABLE refunds
(
    id         BIGSERIAL PRIMARY KEY,
    refund_id  VARCHAR(255)   NOT NULL UNIQUE,
    payment_id BIGINT         NOT NULL,
    amount     NUMERIC(10, 2) NOT NULL,
    status     VARCHAR(20)    NOT NULL,
    reason     VARCHAR(255) NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment FOREIGN KEY (payment_id) REFERENCES payment (id) ON DELETE CASCADE
);

CREATE INDEX idx_refund_payment_id ON refunds (payment_id);
CREATE INDEX idx_refund_status ON refunds (status);

CREATE TABLE payment_audit_logs
(
    id         BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_audit FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_payment_id ON payment_audit_logs (payment_id);
CREATE INDEX idx_audit_event_type ON payment_audit_logs (event_type);

-- Optional trigger to automatically update updated_at timestamp on row update:
CREATE
OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at
= NOW();
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trg_payment_updated_at
    BEFORE UPDATE
    ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_refund_updated_at
    BEFORE UPDATE
    ON refunds
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();