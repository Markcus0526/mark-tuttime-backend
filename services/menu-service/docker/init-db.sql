-- ============================================================================
-- MENU SERVICE - Database Schema (UUID Version with Modifier Groups)
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS menu_service;
SET
search_path TO menu_service, public;

CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- Create Enums
CREATE TYPE discount_type_enum AS ENUM ('PERCENTAGE', 'FIXED_AMOUNT');
CREATE TYPE applies_to_enum AS ENUM ('CATEGORY', 'ITEM', 'ORDER');

-- 1. Categories
CREATE TABLE menu_categories
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    branch_id   UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT NULL,
    priority    INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

-- 2. Items
CREATE TABLE menu_items
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    category_id  UUID           NOT NULL REFERENCES menu_categories (id) ON DELETE CASCADE,
    name         VARCHAR(255)   NOT NULL,
    description  TEXT NULL,
    price        DECIMAL(10, 2) NOT NULL,
    is_available BOOLEAN                 DEFAULT TRUE,
    priority     INT            NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP
);

-- 3. NEW: Modifier Groups (e.g., "Choose your sauce", "Add toppings")
CREATE TABLE modifier_groups
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT NULL,
    min_select  INT          NOT NULL DEFAULT 0, -- 1 for mandatory single choice
    max_select  INT          NOT NULL DEFAULT 1, -- e.g., 5 for pizza toppings
    created_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

-- 4. Modifiers (The actual choices like "Bacon", "Mayo")
CREATE TABLE modifiers
(
    id                UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    modifier_group_id UUID           NOT NULL REFERENCES modifier_groups (id) ON DELETE CASCADE,
    name              VARCHAR(255)   NOT NULL,
    description       TEXT NULL,
    price_adjustment  DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at        TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP
);

-- 5. Mapping (Item <-> Modifier Group)
CREATE TABLE menu_item_modifier_groups
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id      UUID NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    modifier_group_id UUID NOT NULL REFERENCES modifier_groups (id) ON DELETE CASCADE,
    priority          INT  NOT NULL    DEFAULT 0, -- For ordering display in the app
    created_at        TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (menu_item_id, modifier_group_id)
);

-- 6. Stop Lists
CREATE TABLE stop_lists
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id UUID NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    branch_id    UUID NOT NULL,
    start_date   DATE NOT NULL,
    end_date     DATE NULL,
    reason       VARCHAR(255) NULL,
    created_at   TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

-- 7. Discounts
CREATE TABLE discounts
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255)       NOT NULL,
    discount_type discount_type_enum NOT NULL,
    amount        DECIMAL(10, 2)     NOT NULL,
    applies_to    applies_to_enum    NOT NULL,
    reference_id  UUID,
    start_date    TIMESTAMPTZ,
    end_date      TIMESTAMPTZ,
    is_active     BOOLEAN          DEFAULT TRUE,
    created_at    TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

-- Indices
CREATE INDEX idx_discount_applies_to_ref ON discounts (applies_to, reference_id);
CREATE INDEX idx_menu_items_category ON menu_items (category_id);
CREATE INDEX idx_modifiers_group ON modifiers (modifier_group_id);
CREATE INDEX idx_stop_lists_branch_item ON stop_lists (branch_id, menu_item_id);

ANALYZE
menu_categories;
ANALYZE
menu_items;
ANALYZE
modifier_groups;
ANALYZE
modifiers;
ANALYZE
menu_item_modifier_groups;
ANALYZE
stop_lists;
ANALYZE
discounts;
