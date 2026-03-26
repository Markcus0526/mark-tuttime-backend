-- ============================================================================
-- REVIEW SERVICE - Database Schema
-- Database: review_service_db
-- Schema: review_service
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS review_service;
SET
search_path TO review_service, public;

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
search_path TO review_service, public;

-- Enable required PostgreSQL extensions
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- Table: Review
CREATE TABLE reviews
(
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id       UUID     NOT NULL,
    restaurant_id UUID     NOT NULL,
    rating        SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES "users" (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id) ON DELETE CASCADE
);

-- Indexes for Review
CREATE INDEX idx_review_user_id ON review (user_id);
CREATE INDEX idx_review_restaurant_id ON review (restaurant_id);
CREATE INDEX idx_review_status ON review (status);
CREATE INDEX idx_review_created_at ON review (created_at);

-- Table: Review_Moderation_Log
CREATE TABLE review_moderation_logs
(
    id           UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    review_id    UUID        NOT NULL,
    moderator_id UUID,
    action       VARCHAR(50) NOT NULL,
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_moderation_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_moderator FOREIGN KEY (moderator_id) REFERENCES "users" (id) ON DELETE SET NULL
);

-- Indexes for Review_Moderation_Log
CREATE INDEX idx_moderation_review_id ON review_moderation_logs (review_id);
CREATE INDEX idx_moderation_moderator_id ON review_moderation_logs (moderator_id);

-- Table: Rating_Aggregate
CREATE TABLE rating_aggregate
(
    restaurant_id  UUID PRIMARY KEY,
    average_rating DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    total_reviews  INTEGER       NOT NULL DEFAULT 0,
    last_updated   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rating_aggregate_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id) ON DELETE CASCADE
);