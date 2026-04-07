-- ============================================================================
-- POS SERVICE - Adjusted Database Schema
-- Optimized for Spring Boot 3.3.13
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS pos_service;
SET
search_path TO pos_service, public;

SET
timezone = 'UTC';
SET
client_encoding = 'UTF8';
SET
client_min_messages TO WARNING;

CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- Enums
CREATE TYPE pos_direction AS ENUM ('TO_POS', 'FROM_POS');
CREATE TYPE pos_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED');
CREATE TYPE sync_task_status AS ENUM ('PENDING', 'PROCESSING', 'FAILED', 'COMPLETED');

-- Table: pos_system_configs
CREATE TABLE pos_system_configs
(
    id             SERIAL PRIMARY KEY,
    pos_system     VARCHAR(50) UNIQUE NOT NULL,
    api_endpoint   TEXT               NOT NULL,
    webhook_secret TEXT,
    enabled        BOOLEAN            NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ                 DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ                 DEFAULT CURRENT_TIMESTAMP
);

-- Table: pos_logs
CREATE TABLE pos_logs
(
    id               SERIAL PRIMARY KEY,
    pos_system_id    INT         NOT NULL REFERENCES pos_system_configs (id),
    entity_type      VARCHAR(50) NOT NULL,
    entity_id        UUID        NOT NULL, -- Matched to Order/Booking UUIDs
    status           VARCHAR(20) NOT NULL,
    attempt_count    INT         NOT NULL DEFAULT 0,
    last_attempt     TIMESTAMPTZ,
    request_payload  JSONB,                -- Added to store Map<String, Object>
    response_payload JSONB,                -- Added to store POS API response
    created_at       TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pos_logs_entity ON pos_logs (entity_type, entity_id);
CREATE INDEX idx_pos_logs_status ON pos_logs (status);

-- Table: pos_error_details
CREATE TABLE pos_error_details
(
    id               SERIAL PRIMARY KEY,
    pos_log_id       INT NOT NULL REFERENCES pos_logs (id) ON DELETE CASCADE,
    error_code       VARCHAR(100),
    error_stacktrace TEXT,
    created_at       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Table: pos_sync_queues
CREATE TABLE pos_sync_queues
(
    id            SERIAL PRIMARY KEY,
    pos_log_id    INT         NOT NULL REFERENCES pos_logs (id) ON DELETE CASCADE,
    next_retry_at TIMESTAMPTZ NOT NULL,
    retry_count   INT         NOT NULL DEFAULT 0,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pos_sync_queues_status ON pos_sync_queues (status);
CREATE INDEX idx_pos_sync_queues_next_retry ON pos_sync_queues (next_retry_at);

CREATE TABLE sync_logs
(
    id            SERIAL PRIMARY KEY,
    order_id      UUID        NOT NULL,
    restaurant_id UUID        NOT NULL,
    status        VARCHAR(50) NOT NULL,
    event_type    VARCHAR(100),
    metadata      JSONB,
    created_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for performance
CREATE INDEX idx_sync_logs_order_id ON sync_logs (order_id);
CREATE INDEX idx_sync_logs_restaurant_time ON sync_logs (restaurant_id, created_at DESC);

-- Comments
COMMENT
ON TABLE pos_sync_queues IS 'Queue for managing retry tasks for POS synchronization.';
COMMENT
ON COLUMN pos_sync_queues.pos_log_id IS 'Foreign key linking to pos_logs entry';
COMMENT
ON COLUMN pos_sync_queues.next_retry_at IS 'Scheduled time for next retry attempt';

-- Triggers
CREATE
OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at
= now();
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trg_update_pos_logs_updated_at
    BEFORE UPDATE
    ON pos_logs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_update_pos_sync_queues_updated_at
    BEFORE UPDATE
    ON pos_sync_queues
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_update_pos_system_configs_updated_at
    BEFORE UPDATE
    ON pos_system_configs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
