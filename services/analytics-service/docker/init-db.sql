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
CREATE
EXTENSION IF NOT EXISTS timescaledb;

-- Create raw event log table
CREATE TABLE event_logs
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    payload    JSONB        NOT NULL,
    timestamp  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Convert event_log into a hypertable partitioned by timestamp
SELECT create_hypertable('event_logs', 'timestamp', if_not_exists => TRUE);

-- Index for event_type and timestamp queries
CREATE INDEX idx_event_log_type_time ON event_logs (event_type, timestamp);

-- Create aggregated metrics table
CREATE TABLE aggregated_metrics
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_name       VARCHAR(100) NOT NULL,
    value             NUMERIC      NOT NULL,
    time_window_start TIMESTAMPTZ  NOT NULL,
    time_window_end   TIMESTAMPTZ  NOT NULL,
    restaurant_id     UUID NULL,
    CONSTRAINT chk_time_window CHECK (time_window_end > time_window_start)
);

-- Convert aggregated_metrics into a hypertable partitioned by time_window_start
SELECT create_hypertable('aggregated_metrics', 'time_window_start', if_not_exists => TRUE);

-- Index on metric_name and time window for fast querying
CREATE INDEX idx_aggregated_metrics_name_time ON aggregated_metrics (metric_name, time_window_start, time_window_end);

-- Optional index on restaurant_id for filtering
CREATE INDEX idx_aggregated_metrics_restaurant ON aggregated_metrics (restaurant_id);