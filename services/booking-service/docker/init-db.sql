-- ============================================================================
-- BOOKING SERVICE - Database Schema
-- Database: booking_service_db
-- Schema: booking_service
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS booking_service;
SET search_path TO booking_service, public;

-- Set timezone to UTC for all timestamp operations
SET timezone = 'UTC';

-- Set client encoding to UTF-8 to handle Unicode characters
SET client_encoding = 'UTF8';

-- Suppress NOTICE messages
SET client_min_messages TO WARNING;

-- Set search_path after schema is created
SET search_path TO booking_service, public;

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
