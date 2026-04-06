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

CREATE TABLE system_events
(
    id           UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    service_name VARCHAR(50)  NOT NULL, -- e.g., 'REVIEW_SERVICE', 'BOOKING_SERVICE'
    event_type   VARCHAR(100) NOT NULL, -- e.g., 'REVIEW_APPROVED', 'ORDER_CREATED'
    entity_id    UUID,                  -- ID of the restaurant/user involved
    payload      JSONB,                 -- Full event data
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at   TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast time-range reporting
CREATE INDEX idx_events_occurred_at ON system_events (occurred_at DESC);
CREATE INDEX idx_events_type ON system_events (event_type);
