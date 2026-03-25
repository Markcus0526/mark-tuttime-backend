-- ============================================================================
-- MENU SERVICE - Database Schema
-- Database: menu_service_db
-- Schema: menu_service
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS menu_service;
SET search_path TO menu_service, public;

-- Set timezone to UTC for all timestamp operations
SET timezone = 'UTC';

-- Set client encoding to UTF-8 to handle Unicode characters
SET client_encoding = 'UTF8';

-- Suppress NOTICE messages
SET client_min_messages TO WARNING;

-- Set search_path after schema is created
SET search_path TO menu_service, public;

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Categories
CREATE TABLE menu_categories (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL, -- FK to Restaurant Branch
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. Items
CREATE TABLE menu_items (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    price DECIMAL(10,2) NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. Modifiers
CREATE TABLE modifiers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    price_adjustment DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. Mapping (Item <-> Modifier)
CREATE TABLE menu_item_modifiers (
    id BIGSERIAL PRIMARY KEY,
    menu_item_id BIGINT NOT NULL,
    modifier_id BIGINT NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_item_modifier (menu_item_id, modifier_id)
);

-- 5. Stop Lists
CREATE TABLE stop_lists (
    id BIGSERIAL PRIMARY KEY,
    menu_item_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    reason VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 6. Discounts (Postgres uses custom types for ENUMs)
CREATE TABLE discounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    discount_type discount_type_enum NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    applies_to applies_to_enum NOT NULL,
    reference_id BIGINT,
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indices
CREATE INDEX idx_discount_applies_to_ref ON discounts (applies_to, reference_id);
CREATE INDEX idx_discount_active_period ON discounts (is_active, start_date, end_date);


-- CREATE TABLE Order (
--                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
--                        user_id BIGINT NOT NULL,
--                        branch_id BIGINT NOT NULL,
--                        total_amount DECIMAL(10,2) NOT NULL,
--                        discount_amount DECIMAL(10,2) DEFAULT 0.00,
--                        final_amount DECIMAL(10,2) NOT NULL,
--                        status ENUM('CREATED', 'PAID', 'CANCELED', 'COMPLETED') NOT NULL,
--                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
-- );
--
-- CREATE TABLE OrderItem (
--                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
--                            order_id BIGINT NOT NULL,
--                            menu_item_id BIGINT NOT NULL,
--                            quantity INT NOT NULL DEFAULT 1,
--                            unit_price DECIMAL(10,2) NOT NULL,
--                            discount_amount DECIMAL(10,2) DEFAULT 0.00,
--                            final_price DECIMAL(10,2) NOT NULL,
--                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
-- );


-- ============================================================================
-- ANALYZE TABLES - Update Statistics for Query Optimizer
-- ============================================================================

ANALYZE menu_categories;
ANALYZE menu_items;
ANALYZE modifiers;
ANALYZE menu_item_modifiers;
ANALYZE stop_lists;
ANALYZE discounts;

