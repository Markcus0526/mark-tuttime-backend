CREATE SCHEMA IF NOT EXISTS booking_service;
SET search_path TO booking_service, public;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Enum for Type-Safe Statuses
CREATE TYPE booking_status_enum AS ENUM ('CREATED', 'PAID', 'CANCELED', 'NO_SHOW', 'COMPLETED');

-- 1. Bookings Table (Updated for TIMESTAMPTZ and UUID defaults)
CREATE TABLE bookings
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL,
    restaurant_id       UUID        NOT NULL,
    branch_id           UUID        NOT NULL,
    zone_id             UUID        NOT NULL,
    table_id            UUID        NOT NULL,
    status              booking_status_enum NOT NULL DEFAULT 'CREATED',
    booking_datetime    TIMESTAMPTZ NOT NULL, -- Use TZ for global accuracy
    party_size          INT         NOT NULL CHECK (party_size > 0),
    lead_time_minutes   INT         DEFAULT 0,
    grace_period_mins   INT         DEFAULT 15,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    cancellation_reason VARCHAR(255),
    auto_cancelled      BOOLEAN     DEFAULT FALSE
);

-- 2. Booking Status History (Auditing)
CREATE TABLE booking_status_histories
(
    id          BIGSERIAL PRIMARY KEY,
    booking_id  UUID NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    old_status  booking_status_enum,
    new_status  booking_status_enum NOT NULL,
    changed_by  UUID, -- User or System ID
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason      VARCHAR(255)
);

-- 3. Booking Notifications (Tracking communication)
CREATE TABLE booking_notifications
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id        UUID NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL, -- SMS, EMAIL, PUSH
    status            VARCHAR(20) NOT NULL, -- SENT, FAILED, DELIVERED
    recipient_contact VARCHAR(255) NOT NULL,
    sent_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indices for performance
CREATE INDEX idx_bookings_branch_date ON bookings (branch_id, booking_datetime);
CREATE INDEX idx_bookings_table_status ON bookings (table_id, status);
