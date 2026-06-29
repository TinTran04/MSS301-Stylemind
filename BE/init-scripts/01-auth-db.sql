-- Init script for auth_db
-- This script runs automatically when postgres container starts first time

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    full_name VARCHAR(150),
    provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    enabled BOOLEAN NOT NULL DEFAULT true,
    password_setup_required BOOLEAN NOT NULL DEFAULT false,
    password_setup_token_hash VARCHAR(255),
    password_setup_token_expires_at TIMESTAMP,
    password_reset_otp_hash VARCHAR(255),
    password_reset_otp_expires_at TIMESTAMP,
    password_reset_otp_attempts INTEGER NOT NULL DEFAULT 0,
    password_reset_requested_at TIMESTAMP,
    password_reset_token_hash VARCHAR(255),
    password_reset_token_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default admin user (password: admin123 - BCrypt encoded)
-- BCrypt hash for "admin123" with cost 12
INSERT INTO users (id, email, password_hash, full_name, provider, role)
VALUES ('usr_admin', 'admin@stylemind.ai', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.PZvO.S', 'System Admin', 'LOCAL', 'ADMIN')
ON CONFLICT (email) DO NOTHING;

-- Insert test customer user (password: customer123)
INSERT INTO users (id, email, password_hash, full_name, provider, role)
VALUES ('usr_customer', 'customer@stylemind.ai', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.PZvO.S', 'Test Customer', 'LOCAL', 'CUSTOMER')
ON CONFLICT (email) DO NOTHING;

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_provider ON users(provider, provider_id);
