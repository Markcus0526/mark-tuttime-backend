CREATE SCHEMA IF NOT EXISTS booking_service;
SET
search_path TO booking_service, public;

CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- Enum for Type-Safe Statuses
CREATE TYPE booking_status_enum AS ENUM ('CREATED', 'PAID', 'CANCELED', 'NO_SHOW', 'COMPLETED');

-- 1. Bookings Table (Updated for TIMESTAMPTZ and UUID defaults)
CREATE TABLE bookings
(
    id                  UUID PRIMARY KEY             DEFAULT gen_random_uuid(),
    user_id             UUID                NOT NULL,
    restaurant_id       UUID                NOT NULL,
    branch_id           UUID                NOT NULL,
    zone_id             UUID                NOT NULL,
    table_id            UUID                NOT NULL,
    status              booking_status_enum NOT NULL DEFAULT 'CREATED',
    booking_datetime    TIMESTAMPTZ         NOT NULL, -- Use TZ for global accuracy
    duration            INT                 NOT NULL,
    party_size          INT                 NOT NULL CHECK (party_size > 0),
    lead_time_minutes   INT                          DEFAULT 0,
    grace_period_mins   INT                          DEFAULT 15,
    cancellation_reason VARCHAR(255),
    auto_cancelled      BOOLEAN                      DEFAULT FALSE,
    created_at          TIMESTAMPTZ                  DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ                  DEFAULT CURRENT_TIMESTAMP
);

-- 2. Booking Status History (Auditing)
CREATE TABLE booking_status_histories
(
    id         BIGSERIAL PRIMARY KEY,
    booking_id UUID                NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    old_status booking_status_enum,
    new_status booking_status_enum NOT NULL,
    changed_by UUID, -- User or System ID
    changed_at TIMESTAMPTZ         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason     VARCHAR(255),
    created_at TIMESTAMPTZ                  DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ                  DEFAULT CURRENT_TIMESTAMP
);

-- 3. Booking Notifications (Tracking communication)
CREATE TABLE booking_notifications
(
    id                UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    booking_id        UUID         NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    notification_type VARCHAR(50)  NOT NULL, -- SMS, EMAIL, PUSH
    status            VARCHAR(20)  NOT NULL, -- SENT, FAILED, DELIVERED
    recipient_contact VARCHAR(255) NOT NULL,
    sent_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE waitlist_entries
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL, -- FK to external User Service
    branch_id   UUID        NOT NULL, -- Reference to the Restaurant Branch
    zone_id     UUID,                 -- Optional: Can wait for a specific Zone (e.g., VIP)
    party_size  INT         NOT NULL CHECK (party_size > 0),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notified_at TIMESTAMPTZ,          -- When we sent the "Table Ready" notification
    status      VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    -- Statuses: WAITING, NOTIFIED, CONVERTED, EXPIRED, CANCELED

    -- Audit fields
    created_at  TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP
);

-- Indices for performance
CREATE INDEX idx_bookings_branch_date ON bookings (branch_id, booking_datetime);
CREATE INDEX idx_bookings_table_status ON bookings (table_id, status);

-- Index for the "First-Come, First-Served" algorithm
CREATE INDEX idx_waitlist_priority ON booking_service.waitlist_entries (branch_id, joined_at) WHERE status = 'WAITING';

-- Index to quickly find entries for a specific user
CREATE INDEX idx_waitlist_user ON booking_service.waitlist_entries (user_id);