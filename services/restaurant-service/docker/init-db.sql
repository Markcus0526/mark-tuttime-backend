-- ============================================================================
-- RESTAURANT SERVICE - Full Documented Schema
-- Optimized for Spring Boot 3.3.13 + Hibernate 6
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
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'media_entity_type') THEN
CREATE TYPE media_entity_type AS ENUM ('RESTAURANT', 'BRANCH', 'ZONE', 'TABLE');
END IF;
    IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'media_type_enum') THEN
CREATE TYPE media_type_enum AS ENUM ('IMAGE', 'VIDEO', 'SVG');
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
    restaurant_id UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    address       VARCHAR(512) NOT NULL,
    location      GEOGRAPHY(POINT, 4326),
    phone         VARCHAR(20),
    website       VARCHAR(255),
    rating        NUMERIC(2, 1)         DEFAULT 0 CHECK (rating >= 0 AND rating <= 5),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

COMMENT
ON TABLE restaurants IS 'Primary dishTable for restaurants';
COMMENT
ON COLUMN restaurants.location IS 'Geographical point for proximity searches (lon, lat)';

-- 2. BRANCHES
CREATE TABLE branches
(
    branch_id     UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    restaurant_id UUID         NOT NULL REFERENCES restaurants (restaurant_id) ON DELETE CASCADE,
    address       VARCHAR(512) NOT NULL,
    phone         VARCHAR(20),
    timezone      VARCHAR(50)  NOT NULL,
    location      GEOGRAPHY(POINT, 4326),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

COMMENT
ON TABLE branches IS 'Physical locations or outlets of a specific restaurant';

-- 3. RESTAURANT_STAFFS
CREATE TABLE restaurant_staffs
(
    staff_id      UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    restaurant_id UUID        NOT NULL REFERENCES restaurants (restaurant_id) ON DELETE CASCADE,
    branch_id     UUID REFERENCES branches (branch_id) ON DELETE CASCADE, -- NULL if managing whole restaurant
    user_id       UUID        NOT NULL,                                   -- Logical link to User Service
    role_name     VARCHAR(50) NOT NULL CHECK (role_name IN ('OWNER', 'MANAGER', 'HOSTESS')),
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_staff_per_context UNIQUE (restaurant_id, branch_id, user_id)
);

COMMENT
ON TABLE restaurant_staffs IS 'Managers are tied to branch_id; Owners manage restaurant_id';
COMMENT
ON COLUMN restaurant_staffs.branch_id IS 'If set, the MANAGER or HOSTESS only has authority over this specific branch';
COMMENT
ON COLUMN restaurant_staffs.user_id IS 'Logical reference to User Service for account details';
COMMENT
ON COLUMN restaurant_staffs.role_name IS 'Local role within the restaurant (Manager can edit schedules, Hostess manages tables)';

-- 4. ZONES
CREATE TABLE zones
(
    zone_id     UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    branch_id   UUID         NOT NULL REFERENCES branches (branch_id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    capacity    INT          NOT NULL,
    closed      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

COMMENT
ON TABLE zones IS 'Specific areas inside a branch (e.g., Terrace, VIP Room)';

-- 5. TABLES
CREATE TABLE tables
(
    table_id   UUID PRIMARY KEY           DEFAULT uuid_generate_v4(),
    zone_id    UUID              NOT NULL REFERENCES zones (zone_id) ON DELETE CASCADE,
    number     VARCHAR(50)       NOT NULL,
    seats      INT               NOT NULL,
    status     table_status_enum NOT NULL DEFAULT 'ACTIVE',
    closed     BOOLEAN           NOT NULL DEFAULT FALSE,
    deleted    BOOLEAN           NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ                DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ                DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_zone_table_number UNIQUE (zone_id, number)
);

COMMENT
ON TABLE tables IS 'Individual dining tables linked to zones';

-- 6. MEDIAS
CREATE TABLE medias
(
    media_id       UUID PRIMARY KEY           DEFAULT uuid_generate_v4(),
    entity_type    media_entity_type NOT NULL,
    entity_id      UUID              NOT NULL,
    url            VARCHAR(1024),
    alt_text       VARCHAR(255),
    primary_entity BOOLEAN                    DEFAULT FALSE,
    media_type     media_type_enum            DEFAULT 'IMAGE',
    svg_content    TEXT,
    map_entity     BOOLEAN                    DEFAULT FALSE,
    deleted        BOOLEAN           NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ                DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ                DEFAULT CURRENT_TIMESTAMP
);

COMMENT
ON TABLE medias IS 'Stores images, videos, and inline SVGs for floor plans';

-- 7. RESTAURANT_SCHEDULES
CREATE TABLE restaurant_schedules
(
    schedule_id   UUID PRIMARY KEY  DEFAULT uuid_generate_v4(),
    restaurant_id UUID     NOT NULL REFERENCES restaurants (restaurant_id) ON DELETE CASCADE,
    day_of_week   SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    open_time     TIME WITHOUT TIME ZONE NOT NULL,
    close_time    TIME WITHOUT TIME ZONE NOT NULL,
    closed        BOOLEAN           DEFAULT FALSE NOT NULL,
    deleted       BOOLEAN  NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ       DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ       DEFAULT CURRENT_TIMESTAMP
);

COMMENT
ON TABLE restaurant_schedules IS 'General operating hours for the entire restaurant business';

-- 8. BRANCH_SCHEDULES
CREATE TABLE branch_schedules
(
    schedule_id UUID PRIMARY KEY  DEFAULT uuid_generate_v4(),
    branch_id   UUID     NOT NULL REFERENCES branches (branch_id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    open_time   TIME WITHOUT TIME ZONE NOT NULL,
    close_time  TIME WITHOUT TIME ZONE NOT NULL,
    closed      BOOLEAN           DEFAULT FALSE NOT NULL,
    deleted     BOOLEAN  NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ       DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ       DEFAULT CURRENT_TIMESTAMP
);

COMMENT
ON TABLE branch_schedules IS 'Specific operating hours per physical branch';

-- === INDEXES ===
CREATE INDEX idx_staffs_user_id ON restaurant_staffs (user_id);
CREATE INDEX idx_restaurants_geo ON restaurants USING GIST(location);
CREATE INDEX idx_medias_lookup ON medias (entity_type, entity_id);

-- === TRIGGERS ===
CREATE TRIGGER trg_restaurants_upd
    BEFORE UPDATE
    ON restaurants
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_staffs_upd
    BEFORE UPDATE
    ON restaurant_staffs
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
CREATE TRIGGER trg_res_schedules_upd
    BEFORE UPDATE
    ON restaurant_schedules
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER trg_br_schedules_upd
    BEFORE UPDATE
    ON branch_schedules
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
