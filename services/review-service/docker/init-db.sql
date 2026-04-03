-- ============================================================================
-- REVIEW SERVICE - Optimized Schema
-- Improvements: Automatic Timestamp Triggers, Plural Consistency,
-- and Aggregate Sync Logic.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS review_service;
SET
search_path TO review_service, public;

-- Enable UUID extension
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Create a Reusable Trigger Function for 'updated_at'
-- PostgreSQL requires this to automate the updated_at column
CREATE
OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

-- 2. Reviews Table
CREATE TABLE reviews
(
    id            UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    user_id       UUID        NOT NULL,           -- Logical FK to User Service
    restaurant_id UUID        NOT NULL,           -- Logical FK to Restaurant Service
    rating        SMALLINT    NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment       TEXT,

    -- Status for Moderation (Page 9 of PDF)
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'DELETED')),

    -- Metadata
    version       BIGINT      NOT NULL DEFAULT 0, -- For JPA Optimistic Locking
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Moderation Logs
CREATE TABLE review_moderation_logs
(
    id           UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    review_id    UUID        NOT NULL,
    moderator_id UUID        NOT NULL,
    action       VARCHAR(50) NOT NULL,
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_moderation_review FOREIGN KEY (review_id)
        REFERENCES reviews (id) ON DELETE CASCADE
);

-- 4. Rating Aggregates (Optimized for Read Performance)
CREATE TABLE rating_aggregates
(
    restaurant_id  UUID PRIMARY KEY,
    average_rating DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    total_reviews  INTEGER       NOT NULL DEFAULT 0,
    -- Store distribution for UI: {1: 5, 2: 10, ...}
    rating_dist    JSONB                  DEFAULT '{
      "1": 0,
      "2": 0,
      "3": 0,
      "4": 0,
      "5": 0
    }',
    last_updated   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Performance Indexes
CREATE INDEX idx_reviews_restaurant_status ON reviews (restaurant_id, status);
CREATE INDEX idx_reviews_user_id ON reviews (user_id);
CREATE INDEX idx_reviews_created_at ON reviews (created_at DESC);

-- 6. Apply Updated_At Triggers
CREATE TRIGGER trg_reviews_updated_at
    BEFORE UPDATE
    ON reviews
    FOR EACH ROW EXECUTE PROCEDURE update_timestamp();
