CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    token_version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_accounts_email UNIQUE (email),
    CONSTRAINT chk_accounts_role CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT chk_accounts_token_version CHECK (token_version >= 0)
);

CREATE INDEX idx_accounts_email ON accounts (email);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    device_name VARCHAR(150),
    ip_address VARCHAR(45),
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_account
        FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_account_id ON refresh_tokens (account_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_tokens_account
        FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_tokens_account_id ON email_verification_tokens (account_id);
CREATE INDEX idx_email_verification_tokens_expires_at ON email_verification_tokens (expires_at);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_tokens_account
        FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_account_id ON password_reset_tokens (account_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    CONSTRAINT chk_outbox_events_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_events_status ON outbox_events (status);
CREATE INDEX idx_outbox_events_status_created_at ON outbox_events (status, created_at);
