ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS order_cancellations (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    cancellation_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    customer_note TEXT,
    admin_note TEXT,
    rejection_reason TEXT,
    requested_by VARCHAR(50) NOT NULL,
    reviewed_by VARCHAR(50),
    requested_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    approved_at TIMESTAMP,
    idempotency_key VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_cancellations_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_order_cancellations_type
        CHECK (cancellation_type IN ('CUSTOMER_DIRECT', 'CUSTOMER_REQUEST', 'ADMIN_DIRECT')),
    CONSTRAINT chk_order_cancellations_status
        CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_order_cancellations_order_id
    ON order_cancellations(order_id);
CREATE INDEX IF NOT EXISTS idx_order_cancellations_user_id
    ON order_cancellations(user_id);
CREATE INDEX IF NOT EXISTS idx_order_cancellations_status
    ON order_cancellations(status);
CREATE INDEX IF NOT EXISTS idx_order_cancellations_created_at_desc
    ON order_cancellations(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_cancellations_order_created_at_desc
    ON order_cancellations(order_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS ux_order_cancellations_order_requested
    ON order_cancellations(order_id)
    WHERE status = 'REQUESTED';

CREATE UNIQUE INDEX IF NOT EXISTS ux_order_cancellations_idempotency
    ON order_cancellations(requested_by, order_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
