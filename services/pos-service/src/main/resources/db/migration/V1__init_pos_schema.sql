-- ============================================================================
-- Flyway Migration: V1__init_pos_schema.sql
-- Description: Core Schema Initialization for Pos Service
-- Database Engine: PostgreSQL
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
CREATE TYPE pos_provider AS ENUM ('IIKO', 'RKEEPER');

-- 2. POS Configuration Table (The "Source of Truth" for Credentials)
-- Updated with organization_id and terminal_group_id
CREATE TABLE pos_configs
(
    id                SERIAL PRIMARY KEY,
    branch_id         UUID UNIQUE  NOT NULL,
    pos_provider      pos_provider NOT NULL,
    api_endpoint      TEXT         NOT NULL,
    api_key           TEXT,
    webhook_secret    TEXT,
    organization_id   TEXT, -- For iiko
    terminal_group_id TEXT, -- For iiko
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pos_config_branch ON pos_configs (branch_id) WHERE enabled = TRUE;

-- 3. POS Logs Table (The "Black Box" Audit Trail)
-- Tracks every individual request/response
CREATE TABLE pos_logs
(
    id               SERIAL PRIMARY KEY,
    branch_id        UUID NOT NULL,
    entity_id        UUID NOT NULL, -- booking_id or order_id
    entity_type      TEXT NOT NULL, -- 'BOOKING', 'ORDER', 'MENU'
    action           TEXT NOT NULL, -- 'RESERVE_TABLE', 'SEND_ORDER'
    request_payload  JSONB,
    response_payload TEXT,
    status           TEXT NOT NULL, -- 'SUCCESS', 'FAILED', 'SUCCESS_ALREADY_SET'
    attempt_count    INTEGER     DEFAULT 1,
    created_at       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pos_logs_entity ON pos_logs (entity_id);

-- 4. POS Sync Queues Table (The "To-Do" Retry List)
-- Decoupled from logs to handle asynchronous retries
CREATE TABLE pos_sync_queues
(
    id            SERIAL PRIMARY KEY,
    branch_id     UUID NOT NULL,
    booking_id    UUID,
    order_id      UUID,
    table_ids TEXT, -- store as comma-separated or JSON
    action        TEXT NOT NULL,
    payload       JSONB,                           -- Original event snapshot
    status        TEXT NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'COMPLETED', 'FAILED_PERMANENTLY'
    retry_count   INTEGER       DEFAULT 0,
    next_retry_at TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    last_error    TEXT,
    created_at    TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pos_sync_pending ON pos_sync_queues (status, next_retry_at) WHERE status = 'PENDING';

-- Table to link TutTime Menu Items to External POS IDs
CREATE TABLE pos_menu_mappings
(
    id              SERIAL PRIMARY KEY,
    branch_id       UUID NOT NULL REFERENCES pos_configs (branch_id),
    menu_item_id    UUID NOT NULL,  -- Logical ID from Menu Service
    external_pos_id TEXT NOT NULL,  -- The iiko/R-Keeper GUID or Code
    external_name   TEXT,           -- Name as it appears in the POS
    external_price  DECIMAL(10, 2), -- Price as it appears in the POS
    last_synced_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (branch_id, menu_item_id)
);

CREATE INDEX idx_pos_mapping_lookup ON pos_menu_mappings (branch_id, menu_item_id);


-- 5. Triggers for updated_at
CREATE
    OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
        = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$
    language 'plpgsql';

CREATE TRIGGER trg_pos_configs_updated
    BEFORE UPDATE
    ON pos_configs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_pos_sync_queues_updated
    BEFORE UPDATE
    ON pos_sync_queues
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
