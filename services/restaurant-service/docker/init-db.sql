-- ============================================================================
-- RESTAURANT SERVICE - Optimized Database Schema with Soft-Delete and Inline SVG Support
-- Database: restaurant_service_db
-- Schema: restaurant_service
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS restaurant_service;
SET search_path TO restaurant_service, public;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Set timezone to UTC for all timestamp operations
SET timezone = 'UTC';

-- Set client encoding to UTF8 to handle Unicode characters
SET client_encoding = 'UTF8';

-- Suppress NOTICE messages
SET client_min_messages TO WARNING;

-- === Create ENUM types for medias entity_type and media_type with uppercase values ===
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'media_entity_type') THEN
CREATE TYPE media_entity_type AS ENUM ('RESTAURANT', 'BRANCH', 'ZONE', 'TABLE');
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'media_type_enum') THEN
CREATE TYPE media_type_enum AS ENUM ('IMAGE', 'VIDEO', 'SVG');
END IF;
END$$;

-- === Create uppercase ENUM type for table status ===
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'table_status_enum') THEN
CREATE TYPE table_status_enum AS ENUM ('ACTIVE', 'RESERVED', 'OCCUPIED', 'CLOSED');
END IF;
END$$;

-- === Function to auto-update updated_at timestamp ===
CREATE OR REPLACE FUNCTION restaurant_service.update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- === Users Table (Assumed to exist) ===
-- CREATE TABLE users (
--     user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
--     username VARCHAR(255) UNIQUE NOT NULL
-- );

-- === Restaurants Table ===
CREATE TABLE restaurants
(
    restaurant_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_user_id UUID NOT NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    address       VARCHAR(512) NOT NULL,
    location      GEOGRAPHY(POINT, 4326),
    phone         VARCHAR(20),
    website       VARCHAR(255),
    rating        NUMERIC(2, 1) DEFAULT 0 CHECK (rating >= 0 AND rating <= 5),
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_restaurants_is_deleted ON restaurants(is_deleted);

CREATE TRIGGER trg_restaurants_updated_at
    BEFORE UPDATE ON restaurants
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT ON TABLE restaurants IS 'Primary table for restaurants';
COMMENT ON COLUMN restaurants.owner_user_id IS 'Reference to user who owns the restaurant';
COMMENT ON COLUMN restaurants.location IS 'Geographical point (longitude, latitude)';
COMMENT ON COLUMN restaurants.is_deleted IS 'Soft-delete flag';
COMMENT ON COLUMN restaurants.deleted_at IS 'Timestamp when soft-deleted';

-- === Branches Table ===
CREATE TABLE branches
(
    branch_id     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    restaurant_id UUID NOT NULL REFERENCES restaurants (restaurant_id) ON DELETE CASCADE,
    address       VARCHAR(512) NOT NULL,
    phone         VARCHAR(20),
    timezone      VARCHAR(50) NOT NULL,
    location      GEOGRAPHY(POINT, 4326),
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_branches_is_deleted ON branches(is_deleted);

CREATE TRIGGER trg_branches_updated_at
    BEFORE UPDATE ON branches
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT ON TABLE branches IS 'Branches of restaurants';
COMMENT ON COLUMN branches.restaurant_id IS 'Reference to parent restaurant';
COMMENT ON COLUMN branches.is_deleted IS 'Soft-delete flag';
COMMENT ON COLUMN branches.deleted_at IS 'Timestamp when soft-deleted';

-- === Zones Table ===
CREATE TABLE zones
(
    zone_id     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    branch_id   UUID NOT NULL REFERENCES branches (branch_id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    capacity    INT NOT NULL,
    is_closed   BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_zones_is_deleted ON zones(is_deleted);

CREATE TRIGGER trg_zones_updated_at
    BEFORE UPDATE ON zones
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT ON TABLE zones IS 'Zones inside a branch, e.g., dining area sections';
COMMENT ON COLUMN zones.is_closed IS 'Indicates if the zone is manually closed';
COMMENT ON COLUMN zones.is_deleted IS 'Soft-delete flag';
COMMENT ON COLUMN zones.deleted_at IS 'Timestamp when soft-deleted';

-- === Tables Table ===
CREATE TABLE tables
(
    table_id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    zone_id    UUID NOT NULL REFERENCES zones (zone_id) ON DELETE CASCADE,
    number     VARCHAR(50) NOT NULL,
    seats      INT NOT NULL,
    status     table_status_enum NOT NULL DEFAULT 'ACTIVE',
    is_closed  BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_zone_table_number UNIQUE (zone_id, number)
);

CREATE INDEX idx_tables_is_deleted ON tables(is_deleted);

-- Create a single trigger on tables for BEFORE UPDATE
CREATE TRIGGER trg_tables_updated_at
    BEFORE UPDATE ON tables
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT ON TABLE tables IS 'Tables within zones';
COMMENT ON COLUMN tables.number IS 'Table number or identifier within the zone';
COMMENT ON CONSTRAINT unique_zone_table_number ON tables IS 'Enforces unique table number per zone';
COMMENT ON COLUMN tables.status IS 'Table status with uppercase enum values';
COMMENT ON COLUMN tables.is_deleted IS 'Soft-delete flag';
COMMENT ON COLUMN tables.deleted_at IS 'Timestamp when soft-deleted';

-- === Medias Table (includes inline SVG content) ===
CREATE TABLE medias
(
    media_id    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type media_entity_type NOT NULL,
    entity_id   UUID NOT NULL,
    url         VARCHAR(1024),
    alt_text    VARCHAR(255),
    is_primary  BOOLEAN DEFAULT FALSE,
    media_type  media_type_enum DEFAULT 'IMAGE',
    svg_content TEXT, -- Inline SVG XML content for vector images
    is_map      BOOLEAN DEFAULT FALSE, -- Flag to indicate if this media is a map
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_medias_is_deleted ON medias(is_deleted);
CREATE INDEX idx_medias_is_map ON medias(is_map);
CREATE INDEX idx_media_lookup ON medias (entity_type, entity_id);

CREATE TRIGGER trg_medias_updated_at
    BEFORE UPDATE ON medias
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT ON TABLE medias IS 'Media (images/videos/SVGs) linked to restaurant structure entities';
COMMENT ON COLUMN medias.entity_type IS 'Type of entity the media is linked to';
COMMENT ON COLUMN medias.is_primary IS 'Indicates if this media is the primary media for the entity';
COMMENT ON COLUMN medias.is_map IS 'Indicates if this media represents a map (e.g., floor plan)';
COMMENT ON COLUMN medias.svg_content IS 'Stores inline SVG XML content for vector images';
COMMENT ON COLUMN medias.is_deleted IS 'Soft-delete flag';
COMMENT ON COLUMN medias.deleted_at IS 'Timestamp when soft-deleted';

-- === Restaurant Schedules Table ===
CREATE TABLE restaurant_schedules
(
    schedule_id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    restaurant_id UUID NOT NULL REFERENCES restaurants (restaurant_id) ON DELETE CASCADE,
    day_of_week   SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6), -- 0=Sunday ... 6=Saturday
    open_time     TIME WITHOUT TIME ZONE NOT NULL,
    close_time    TIME WITHOUT TIME ZONE NOT NULL,
    is_closed     BOOLEAN DEFAULT FALSE NOT NULL,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_restaurant_schedule_restaurant_id ON restaurant_schedules (restaurant_id);
CREATE INDEX idx_restaurant_schedule_day_of_week ON restaurant_schedules (day_of_week);
CREATE INDEX idx_restaurant_schedule_is_deleted ON restaurant_schedules(is_deleted);

CREATE TRIGGER trg_restaurant_schedules_updated_at
    BEFORE UPDATE ON restaurant_schedules
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT ON TABLE restaurant_schedules IS 'Schedules per restaurant by day of week';
COMMENT ON COLUMN restaurant_schedules.is_deleted IS 'Soft-delete flag';
COMMENT ON COLUMN restaurant_schedules.deleted_at IS 'Timestamp when soft-deleted';

-- === Branch Schedules Table ===
CREATE TABLE branch_schedules
(
    schedule_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    branch_id   UUID NOT NULL REFERENCES branches (branch_id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6), -- 0=Sunday ... 6=Saturday
    open_time   TIME WITHOUT TIME ZONE NOT NULL,
    close_time  TIME WITHOUT TIME ZONE NOT NULL,
    is_closed   BOOLEAN DEFAULT FALSE NOT NULL,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_branch_schedule_branch_id ON branch_schedules (branch_id);
CREATE INDEX idx_branch_schedule_day_of_week ON branch_schedules (day_of_week);
CREATE INDEX idx_branch_schedule_is_deleted ON branch_schedules(is_deleted);

CREATE TRIGGER trg_branch_schedules_updated_at
    BEFORE UPDATE ON branch_schedules
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT ON TABLE branch_schedules IS 'Schedules per branch by day of week';
COMMENT ON COLUMN branch_schedules.is_deleted IS 'Soft-delete flag';
COMMENT ON COLUMN branch_schedules.deleted_at IS 'Timestamp when soft-deleted';

-- ============================================================================
-- End of schema
-- ============================================================================