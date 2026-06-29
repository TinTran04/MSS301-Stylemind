CREATE UNIQUE INDEX uk_email_verification_tokens_token_hash
    ON email_verification_tokens (token_hash);

CREATE UNIQUE INDEX uk_password_reset_tokens_token_hash
    ON password_reset_tokens (token_hash);
