-- Init script for auth_db
-- This script runs automatically when postgres container starts first time

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER'
        CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'ADMIN')),
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT ck_users_account_status
        CHECK (account_status IN ('ACTIVE', 'DISABLED')),
    password_setup_required BOOLEAN NOT NULL DEFAULT false,
    password_setup_token_hash VARCHAR(255),
    password_setup_token_expires_at TIMESTAMP,
    password_reset_otp_hash VARCHAR(255),
    password_reset_otp_expires_at TIMESTAMP,
    password_reset_otp_attempts INTEGER NOT NULL DEFAULT 0,
    password_reset_requested_at TIMESTAMP,
    password_reset_token_hash VARCHAR(255),
    password_reset_token_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Enable pgcrypto for BCrypt password hashing
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Default admin user
-- email: admin@stylemind.ai
-- password: Admin@123
INSERT INTO users (id, email, password_hash, provider, role)
VALUES (
           'usr_admin',
           'admin@stylemind.ai',
           crypt('Admin@123', gen_salt('bf', 12)),
           'LOCAL',
           'ADMIN'
       )
    ON CONFLICT (email) DO UPDATE
                               SET password_hash = EXCLUDED.password_hash,
                               provider = EXCLUDED.provider,
                               role = EXCLUDED.role;

-- Test customer user
-- email: customer@stylemind.ai
-- password: Customer@123
INSERT INTO users (id, email, password_hash, provider, role)
VALUES (
           'usr_customer',
           'customer@stylemind.ai',
           crypt('Customer@123', gen_salt('bf', 12)),
           'LOCAL',
           'CUSTOMER'
       )
    ON CONFLICT (email) DO UPDATE
                               SET password_hash = EXCLUDED.password_hash,
                               provider = EXCLUDED.provider,
                               role = EXCLUDED.role;

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_provider ON users(provider, provider_id);

-- Admin audit log for destructive actions (matches auth-service Flyway V3)
CREATE TABLE IF NOT EXISTS audit_log (
    id VARCHAR(50) PRIMARY KEY,
    actor_user_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_user_id VARCHAR(50) NOT NULL,
    detail VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_log_target_user ON audit_log(target_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor_user ON audit_log(actor_user_id);

-- AUTH-REG-OTP: stage unverified sign-ups until the email OTP is confirmed.
-- Kept separate from `users` so verified-account, login, admin and
-- forgot-password flows are unaffected. Row is created on register and
-- removed once the OTP is verified (which creates the real ACTIVE user).
CREATE TABLE IF NOT EXISTS pending_registrations (
    id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    otp_expires_at TIMESTAMP NOT NULL,
    otp_attempts INTEGER NOT NULL DEFAULT 0,
    requested_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pending_registrations_email ON pending_registrations(email);
