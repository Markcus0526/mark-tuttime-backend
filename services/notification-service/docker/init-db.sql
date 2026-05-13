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

-- 1. Custom Types for consistency
CREATE TYPE notification_channel AS ENUM ('EMAIL', 'SMS', 'PUSH', 'WEB_PUSH', 'IN_APP');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'CANCELLED');
CREATE TYPE notification_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'URGENT');
CREATE TYPE notification_category AS ENUM ('ACCOUNT', 'BOOKING', 'PAYMENT', 'REVIEW', 'MARKETING');

DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notification_template_key') THEN
CREATE DOMAIN notification_template_key AS VARCHAR(100)
    CHECK (VALUE IN (
                     'USER_REGISTER', 'USER_LOGIN', -- Added USER_LOGIN here
                     'USER_VERIFICATION', 'PASSWORD_RESET', 'MFA_CODE',
                     'ACCOUNT_LOCKED', 'ADMIN_USER_CREATED', 'ADMIN_USER_BLOCKED', 'ADMIN_USER_UNBLOCKED',
                     'ADMIN_USER_DELETED',
                     'BOOKING_CREATED', 'BOOKING_REMINDER', 'BOOKING_CANCELED',
                     'BOOKING_COMPLETED', 'REVIEW_REQUEST',
                     'MANAGER_BOOKING_ALERT', 'MANAGER_BOOKING_CANCEL', 'MANAGER_LOW_AVAILABILITY',
                     'PAYMENT_SUCCESS', 'PAYMENT_FAILED', 'PAYMENT_REFUNDED',
                     'REVIEW_APPROVED', 'REVIEW_REJECTED'
        ));
END IF;
END $$;

-- 2. Templates Table
-- Stores message structures with placeholders like {{user_name}}
CREATE TABLE notification_templates
(
    id           SERIAL PRIMARY KEY,
    template_key notification_template_key NOT NULL,
    channel      notification_channel      NOT NULL,
    language     VARCHAR(10) DEFAULT 'en'  NOT NULL,
    title        VARCHAR(255),                       -- Email subject or Push title
    content      TEXT                      NOT NULL, -- The body (Markdown or HTML)
    active       BOOLEAN     DEFAULT TRUE,
    created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (template_key, channel, language)
);

-- 3. User Preferences Table
-- Controls "Opt-in/Opt-out" settings per user
CREATE TABLE user_notification_preferences
(
    id                SERIAL PRIMARY KEY,
    user_id           UUID                 NOT NULL, -- From User Service
    notification_type notification_category DEFAULT 'ACCOUNT',
    channel           notification_channel NOT NULL,
    enabled           BOOLEAN               DEFAULT TRUE,
    updated_at        TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, notification_type, channel)
);

-- 4. Notification Logs Table
-- The history of every message sent
CREATE TABLE notification_logs
(
    id                UUID PRIMARY KEY                   DEFAULT gen_random_uuid(),
    user_id           UUID                      NOT NULL,
    template_id       INTEGER REFERENCES notification_templates (id),
    template_key      notification_template_key NOT NULL,
    event_id          UUID                      NOT NULL,
    channel           notification_channel      NOT NULL,
    recipient         VARCHAR(255)              NOT NULL, -- Email address or Phone number
    status            notification_status                DEFAULT 'PENDING',
    priority          notification_priority              DEFAULT 'MEDIUM',
    provider_id       VARCHAR(255),                       -- External ID from Twilio/SendGrid
    provider_response JSONB,                              -- Raw error or success response
    params            JSONB,
    retry_count       INT                                DEFAULT 0,
    scheduled_at      TIMESTAMPTZ,
    sent_at           TIMESTAMPTZ,
    version           BIGINT                    NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ                        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (event_id, channel)
);

-- 5. Indexes for performance
CREATE INDEX idx_notif_logs_user_id ON notification_logs (user_id);
CREATE INDEX idx_notif_logs_status ON notification_logs (status) WHERE status = 'FAILED';
CREATE INDEX idx_notif_logs_retry ON notification_logs (status, created_at) WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX idx_user_pref_lookup ON user_notification_preferences (user_id);
CREATE INDEX idx_notification_logs_template_id ON notification_logs (template_id);

SET
search_path TO notification_service, public;

INSERT INTO notification_templates (template_key, channel, language, title, content, active)
VALUES
-- 1. USER_REGISTER
('USER_REGISTER', 'EMAIL', 'en', 'Welcome to TutTime!', 'Hi {{userName}}, thanks for joining! Explore restaurants and book tables easily.',
 true),
('USER_REGISTER', 'SMS', 'en', NULL, 'Welcome to TutTime, {{userName}}! Your account is ready. Start exploring now: {{appLink}}',
 true),
('USER_REGISTER', 'IN_APP', 'en', 'New Registration', 'New user {{userName}} has just joined the platform.',
 true),

-- 2. USER_LOGIN
('USER_LOGIN', 'EMAIL', 'en', 'New Login Alert', 'Hello, a new login was detected on your account at {{time}}.',
 true),
('USER_LOGIN', 'SMS', 'en', NULL, 'TutTime Security: New login detected at {{time}}. If this wasn''t you, reset your password now.',
 true),
('USER_LOGIN', 'IN_APP', 'en', 'Security Alert', 'Your account was accessed from a new device/location.',
 true),

-- 3. USER_VERIFICATION
('USER_VERIFICATION', 'EMAIL', 'en', 'Verify Your Account', 'Your verification code is: <strong>{{code}}</strong>',
 true),
('USER_VERIFICATION', 'SMS', 'en', NULL, 'TutTime: Your verification code is {{code}}. Valid for 10 mins.',
 true),

-- 4. PASSWORD_RESET
('PASSWORD_RESET', 'EMAIL', 'en', 'Reset Your Password', 'Use code <strong>{{code}}</strong> to reset your password. Do not share this code.',
 true),
('PASSWORD_RESET', 'SMS', 'en', NULL, 'TutTime: Use code {{code}} to reset your password. Do not share this code.',
 true),

-- 5. ADMIN_USER_CREATED
('ADMIN_USER_CREATED', 'EMAIL', 'en', 'Admin Access Granted', 'You have been added as an admin. Your temp password is: {{tempPassword}}',
 true),
('ADMIN_USER_CREATED', 'SMS', 'en', NULL, 'TutTime Admin: Your account is ready. Temp password: {{tempPassword}}',
 true),
('ADMIN_USER_CREATED', 'IN_APP', 'en', 'Staff Added', 'New admin/staff member {{staffName}} has been created.',
 true),

-- 6. ADMIN_USER_BLOCKED
('ADMIN_USER_BLOCKED', 'EMAIL', 'en', 'Admin Access Granted', 'You have been blocked as an admin.',
 true),
('ADMIN_USER_BLOCKED', 'SMS', 'en', NULL, 'TutTime Admin: Your account is blocked.',
 true),
('ADMIN_USER_BLOCKED', 'IN_APP', 'en', 'Staff Added', 'Admin/staff member {{staffName}} has been blocked.',
 true),

-- 7. ADMIN_USER_UNBLOCKED
('ADMIN_USER_UNBLOCKED', 'EMAIL', 'en', 'Admin Access Granted', 'You have been unblocked as an admin.',
 true),
('ADMIN_USER_UNBLOCKED', 'SMS', 'en', NULL, 'TutTime Admin: Your account is unblocked.',
 true),
('ADMIN_USER_UNBLOCKED', 'IN_APP', 'en', 'Staff Added', 'Admin/staff member {{staffName}} has been unblocked.',
 true),

-- 8 ADMIN_USER_DELETED
('ADMIN_USER_DELETED', 'EMAIL', 'en', 'Admin Access Granted', 'You have been deleted as an admin.',
 true),
('ADMIN_USER_DELETED', 'SMS', 'en', NULL, 'TutTime Admin: Your account is deleted.',
 true),
('ADMIN_USER_DELETED', 'IN_APP', 'en', 'Staff Added', 'Admin/staff member {{staffName}} has been deleted.',
 true),

-- 9. BOOKING_CREATED
('BOOKING_CREATED', 'EMAIL', 'en', 'Booking Confirmed!', 'Reservation confirmed at {{restaurantName}} for {{bookingTime}}.',
 true),
('BOOKING_CREATED', 'SMS', 'en', NULL, 'TutTime: Booking confirmed @ {{restaurantName}} for {{bookingTime}}. See you there!',
 true),
('BOOKING_CREATED', 'IN_APP', 'en', 'New Booking', 'New reservation for {{guestCount}} guests at {{bookingTime}}.',
 true),

-- 10. BOOKING_CANCELED
('BOOKING_CANCELED', 'EMAIL', 'en', 'Booking Canceled', 'Your reservation at {{restaurantName}} has been canceled.',
 true),
('BOOKING_CANCELED', 'SMS', 'en', NULL, 'TutTime: Your booking at {{restaurantName}} for {{bookingTime}} was canceled.',
 true),
('BOOKING_CANCELED', 'IN_APP', 'en', 'Cancellation', 'Booking for {{userName}} at {{bookingTime}} was canceled.',
 true),

-- 11. BOOKING_COMPLETED
('BOOKING_COMPLETED', 'EMAIL', 'en', 'How was your meal?', 'Thanks for visiting {{restaurantName}}! Rate your experience here.',
 true),
('BOOKING_COMPLETED', 'SMS', 'en', NULL, 'TutTime: Thanks for dining @ {{restaurantName}}! Rate your visit: {{reviewLink}}',
 true),
('BOOKING_COMPLETED', 'IN_APP', 'en', 'Table Released', 'Booking for Table {{tableNumber}} is now marked as completed.',
 true),

-- 12. BOOKING_COMPLETED
('BOOKING_REMINDER', 'EMAIL', 'en', 'Reminder: Your table is waiting', 'Don''t forget your reservation at {{restaurantName}} today at {{bookingTime}}.',
 true),
('BOOKING_REMINDER', 'SMS', 'en', NULL, 'TutTime reminder: Your table at {{restaurantName}} at {{bookingTime}}.',
 true),

-- 13. PAYMENT_SUCCESS
('PAYMENT_SUCCESS', 'EMAIL', 'en', 'Payment Successful', 'Received {{amount}} for Order #{{orderId}}.',
 true),
('PAYMENT_SUCCESS', 'SMS', 'en', NULL, 'TutTime: Payment of {{amount}} confirmed for Order #{{orderId}}. Thanks!',
 true),
('PAYMENT_SUCCESS', 'IN_APP', 'en', 'Payment Received', 'Payment of {{amount}} confirmed for Order #{{orderId}}.',
 true),

-- 14. PAYMENT_REFUNDED
('PAYMENT_REFUNDED', 'EMAIL', 'en', 'Refund Processed', 'A refund of {{amount}} for Order #{{orderId}} has been issued.',
 true),
('PAYMENT_REFUNDED', 'SMS', 'en', NULL, 'TutTime: Refund of {{amount}} for Order #{{orderId}} has been issued to your account.',
 true),
('PAYMENT_REFUNDED', 'IN_APP', 'en', 'Refund Issued', 'Refund of {{amount}} processed for Order #{{orderId}}.',
 true),

-- 15. PAYMENT_FAILED
('PAYMENT_FAILED', 'EMAIL', 'en', 'Payment Failed', 'Your payment of {{amount}} could not be processed. Please try again or use another method.',
 true),
('PAYMENT_FAILED', 'SMS', 'en', NULL, 'TutTime: Payment failed for {{amount}}. Check your payment method.',
 true),
('PAYMENT_FAILED', 'IN_APP', 'en', 'Payment Failed', 'Your payment of {{amount}} failed. Please retry.',
 true),

-- 16. REVIEW_APPROVED
('REVIEW_APPROVED', 'EMAIL', 'en', 'Review Published!', 'Your review for {{restaurantName}} is now live!',
 true),
('REVIEW_APPROVED', 'SMS', 'en', NULL, 'TutTime: Your review for {{restaurantName}} is live! Thanks for the feedback.',
 true),
('REVIEW_APPROVED', 'IN_APP', 'en', 'New Review', 'User {{userName}} posted a new review for your restaurant.',
 true);
