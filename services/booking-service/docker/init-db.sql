-- ============================================================================
-- BOOKING SERVICE - Database Schema
-- Database: bookingservice_db
-- Schema: bookingservice
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS bookingservice;
SET
search_path TO bookingservice, public;

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
search_path TO bookingservice, public;

-- Enable required PostgreSQL extensions
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE booking_status AS ENUM ('CREATED', 'PAID', 'CANCELED', 'NO_SHOW', 'COMPLETED');

-- Table: bookings
CREATE TABLE bookings
(
    booking_id          UUID PRIMARY KEY,
    user_id             UUID        NOT NULL,
    restaurant_id       UUID        NOT NULL,
    branch_id           UUID        NOT NULL,
    zone_id             UUID        NOT NULL,
    table_id            UUID        NOT NULL,
    booking_status      VARCHAR(20) NOT NULL,
    booking_datetime    TIMESTAMP   NOT NULL,
    party_size          INT         NOT NULL,
    lead_time_minutes   INT,
    grace_period_mins   INT,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    cancellation_reason VARCHAR(255),
    auto_cancelled      BOOLEAN              DEFAULT FALSE
);

CREATE INDEX idx_bookings_datetime ON bookings (booking_datetime);
CREATE INDEX idx_bookings_status ON bookings (booking_status);
CREATE INDEX idx_bookings_table_id ON bookings (table_id);

-- Table: booking_locks
CREATE TABLE booking_locks
(
    lock_id          UUID PRIMARY KEY,
    booking_id       UUID      NOT NULL,
    table_id         UUID      NOT NULL,
    lock_acquired_at TIMESTAMP NOT NULL,
    lock_expires_at  TIMESTAMP NOT NULL,
    CONSTRAINT fk_booking_locks_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id)
);

-- Table: booking_status_histories
CREATE TABLE booking_status_histories
(
    history_id BIGSERIAL PRIMARY KEY,
    booking_id UUID        NOT NULL,
    old_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    changed_by UUID,
    changed_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason     VARCHAR(255),
    CONSTRAINT fk_booking_status_histories_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id)
);

-- Table: booking_notifications_s
CREATE TABLE booking_notifications_s
(
    notification_id   UUID PRIMARY KEY,
    booking_id        UUID         NOT NULL,
    notification_type VARCHAR(50)  NOT NULL,
    sent_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status            VARCHAR(20)  NOT NULL,
    recipient_contact VARCHAR(255) NOT NULL,
    CONSTRAINT fk_booking_notifications_s_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id)
);