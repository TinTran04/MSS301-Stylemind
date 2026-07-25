-- AUTH-REG-OTP: stage unverified sign-ups until the email OTP is confirmed.
-- Kept separate from `users` so the verified-account, login, admin and
-- forgot-password flows are entirely unaffected. A row is created on
-- POST /api/v1/auth/register and deleted once the OTP is verified (which
-- is when the real ACTIVE user row is created).
CREATE TABLE IF NOT EXISTS pending_registrations (
    id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    otp_expires_at TIMESTAMP NOT NULL,
    otp_attempts INTEGER NOT NULL DEFAULT 0,
    requested_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pending_registrations_email ON pending_registrations(email);
