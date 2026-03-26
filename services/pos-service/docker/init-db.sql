-- ============================================================================
-- POS SERVICE - Database Schema
-- Database: pos_service_db
-- Schema: pos_service
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS pos_service;
SET
search_path TO pos_service, public;

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
search_path TO pos_service, public;

-- Enable required PostgreSQL extensions
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- Enum types for controlled vocabularies with uppercase values
CREATE TYPE pos_direction AS ENUM ('TO_POS', 'FROM_POS');
CREATE TYPE pos_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED');
CREATE TYPE sync_task_status AS ENUM ('PENDING', 'PROCESSING', 'FAILED', 'COMPLETED');

CREATE TABLE pos_system_configs
(
    id             SERIAL PRIMARY KEY,
    pos_system     VARCHAR(50) UNIQUE NOT NULL,
    api_endpoint   TEXT               NOT NULL,
    webhook_secret TEXT,
    enabled        BOOLEAN            NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ        NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ        NOT NULL DEFAULT now()
);
-- Table: pos_logs
CREATE TABLE pos_logs
(
    id            SERIAL PRIMARY KEY,
    pos_system_id INT NOT NULL REFERENCES pos_system_configs(id),
    entity_type   VARCHAR(50) NOT NULL, -- e.g., BOOKING, ORDER, MENU
    entity_id     VARCHAR(100) NOT NULL, -- ID in your backend system
    status        VARCHAR(20) NOT NULL, -- e.g., SUCCESS, FAILED, PENDING
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for pos_logs
CREATE INDEX idx_pos_logs_entity ON pos_logs (entity_type, entity_id);
CREATE INDEX idx_pos_logs_status ON pos_logs (status);

CREATE TABLE pos_error_details
(
    id               SERIAL PRIMARY KEY,
    pos_log_id       INT         NOT NULL REFERENCES pos_logs (id) ON DELETE CASCADE,
    error_code       VARCHAR(100),
    error_stacktrace TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Table: pos_sync_queues
CREATE TABLE pos_sync_queues
(
    id             SERIAL PRIMARY KEY,
    pos_log_id     INT NOT NULL REFERENCES pos_logs(id) ON DELETE CASCADE,
    next_retry_at  TIMESTAMPTZ NOT NULL,
    retry_count    INT NOT NULL DEFAULT 0,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT
ON TABLE pos_sync_queues IS 'Queue for managing retry tasks for POS synchronization.';
COMMENT
ON COLUMN pos_sync_queues.pos_log_id IS 'Foreign key linking to pos_logs entry';
COMMENT
ON COLUMN pos_sync_queues.next_attempt_at IS 'Scheduled time for next retry attempt';
COMMENT
ON COLUMN pos_sync_queues.max_attempts IS 'Maximum allowed retry attempts';
COMMENT
ON COLUMN pos_sync_queues.current_attempt IS 'Retry attempts made so far';
COMMENT
ON COLUMN pos_sync_queues.status IS 'Current status of the sync task';

-- Indexes for pos_sync_queues
CREATE INDEX idx_pos_sync_queues_status ON pos_sync_queues (status);
CREATE INDEX idx_pos_sync_queues_next_attempt ON pos_sync_queues (next_attempt_at);

-- Table: pos_system_configs
CREATE TABLE pos_system_configs
(
    id             SERIAL PRIMARY KEY,
    pos_system     VARCHAR(50) UNIQUE NOT NULL,
    api_endpoint   TEXT               NOT NULL,
    webhook_secret TEXT,
    enabled        BOOLEAN            NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ        NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ        NOT NULL DEFAULT now()
);

-- Optional Table: pos_error_details
CREATE TABLE pos_error_details
(
    id               SERIAL PRIMARY KEY,
    pos_log_id       INT         NOT NULL REFERENCES pos_logs (id) ON DELETE CASCADE,
    error_code       VARCHAR(100),
    error_stacktrace TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Trigger function to update updated_at timestamp
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

-- Attach trigger to tables that need updated_at auto-update
CREATE TRIGGER trg_update_pos_logs_updated_at
    BEFORE UPDATE
    ON pos_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_update_pos_sync_queues_updated_at
    BEFORE UPDATE
    ON pos_sync_queues
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_update_pos_system_configs_updated_at
    BEFORE UPDATE
    ON pos_system_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
