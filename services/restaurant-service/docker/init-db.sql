-- ============================================================================
-- RESTAURANT SERVICE - Database Schema
-- Database: restaurant_service_db
-- Schema: restaurant_service
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS restaurant_service;
SET search_path TO restaurant_service, public;

-- Set timezone to UTC for all timestamp operations
SET timezone = 'UTC';

-- Set client encoding to UTF-8 to handle Unicode characters
SET client_encoding = 'UTF8';

-- Suppress NOTICE messages
SET client_min_messages TO WARNING;

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS postgis; -- Required for GEOGRAPHY/GEOMETRY

-- Restaurant Table
CREATE TABLE restaurants (
    restaurant_id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(user_id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address VARCHAR(512) NOT NULL,
    location GEOGRAPHY(POINT, 4326),
    phone VARCHAR(20),
    website VARCHAR(255),
    rating NUMERIC(2,1) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Branch Table
CREATE TABLE branches (
    branch_id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurant(restaurant_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(512) NOT NULL,
    phone VARCHAR(20),
    timezone VARCHAR(50) NOT NULL,
    location GEOGRAPHY(POINT, 4326),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Zone Table
CREATE TABLE zones (
    zone_id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branch(branch_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    capacity INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table Table (named "table" is a reserved keyword, so quoted)
CREATE TABLE tables (
    table_id BIGSERIAL PRIMARY KEY,
    zone_id BIGINT NOT NULL REFERENCES zone(zone_id) ON DELETE CASCADE,
    number VARCHAR(50) NOT NULL,
    seats INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Media Table for Images/Videos linked to Restaurant Structure entities
CREATE TABLE medias (
    media_id BIGSERIAL PRIMARY KEY,
    entity_type INTEGER NOT NULL, -- 'RESTAURANT', 'BRANCH', 'ZONE', 'TABLE'
    entity_id BIGINT NOT NULL,
    url VARCHAR(1024) NOT NULL,
    alt_text VARCHAR(255),
    is_primary BOOLEAN DEFAULT FALSE,
    media_type INTEGER DEFAULT 1, -- 'image', 'video'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for polymorphic lookups
CREATE INDEX idx_media_lookup ON media(entity_type, entity_id);

-- Add Auto-Update Triggers
CREATE TRIGGER update_res_modtime BEFORE UPDATE ON restaurants FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_branch_modtime BEFORE UPDATE ON branches FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_zone_modtime BEFORE UPDATE ON zones FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_table_modtime BEFORE UPDATE ON tables FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_media_modtime BEFORE UPDATE ON medias FOR EACH ROW EXECUTE PROCEDURE update_modified_column();