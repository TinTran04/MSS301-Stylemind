-- Init script for payment_db
-- Transactions

-- Transactions
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    transaction_ref VARCHAR(100),
    transfer_content VARCHAR(100),
    gateway_transaction_id VARCHAR(100) UNIQUE,
    expires_at TIMESTAMP,
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_transactions_order_id ON transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_transfer_content ON transactions(transfer_content);

-- Webhook delivery/audit log - one row per SePay webhook attempt (see §SEC-06:
-- idempotency + reconciliation). Never stores the webhook's Authorization/API key.
CREATE TABLE IF NOT EXISTS payment_webhook_events (
    id VARCHAR(50) PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    gateway_transaction_id VARCHAR(100),
    transaction_id VARCHAR(50),
    transfer_content VARCHAR(200),
    amount DECIMAL(12, 2),
    result VARCHAR(30) NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_webhook_events_gateway_txn_id ON payment_webhook_events(gateway_transaction_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_webhook_events_provider_gateway_txn
    ON payment_webhook_events(provider, gateway_transaction_id)
    WHERE gateway_transaction_id IS NOT NULL;
