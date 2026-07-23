ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS order_cancellation_id VARCHAR(50);

CREATE TABLE IF NOT EXISTS refund_transactions (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    payment_transaction_id VARCHAR(50) NOT NULL,
    order_cancellation_id VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    method VARCHAR(30) NOT NULL,
    provider_reference VARCHAR(150),
    proof_url TEXT,
    note TEXT,
    processed_by VARCHAR(50),
    processed_at TIMESTAMP,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_refund_transactions_payment_transaction
        FOREIGN KEY (payment_transaction_id) REFERENCES transactions(id),
    CONSTRAINT ux_refund_transactions_order_cancellation
        UNIQUE (order_cancellation_id),
    CONSTRAINT chk_refund_transactions_status
        CHECK (status IN ('REFUND_PENDING', 'REFUNDED', 'REFUND_FAILED')),
    CONSTRAINT chk_refund_transactions_method
        CHECK (method IN ('MANUAL_BANK_TRANSFER'))
);

CREATE INDEX IF NOT EXISTS idx_refund_transactions_order_id
    ON refund_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_refund_transactions_status
    ON refund_transactions(status);
