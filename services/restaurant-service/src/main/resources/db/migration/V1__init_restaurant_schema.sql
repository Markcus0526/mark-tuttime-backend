-- ============================================================================
-- Flyway Migration: V1__init_restaurant_schema.sql
-- Description: Core Schema Initialization for Restaurant Service
-- Database Engine: PostgreSQL
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS restaurant_service;
SET
    search_path TO restaurant_service, public;

-- Enable required extensions
CREATE
    EXTENSION IF NOT EXISTS postgis;
CREATE
    EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
    EXTENSION IF NOT EXISTS "pgcrypto";

-- === ENUM TYPES ===
DO
$$
    BEGIN
        IF
            NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'media_type_enum') THEN
            CREATE TYPE media_type_enum AS ENUM ('IMAGE', 'VIDEO', 'CANVAS');
        END IF;
        IF
            NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'table_status_enum') THEN
            CREATE TYPE table_status_enum AS ENUM ('ACTIVE', 'RESERVED', 'OCCUPIED', 'CLOSED');
        END IF;
    END $$;

-- === Timestamp Automation Function ===
CREATE
    OR REPLACE FUNCTION update_modified_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
        = CURRENT_TIMESTAMP;
    RETURN NEW;
END; $$
    LANGUAGE plpgsql;

-- 1. RESTAURANTS
CREATE TABLE restaurants
(
    id          UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    phone       VARCHAR(20),
    website     VARCHAR(255),
    rating      NUMERIC(2, 1)         DEFAULT 0 CHECK (rating >= 0 AND rating <= 5),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

COMMENT
    ON TABLE restaurants IS 'Primary dishTable for restaurants';

-- 2. BRANCHES
CREATE TABLE branches
(
    id            UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    restaurant_id UUID         NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    address       VARCHAR(512) NOT NULL,
    phone         VARCHAR(20),
    timezone      VARCHAR(50)  NOT NULL,
    location      GEOGRAPHY(POINT, 4326),
    rating        NUMERIC(2, 1)         DEFAULT 0 CHECK (rating >= 0 AND rating <= 5),
    closed        BOOLEAN      NOT NULL DEFAULT FALSE,
    min_price     NUMERIC(12, 2)        DEFAULT 0,
    max_price     NUMERIC(12, 2)        DEFAULT 0,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_price_range CHECK (min_price <= max_price)
);

CREATE INDEX idx_branches_rating ON branches (rating DESC) WHERE deleted = FALSE;

COMMENT
    ON TABLE branches IS 'Physical locations or outlets of a specific restaurant';

-- 3. STAFFS
CREATE TABLE staffs
(
    id            UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    restaurant_id UUID        NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    branch_id     UUID REFERENCES branches (id) ON DELETE CASCADE, -- NULL if managing whole restaurant
    user_id       UUID        NOT NULL,                            -- Logical link to User Service
    role_name     VARCHAR(50) NOT NULL CHECK (role_name IN ('OWNER', 'MANAGER', 'HOSTESS')),
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_staff_per_context UNIQUE (restaurant_id, branch_id, user_id)
);

-- 4. ZONES
CREATE TABLE zones
(
    id          UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    branch_id   UUID         NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    capacity    INT          NOT NULL,
    closed      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

-- 5. TABLES
CREATE TABLE tables
(
    id         UUID PRIMARY KEY           DEFAULT uuid_generate_v4(),
    zone_id    UUID              NOT NULL REFERENCES zones (id) ON DELETE CASCADE,
    seats      INT               NOT NULL,
    status     table_status_enum NOT NULL DEFAULT 'ACTIVE',
    pos_x      FLOAT             NOT NULL,
    pos_y      FLOAT             NOT NULL,
    table_type VARCHAR(20),
    label      VARCHAR(50)       NOT NULL,
    notes      TEXT,
    closed     BOOLEAN           NOT NULL DEFAULT FALSE,
    version    BIGINT            NOT NULL DEFAULT 0,
    deleted    BOOLEAN           NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ                DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ                DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX unique_zone_table_label
    ON tables (zone_id, label) WHERE (deleted = FALSE);

-- 6. MEDIAS
CREATE TABLE medias
(
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_id      UUID    NOT NULL,
    url            VARCHAR(1024),
    alt_text       VARCHAR(255),
    primary_entity BOOLEAN          DEFAULT FALSE,
    media_type     media_type_enum  DEFAULT 'IMAGE',
    layout_config  JSONB, -- Stores {canvas: {...}, fixtures: [...]}
    map_entity     BOOLEAN          DEFAULT FALSE,
    deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

-- 7. SCHEDULES
CREATE TABLE schedules
(
    id          UUID PRIMARY KEY  DEFAULT uuid_generate_v4(),
    entity_id   UUID     NOT NULL,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    open_time   TIME WITHOUT TIME ZONE NOT NULL,
    close_time  TIME WITHOUT TIME ZONE NOT NULL,
    closed      BOOLEAN           DEFAULT FALSE NOT NULL,
    deleted     BOOLEAN  NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ       DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ       DEFAULT CURRENT_TIMESTAMP
);

-- 8. MASTER TAGS (Managed by Admin Panel)
CREATE TABLE tags
(
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(50) NOT NULL UNIQUE,
    icon_url   VARCHAR(255),         -- For custom icons or emoji strings
    category   VARCHAR(30) NOT NULL, -- e.g., 'CUISINE', 'DIETARY', 'VIBE'
    created_at TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

-- 9. RESTAURANT-TAG MAPPING (Many-to-Many)
CREATE TABLE branch_tags
(
    branch_id UUID NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    tag_id    UUID NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (branch_id, tag_id)
);

CREATE TABLE user_bookmarks
(
    user_id    UUID NOT NULL, -- Linked to your User Service
    branch_id  UUID NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, branch_id)
);

-- === INDEXES ===
CREATE INDEX idx_staffs_user_id ON staffs (user_id);
CREATE INDEX idx_branches_geo ON branches USING GIST(location);
CREATE INDEX idx_res_tags_lookup ON branch_tags (tag_id);
CREATE INDEX idx_user_bookmarks_user ON user_bookmarks (user_id);

-- === TRIGGERS ===
CREATE TRIGGER trg_restaurants_upd
    BEFORE UPDATE
    ON restaurants
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_staffs_upd
    BEFORE UPDATE
    ON staffs
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_branches_upd
    BEFORE UPDATE
    ON branches
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_zones_upd
    BEFORE UPDATE
    ON zones
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_tables_upd
    BEFORE UPDATE
    ON tables
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_medias_upd
    BEFORE UPDATE
    ON medias
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_schedules_upd
    BEFORE UPDATE
    ON schedules
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_tags_upd
    BEFORE UPDATE
    ON tags
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
