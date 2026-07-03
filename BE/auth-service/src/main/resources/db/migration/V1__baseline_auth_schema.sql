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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_provider ON users(provider, provider_id);
