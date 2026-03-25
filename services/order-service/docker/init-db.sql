-- ============================================================================
-- ORDER SERVICE - Database Schema
-- Database: order_service_db
-- Schema: order_service
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS order_service;
SET
search_path TO order_service, public;

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
search_path TO order_service, public;

-- Enable required PostgreSQL extensions
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- Create enum type for order status
CREATE TYPE order_status AS ENUM (
    'CREATED',
	'PAID',
	'SENT_TO_RESTAURANT',
	'ACCEPTED',
	'PREPARING',
	'READY',
	'COMPLETED',
	'SERVED',
	'CANCELED',
	'REFUNDED'
);

-- Enable UUID extension for UUID generation
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

-- Orders table
CREATE TABLE orders
(
    id           UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    booking_id   UUID           NOT NULL,
    user_id      UUID           NOT NULL,
    status       order_status   NOT NULL DEFAULT 'CREATED',
    total_amount NUMERIC(10, 2) NOT NULL CHECK (total_amount >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Indexes for orders table
CREATE INDEX idx_orders_booking_id ON orders (booking_id);
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_deleted_at ON orders (deleted_at);

-- Order items table
CREATE TABLE order_items
(
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id     UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    quantity     INT  NOT NULL CHECK (quantity > 0),
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Indexes for order_items
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_menu_item_id ON order_items (menu_item_id);
CREATE INDEX idx_order_items_deleted_at ON order_items (deleted_at);

-- Order item modifiers table
CREATE TABLE order_item_modifiers
(
    id               UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    order_item_id    UUID                     NOT NULL,
    modifier_id      UUID                     NOT NULL,
    price_adjustment NUMERIC(10, 2)           NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by       UUID,
    updated_by       UUID,
    deleted_at       TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE
);

-- Indexes for order_item_modifiers
CREATE INDEX idx_order_item_modifiers_order_item_id ON order_item_modifiers (order_item_id);
CREATE INDEX idx_order_item_modifiers_modifier_id ON order_item_modifiers (modifier_id);
CREATE INDEX idx_order_item_modifiers_deleted_at ON order_item_modifiers (deleted_at);

-- Optional: triggers to update updated_at column on row update
CREATE
OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at
= now();
RETURN NEW;
END;
$$
language 'plpgsql';

CREATE TRIGGER trg_update_orders_updated_at
    BEFORE UPDATE
    ON orders
    FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();

CREATE TRIGGER trg_update_order_items_updated_at
    BEFORE UPDATE
    ON order_items
    FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();

CREATE TRIGGER trg_update_order_item_modifiers_updated_at
    BEFORE UPDATE
    ON order_item_modifiers
    FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();