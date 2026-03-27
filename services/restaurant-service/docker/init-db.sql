-- ============================================================================
-- RESTAURANT SERVICE - Optimized Database Schema with Uppercase ENUM Values
-- Database: restaurant_service_db
-- Schema: restaurant_service
-- ============================================================================

CREATE SCHEMA restaurant_service;
SET
search_path TO restaurant_service, public;

-- Enable required extensions
CREATE
EXTENSION "uuid-ossp";
CREATE
EXTENSION "pgcrypto";
CREATE
EXTENSION postgis;

-- Set timezone to UTC for all timestamp operations
SET
timezone = 'UTC';

-- Set client encoding to UTF8 to handle Unicode characters
SET
client_encoding = 'UTF8';

-- Suppress NOTICE messages
SET
client_min_messages TO WARNING;

-- === Create ENUM types for medias entity_type and media_type with uppercase values ===
DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'media_entity_type') THEN
CREATE TYPE media_entity_type AS ENUM ('RESTAURANT', 'BRANCH', 'ZONE', 'TABLE');
END IF;
    IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'media_type_enum') THEN
CREATE TYPE media_type_enum AS ENUM ('IMAGE', 'VIDEO');
END IF;
END$$;

-- === Function to auto-updateMedia updated_at timestamp ===
CREATE
OR REPLACE FUNCTION restaurant_service.update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

-- === Users Table (Referenced by restaurants.owner_user_id) ===
-- Assuming users table exists; if not, define it minimally here for FK support:
-- CREATE TABLE users (
--     user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
--     username VARCHAR(255) UNIQUE NOT NULL
-- );

-- === Restaurants Table ===
CREATE TABLE restaurants
(
    restaurant_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_user_id UUID         NOT NULL REFERENCES users (user_id),
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    address       VARCHAR(512) NOT NULL,
    location      GEOGRAPHY(POINT, 4326),
    phone         VARCHAR(20),
    website       VARCHAR(255),
    rating        NUMERIC(2, 1)    DEFAULT 0 CHECK (rating >= 0 AND rating <= 5),
    created_at    TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_restaurants_updated_at
    BEFORE UPDATE
    ON restaurants
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT
ON TABLE restaurants IS 'Primary table for restaurants';
COMMENT
ON COLUMN restaurants.owner_user_id IS 'Reference to user who owns the restaurant';
COMMENT
ON COLUMN restaurants.location IS 'Geographical point (longitude, latitude)';

-- === Branches Table ===
CREATE TABLE branches
(
    branch_id     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    restaurant_id UUID         NOT NULL REFERENCES restaurants (restaurant_id) ON DELETE CASCADE,
    address       VARCHAR(512) NOT NULL,
    phone         VARCHAR(20),
    timezone      VARCHAR(50)  NOT NULL,
    location      GEOGRAPHY(POINT, 4326),
    created_at    TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_branches_updated_at
    BEFORE UPDATE
    ON branches
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT
ON TABLE branches IS 'Branches of restaurants';
COMMENT
ON COLUMN branches.restaurant_id IS 'Reference to parent restaurant';

-- === Zones Table ===
CREATE TABLE zones
(
    zone_id     UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    branch_id   UUID         NOT NULL REFERENCES branches (branch_id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    capacity    INT          NOT NULL,
    is_closed   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_zones_updated_at
    BEFORE UPDATE
    ON zones
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT
ON TABLE zones IS 'Zones inside a branch, e.g., dining area sections';
COMMENT
ON COLUMN zones.is_closed IS 'Indicates if the zone is manually closed';

-- === Tables Table ===
CREATE TABLE tables
(
    table_id   UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    zone_id    UUID        NOT NULL REFERENCES zones (zone_id) ON DELETE CASCADE,
    number     VARCHAR(50) NOT NULL,
    seats      INT         NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'active',
    is_closed  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_zone_table_number UNIQUE (zone_id, number)
);

CREATE TRIGGER trg_tables_updated_at
    BEFORE UPDATE
    ON tables
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT
ON TABLE tables IS 'Tables within zones';
COMMENT
ON COLUMN tables.number IS 'Table number or identifier within the zone';
COMMENT
ON CONSTRAINT unique_zone_table_number ON tables IS 'Enforces unique table number per zone';

-- === Medias Table ===
CREATE TABLE medias
(
    media_id    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type media_entity_type NOT NULL,
    entity_id   UUID              NOT NULL,
    url         VARCHAR(1024)     NOT NULL,
    alt_text    VARCHAR(255),
    is_primary  BOOLEAN          DEFAULT FALSE,
    media_type  media_type_enum  DEFAULT 'IMAGE',
    created_at  TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_media_lookup ON medias (entity_type, entity_id);

CREATE TRIGGER trg_medias_updated_at
    BEFORE UPDATE
    ON medias
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT
ON TABLE medias IS 'Media (images/videos) linked to restaurant structure entities';
COMMENT
ON COLUMN medias.entity_type IS 'Type of entity the media is linked to';
COMMENT
ON COLUMN medias.is_primary IS 'Indicates if this media is the primary media for the entity';

-- === Restaurant Schedules Table ===
CREATE TABLE restaurant_schedules
(
    schedule_id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    restaurant_id UUID                           NOT NULL REFERENCES restaurants (restaurant_id) ON DELETE CASCADE,
    day_of_week   SMALLINT                       NOT NULL CHECK (day_of_week BETWEEN 0 AND 6), -- 0=Sunday ... 6=Saturday
    open_time     TIME WITHOUT TIME ZONE NOT NULL,
    close_time    TIME WITHOUT TIME ZONE NOT NULL,
    is_closed     BOOLEAN          DEFAULT FALSE NOT NULL,
    created_at    TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_restaurant_schedule_restaurant_id ON restaurant_schedules (restaurant_id);
CREATE INDEX idx_restaurant_schedule_day_of_week ON restaurant_schedules (day_of_week);

CREATE TRIGGER trg_restaurant_schedules_updated_at
    BEFORE UPDATE
    ON restaurant_schedules
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT
ON TABLE restaurant_schedules IS 'Schedules per restaurant by day of week';

-- === Branch Schedules Table ===
CREATE TABLE branch_schedules
(
    schedule_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    branch_id   UUID                           NOT NULL REFERENCES branches (branch_id) ON DELETE CASCADE,
    day_of_week SMALLINT                       NOT NULL CHECK (day_of_week BETWEEN 0 AND 6), -- 0=Sunday ... 6=Saturday
    open_time   TIME WITHOUT TIME ZONE NOT NULL,
    close_time  TIME WITHOUT TIME ZONE NOT NULL,
    is_closed   BOOLEAN          DEFAULT FALSE NOT NULL,
    created_at  TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_branch_schedule_branch_id ON branch_schedules (branch_id);
CREATE INDEX idx_branch_schedule_day_of_week ON branch_schedules (day_of_week);

CREATE TRIGGER trg_branch_schedules_updated_at
    BEFORE UPDATE
    ON branch_schedules
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

COMMENT
ON TABLE branch_schedules IS 'Schedules per branch by day of week';

-- === Additional Triggers for zones and tables is_closed updates ===
CREATE TRIGGER trg_zones_updated_at_is_closed
    BEFORE UPDATE
    ON zones
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

CREATE TRIGGER trg_tables_updated_at_is_closed
    BEFORE UPDATE
    ON tables
    FOR EACH ROW EXECUTE PROCEDURE restaurant_service.update_modified_column();

-- ============================================================================
-- End of schema
-- ============================================================================