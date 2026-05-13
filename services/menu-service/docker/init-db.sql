-- ============================================================================
-- MENU SERVICE - Complete Production Schema (UUID & Inheritance)
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS menu_service;
SET
search_path TO menu_service, public;

-- Enable extensions
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. ENUMS
-- ============================================================================
CREATE TYPE discount_type_enum AS ENUM ('PERCENTAGE', 'FIXED_AMOUNT');
CREATE TYPE applies_to_enum AS ENUM ('CATEGORY', 'ITEM', 'ORDER');
CREATE TYPE media_type_enum AS ENUM ('IMAGE', 'VIDEO');

-- ============================================================================
-- 2. HELPER FUNCTION FOR updated_at TIMESTAMPS
-- ============================================================================
CREATE
OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

-- ============================================================================
-- 3. TABLES
-- ============================================================================

-- 3.1 CATEGORIES
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

CREATE INDEX idx_menu_categories_branch ON menu_categories (branch_id);
CREATE UNIQUE INDEX uk_menu_categories_branch_name ON menu_categories (branch_id, name) WHERE deleted = FALSE;

-- 3.2 ITEMS
CREATE TABLE menu_items
(
    id               UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    menu_category_id UUID           NOT NULL REFERENCES menu_categories (id) ON DELETE CASCADE,
    name             VARCHAR(255)   NOT NULL,
    description      TEXT,
    price            DECIMAL(10, 2) NOT NULL,
    available        BOOLEAN                 DEFAULT TRUE,
    priority         INT            NOT NULL DEFAULT 0,
    version          BIGINT         NOT NULL DEFAULT 0,
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_menu_items_category ON menu_items (menu_category_id);
CREATE UNIQUE INDEX uk_menu_items_category_name ON menu_items (menu_category_id, name) WHERE deleted = FALSE;

-- 3.3 MODIFIER GROUPS
CREATE TABLE modifier_groups
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    branch_id   UUID         NOT NULL, -- added for proper scoping
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    min_select  INT          NOT NULL DEFAULT 0,
    max_select  INT          NOT NULL DEFAULT 1,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_modifier_group_min_max CHECK (min_select <= max_select)
);

CREATE UNIQUE INDEX uk_modifier_groups_branch_name ON modifier_groups (branch_id, name) WHERE deleted = FALSE;

-- 3.4 MODIFIERS (choices)
CREATE TABLE modifiers
(
    id                UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    modifier_group_id UUID           NOT NULL REFERENCES modifier_groups (id) ON DELETE CASCADE,
    name              VARCHAR(255)   NOT NULL,
    description       TEXT,
    price_adjustment  DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    deleted           BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_modifiers_group ON modifiers (modifier_group_id);
CREATE UNIQUE INDEX uk_modifiers_group_name ON modifiers (modifier_group_id, name) WHERE deleted = FALSE;

-- 3.5 ITEM-LEVEL MODIFIER MAPPING
CREATE TABLE menu_item_modifier_groups
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id      UUID NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    modifier_group_id UUID NOT NULL REFERENCES modifier_groups (id) ON DELETE CASCADE,
    priority          INT  NOT NULL    DEFAULT 0,
    UNIQUE (menu_item_id, modifier_group_id)
);

-- 3.6 CATEGORY-LEVEL MODIFIER INHERITANCE
CREATE TABLE menu_category_modifier_groups
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_category_id  UUID NOT NULL REFERENCES menu_categories (id) ON DELETE CASCADE,
    modifier_group_id UUID NOT NULL REFERENCES modifier_groups (id) ON DELETE CASCADE,
    priority          INT  NOT NULL    DEFAULT 0,
    UNIQUE (menu_category_id, modifier_group_id)
);

-- 3.7 STOP LISTS (temporary unavailability)
CREATE TABLE stop_lists
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id UUID NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    branch_id    UUID NOT NULL,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL    DEFAULT '9999-12-31',
    reason       VARCHAR(255),
    created_at   TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stop_lists_active ON stop_lists (branch_id, menu_item_id) WHERE end_date >= '9999-12-31';

-- 3.8 DISCOUNTS (with branch_id, soft‑delete, and proper constraints)
CREATE TABLE discounts
(
    id            UUID PRIMARY KEY            DEFAULT gen_random_uuid(),
    branch_id     UUID               NOT NULL,
    name          VARCHAR(255)       NOT NULL,
    discount_type discount_type_enum NOT NULL,
    amount        DECIMAL(10, 2)     NOT NULL,
    applies_to    applies_to_enum    NOT NULL,
    reference_id  UUID,
    start_date    TIMESTAMPTZ,
    end_date      TIMESTAMPTZ,
    active        BOOLEAN                     DEFAULT TRUE,
    version       BIGINT             NOT NULL DEFAULT 0,
    deleted       BOOLEAN            NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ                 DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ                 DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_discount_amount_percentage CHECK (
        (discount_type != 'PERCENTAGE') OR (amount >= 0 AND amount <= 100)
        ),
    CONSTRAINT chk_discount_amount_fixed CHECK (
        (discount_type != 'FIXED_AMOUNT') OR (amount >= 0)
        )
);

CREATE INDEX idx_discounts_branch ON discounts (branch_id);
CREATE INDEX idx_discounts_reference ON discounts (applies_to, reference_id) WHERE active = TRUE AND deleted = FALSE;
CREATE UNIQUE INDEX uk_discounts_branch_name ON discounts (branch_id, name) WHERE deleted = FALSE;

-- 3.9 MEDIAS (polymorphic)
CREATE TABLE medias
(
    id             UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    entity_id      UUID          NOT NULL,
    url            VARCHAR(1024) NOT NULL,
    alt_text       VARCHAR(255),
    primary_entity BOOLEAN                DEFAULT FALSE,
    media_type     media_type_enum        DEFAULT 'IMAGE',
    deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ            DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ            DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_medias_entity ON medias (entity_id);

-- 3.10 MASTER MENU TAGS
CREATE TABLE menu_tags
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(50) NOT NULL UNIQUE,
    icon_url   VARCHAR(255),
    created_at TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

-- 3.11 MENU ITEM TAG MAPPING
CREATE TABLE menu_item_tags
(
    menu_item_id UUID NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    tag_id       UUID NOT NULL REFERENCES menu_tags (id) ON DELETE CASCADE,
    PRIMARY KEY (menu_item_id, tag_id)
);

CREATE INDEX idx_menu_item_tags_lookup ON menu_item_tags (tag_id);

-- ============================================================================
-- 4. TRIGGERS FOR updated_at
-- ============================================================================
CREATE TRIGGER trg_menu_categories_upd
    BEFORE UPDATE
    ON menu_categories
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER trg_menu_items_upd
    BEFORE UPDATE
    ON menu_items
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER trg_modifier_groups_upd
    BEFORE UPDATE
    ON modifier_groups
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER trg_modifiers_upd
    BEFORE UPDATE
    ON modifiers
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER trg_item_modifier_groups_upd
    BEFORE UPDATE
    ON menu_item_modifier_groups
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER trg_category_modifier_groups_upd
    BEFORE UPDATE
    ON menu_category_modifier_groups
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER trg_stop_lists_upd
    BEFORE UPDATE
    ON stop_lists
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER trg_discounts_upd
    BEFORE UPDATE
    ON discounts
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER trg_medias_upd
    BEFORE UPDATE
    ON medias
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER trg_menu_tags_upd
    BEFORE UPDATE
    ON menu_tags
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();