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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_webhook_events_gateway_txn_id ON payment_webhook_events(gateway_transaction_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_webhook_events_provider_gateway_txn
    ON payment_webhook_events(provider, gateway_transaction_id)
    WHERE gateway_transaction_id IS NOT NULL;

-- Seed Data for Transactions
INSERT INTO transactions (id, order_id, user_id, amount, method, status, transaction_ref, gateway_transaction_id, paid_at)
VALUES
    ('txn_001', 'order_001', 'usr_customer', 687000.00, 'SEPAY_QR', 'COMPLETED', 'STYLEMIND-001', 'SEPAY-TXN-001', NOW() - INTERVAL '2 days'),
    ('txn_002', 'order_002', 'usr_customer', 1299000.00, 'SEPAY_QR', 'PENDING', 'STYLEMIND-002', 'SEPAY-TXN-002', NULL),
    ('txn_003', 'order_003', 'usr_admin', 229000.00, 'SEPAY_QR', 'COMPLETED', 'STYLEMIND-003', 'SEPAY-TXN-003', NOW() - INTERVAL '5 days'),
    ('txn_004', 'order_004', 'usr_001', 458000.00, 'SEPAY_QR', 'COMPLETED', 'STYLEMIND-004', 'SEPAY-TXN-004', NOW() - INTERVAL '3 days'),
    ('txn_005', 'order_005', 'usr_002', 359000.00, 'SEPAY_QR', 'PENDING', 'STYLEMIND-005', 'SEPAY-TXN-005', NULL),
    ('txn_006', 'order_006', 'usr_003', 849000.00, 'SEPAY_QR', 'COMPLETED', 'STYLEMIND-006', 'SEPAY-TXN-006', NOW() - INTERVAL '1 day'),
    ('txn_007', 'order_007', 'usr_004', 1149000.00, 'SEPAY_QR', 'FAILED', 'STYLEMIND-007', 'SEPAY-TXN-007', NULL),
    ('txn_008', 'order_008', 'usr_005', 539000.00, 'SEPAY_QR', 'COMPLETED', 'STYLEMIND-008', 'SEPAY-TXN-008', NOW() - INTERVAL '6 hours')
ON CONFLICT (gateway_transaction_id) DO UPDATE SET order_id = EXCLUDED.order_id, user_id = EXCLUDED.user_id, amount = EXCLUDED.amount, method = EXCLUDED.method, status = EXCLUDED.status, transaction_ref = EXCLUDED.transaction_ref, paid_at = EXCLUDED.paid_at;
