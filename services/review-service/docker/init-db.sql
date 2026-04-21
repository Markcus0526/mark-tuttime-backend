-- ============================================================================
-- REVIEW SERVICE - Production Ready Schema
-- Optimized for Spring Boot 3.3.13 + Hibernate 6
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS review_service;
SET search_path TO review_service, public;

-- Enable pgcrypto for gen_random_uuid() if not already available
-- Note: gen_random_uuid() is built-in for Postgres 13+
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Automation: Global Updated_At Trigger Function
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Reviews Table
-- Uses native gen_random_uuid() to avoid uuid-ossp dependency
CREATE TABLE reviews
(
    id            UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL,           -- Logical FK to User Service
    branch_id     UUID        NOT NULL,           -- Logical FK to Branch Service
    rating        SMALLINT    NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment       TEXT,

    -- Status for Moderation
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'DELETED')),

    -- Metadata for Hibernate Optimistic Locking & Auditing
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Moderation Logs
CREATE TABLE review_moderation_logs
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id    UUID        NOT NULL,
    moderator_id UUID        NOT NULL,
    action       VARCHAR(50) NOT NULL, -- e.g., 'APPROVE', 'REJECT', 'FLAG'
    reason       TEXT,
    created_at   TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_moderation_review FOREIGN KEY (review_id)
        REFERENCES reviews (id) ON DELETE CASCADE
);

-- 4. Rating Aggregates (Read-Model optimized)
CREATE TABLE rating_aggregates
(
    branch_id      UUID PRIMARY KEY,
    average_rating DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    total_reviews  INTEGER       NOT NULL DEFAULT 0,
    -- JSONB default optimized for direct Map mapping in Java
    rating_dist    JSONB         NOT NULL DEFAULT '{"1":0,"2":0,"3":0,"4":0,"5":0}',
    created_at     TIMESTAMPTZ            DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ            DEFAULT CURRENT_TIMESTAMP
);

-- 5. Performance Indexes
-- For customer view: See all reviews for a branch
CREATE INDEX idx_reviews_branch_status ON reviews (branch_id, status)
    WHERE status = 'APPROVED';

-- For user profile: See my own review history
CREATE INDEX idx_reviews_user_id ON reviews (user_id);

-- For admin dashboard: Find PENDING reviews to moderate (Partial Index)
CREATE INDEX idx_reviews_pending_moderation ON reviews (created_at DESC)
    WHERE status = 'PENDING';

-- 6. Apply Updated_At Triggers (Now applied to ALL relevant tables)
CREATE TRIGGER trg_reviews_updated_at
    BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_moderation_logs_updated_at
    BEFORE UPDATE ON review_moderation_logs
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_rating_aggregates_updated_at
    BEFORE UPDATE ON rating_aggregates
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- 7. Comments for Documentation
COMMENT ON COLUMN reviews.version IS 'Used by Hibernate @Version for concurrency control';
COMMENT ON COLUMN rating_aggregates.rating_dist IS 'JSON map of score counts: {1: int, 2: int...}';
