-- ============================================================================
-- MENU SERVICE - Full Production Schema (UUID & Inheritance Version)
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS menu_service;
SET
search_path TO menu_service, public;

-- Enable UUID extension
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. ENUMS
CREATE TYPE discount_type_enum AS ENUM ('PERCENTAGE', 'FIXED_AMOUNT');
CREATE TYPE applies_to_enum AS ENUM ('CATEGORY', 'ITEM', 'ORDER');
CREATE TYPE media_type_enum AS ENUM ('IMAGE', 'VIDEO');

-- 2. CATEGORIES
CREATE TABLE menu_categories
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    branch_id   UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    priority    INT          NOT NULL DEFAULT 0,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

-- 3. ITEMS
CREATE TABLE menu_items
(
    id               UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    menu_category_id UUID           NOT NULL REFERENCES menu_categories (id) ON DELETE CASCADE,
    name             VARCHAR(255)   NOT NULL,
    description      TEXT,
    price            DECIMAL(10, 2) NOT NULL,
    available        BOOLEAN                 DEFAULT TRUE,
    priority         INT            NOT NULL DEFAULT 0,
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP
);

-- 4. MODIFIER GROUPS (Rules: e.g., "Choose 1 Side")
CREATE TABLE modifier_groups
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    min_select  INT          NOT NULL DEFAULT 0, -- 1 for mandatory
    max_select  INT          NOT NULL DEFAULT 1, -- >1 for multiple choice
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

-- 5. MODIFIERS (The specific choices: e.g., "Fries")
CREATE TABLE modifiers
(
    id                UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    modifier_group_id UUID           NOT NULL REFERENCES modifier_groups (id) ON DELETE CASCADE,
    name              VARCHAR(255)   NOT NULL,
    description       TEXT,
    price_adjustment  DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    deleted           BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP
);

-- 6. ITEM-LEVEL MODIFIER MAPPING
CREATE TABLE menu_item_modifier_groups
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id      UUID NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    modifier_group_id UUID NOT NULL REFERENCES modifier_groups (id) ON DELETE CASCADE,
    priority          INT  NOT NULL    DEFAULT 0,
    UNIQUE (menu_item_id, modifier_group_id)
);

-- 7. CATEGORY-LEVEL INHERITANCE (Bulk Mapping)
CREATE TABLE menu_category_modifier_groups
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_category_id  UUID NOT NULL REFERENCES menu_categories (id) ON DELETE CASCADE,
    modifier_group_id UUID NOT NULL REFERENCES modifier_groups (id) ON DELETE CASCADE,
    priority          INT  NOT NULL    DEFAULT 0,
    UNIQUE (menu_category_id, modifier_group_id)
);

-- 8. STOP LISTS (Temporary Unavailability)
CREATE TABLE stop_lists
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id UUID NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    branch_id    UUID NOT NULL,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL DEFAULT '9999-12-31',
    reason       VARCHAR(255),
    created_at   TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

-- 9. DISCOUNTS (Promotional Rules)
CREATE TABLE discounts
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255)       NOT NULL,
    discount_type discount_type_enum NOT NULL,
    amount        DECIMAL(10, 2)     NOT NULL,
    applies_to    applies_to_enum    NOT NULL, -- CATEGORY, ITEM, or ORDER
    reference_id  UUID,                        -- The ID of the Category or Item
    start_date    TIMESTAMPTZ,
    end_date      TIMESTAMPTZ,
    active        BOOLEAN          DEFAULT TRUE,
    created_at    TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

-- 10. MEDIAS (Polymorphic Images)
CREATE TABLE medias
(
    id             UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    entity_id      UUID          NOT NULL, -- Links to Item, Category, or Modifier ID
    url            VARCHAR(1024) NOT NULL,
    alt_text       VARCHAR(255),
    primary_entity BOOLEAN                DEFAULT FALSE,
    media_type     media_type_enum        DEFAULT 'IMAGE',
    deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ            DEFAULT CURRENT_TIMESTAMP
);

-- 11. INDICES (For Query Optimization)
CREATE INDEX idx_menu_items_category ON menu_items (menu_category_id);
CREATE INDEX idx_modifiers_group ON modifiers (modifier_group_id);
CREATE INDEX idx_stop_lists_active
    ON stop_lists (branch_id, menu_item_id)
    WHERE end_date >= '9999-12-31';
CREATE INDEX idx_medias_entity ON medias (entity_id);
CREATE INDEX idx_discount_lookup ON discounts (active, applies_to, reference_id);