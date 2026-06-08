-- ============================================================================
-- Flyway Migration: V1__payment_pos_schema.sql
-- Description: Core Schema Initialization for Payment Service
-- Database Engine: PostgreSQL
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS order_service;
SET
    search_path TO order_service, public;

-- Standardize Environment
SET
    timezone = 'UTC';
SET
    client_encoding = 'UTF8';
SET
    client_min_messages TO WARNING;

-- Enable UUID extension (Required once per database)
CREATE
    EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Create enum type for order status
-- Note: Cast to TEXT in JPA/Hibernate is easier if this is defined.
DO
$$
    BEGIN
        IF
            NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_status') THEN
            CREATE TYPE order_status AS ENUM (
                'CREATED', 'SENT_TO_RESTAURANT', 'ACCEPTED',
                'PREPARING', 'READY', 'SERVED', 'COMPLETED', 'CANCELED'
                );
        END IF;
    END $$;

-- 2. Orders Table
CREATE TABLE orders
(
    id               UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    booking_id       UUID           NOT NULL, -- Logical reference to Booking Service
    user_id          UUID           NOT NULL, -- Logical reference to User Service
    branch_id        UUID           NOT NULL, -- Logical reference to Branch Service
    booking_datetime TIMESTAMPTZ,
    status           order_status   NOT NULL DEFAULT 'CREATED',
    total_amount     NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (total_amount >= 0),
    guest_name       VARCHAR(100),
    guest_phone      VARCHAR(20),
    special_request  TEXT,
    version          BIGINT         NOT NULL DEFAULT 0,
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    created_by       UUID,
    updated_by       UUID
);

-- 3. Order Items Table
CREATE TABLE order_items
(
    id           UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    order_id     UUID           NOT NULL,
    menu_item_id UUID           NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0),
    price        NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    version      BIGINT         NOT NULL DEFAULT 0,
    deleted      BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_orders FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE
);

-- 4. Order Item Modifiers Table
CREATE TABLE order_item_modifiers
(
    id               UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    order_item_id    UUID           NOT NULL,
    modifier_id      UUID           NOT NULL,
    price_adjustment NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_modifiers_order_item FOREIGN KEY (order_item_id)
        REFERENCES order_items (id) ON DELETE CASCADE
);

-- 5. Join table for multiple tables per order
CREATE TABLE order_tables
(
    order_id UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    table_id UUID NOT NULL,
    PRIMARY KEY (order_id, table_id)
);

-- 5. Performance Indexes
CREATE INDEX IF NOT EXISTS idx_orders_booking_status ON orders (booking_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders (user_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_orders_branch_id ON orders (branch_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_modifiers_item_id ON order_item_modifiers (order_item_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_order_tables_table_id ON order_tables (table_id);

-- 6. Trigger for Automated 'updated_at'
CREATE
    OR REPLACE FUNCTION update_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
        = now();
    RETURN NEW;
END;
$$
    language 'plpgsql';

CREATE TRIGGER trg_orders_timestamp
    BEFORE UPDATE
    ON orders
    FOR EACH ROW EXECUTE PROCEDURE update_timestamp();
CREATE TRIGGER trg_items_timestamp
    BEFORE UPDATE
    ON order_items
    FOR EACH ROW EXECUTE PROCEDURE update_timestamp();
CREATE TRIGGER trg_modifiers_timestamp
    BEFORE UPDATE
    ON order_item_modifiers
    FOR EACH ROW EXECUTE PROCEDURE update_timestamp();
