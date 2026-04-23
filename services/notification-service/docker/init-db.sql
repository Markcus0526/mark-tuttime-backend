-- ============================================================================
-- NOTIFICATION SERVICE - Database Schema
-- Database: notification_service_db
-- Schema: notification_service
-- 
-- NOTE: This script runs automatically when the database is first initialized.
-- If the database already exists, you need to drop the volume and recreate it.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS notification_service;
SET
search_path TO notification_service, public;

-- Set timezone to UTC for all timestamp operations
SET
timezone = 'UTC';

-- Set client encoding to UTF-8 to handle Unicode characters
SET
client_encoding = 'UTF8';

-- Suppress NOTICE messages
SET
client_min_messages TO WARNING;

-- Set search_path after schema is created
SET
search_path TO notification_service, public;

-- Enable required PostgreSQL extensions
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

CREATE SCHEMA IF NOT EXISTS notification_service;
SET
search_path TO notification_service, public;

-- TutTime Notification Service - Full Support Database Schema
-- Optimized for Spring Boot 3.3.13, gRPC, and Kafka

CREATE SCHEMA IF NOT EXISTS notification_service;

-- 1. Custom Types for consistency
CREATE TYPE notification_channel AS ENUM ('EMAIL', 'SMS', 'PUSH', 'WEB_PUSH', 'IN_APP');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'CANCELLED');
CREATE TYPE notification_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'URGENT');

-- 2. Templates Table
-- Stores message structures with placeholders like {{user_name}}
CREATE TABLE notification_templates (
                                        id              SERIAL PRIMARY KEY,
                                        template_key    VARCHAR(100) NOT NULL,
                                        channel         notification_channel NOT NULL,
                                        language        VARCHAR(10) DEFAULT 'en' NOT NULL,
                                        title           VARCHAR(255), -- Email subject or Push title
                                        content         TEXT NOT NULL, -- The body (Markdown or HTML)
                                        active       BOOLEAN DEFAULT TRUE,
                                        created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                                        updated_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                                        UNIQUE (template_key, channel, language)
);

-- 3. User Preferences Table
-- Controls "Opt-in/Opt-out" settings per user
CREATE TABLE user_notification_preferences (
                                               id                  SERIAL PRIMARY KEY,
                                               user_id             UUID NOT NULL, -- From User Service
                                               notification_type   VARCHAR(50) NOT NULL, -- e.g., 'MARKETING', 'BOOKING'
                                               channel             notification_channel NOT NULL,
                                               enabled          BOOLEAN DEFAULT TRUE,
                                               updated_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                                               UNIQUE (user_id, notification_type, channel)
);

-- 4. Notification Logs Table
-- The history of every message sent
CREATE TABLE notification_logs (
                                   id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   user_id             UUID NOT NULL,
                                   template_key        VARCHAR(100),
                                   event_id            VARCHAR(255),
                                   channel             notification_channel NOT NULL,
                                   recipient           VARCHAR(255) NOT NULL, -- Email address or Phone number
                                   status              notification_status DEFAULT 'PENDING',
                                   priority            notification_priority DEFAULT 'MEDIUM',
                                   provider_id         VARCHAR(255), -- External ID from Twilio/SendGrid
                                   provider_response   JSONB, -- Raw error or success response
                                   retry_count         INT DEFAULT 0,
                                   scheduled_at        TIMESTAMPTZ,
                                   sent_at             TIMESTAMPTZ,
                                   created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                                   UNIQUE (event_id, channel)
);

-- 5. Indexes for performance
CREATE INDEX idx_notif_logs_user_id ON notification_logs(user_id);
CREATE INDEX idx_notif_logs_status ON notification_logs(status) WHERE status = 'FAILED';
CREATE INDEX idx_user_pref_lookup ON user_notification_preferences(user_id);



