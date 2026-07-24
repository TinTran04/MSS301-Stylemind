CREATE TABLE IF NOT EXISTS order_return_requests (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(50) NOT NULL,
    status VARCHAR(40) NOT NULL,
    reason_code VARCHAR(80) NOT NULL,
    customer_note TEXT,
    admin_note TEXT,
    rejection_reason TEXT,
    bank_name VARCHAR(120),
    bank_account_number VARCHAR(80),
    bank_account_holder VARCHAR(150),
    bank_branch VARCHAR(150),
    refund_reference VARCHAR(150),
    refund_note TEXT,
    requested_by VARCHAR(50) NOT NULL,
    reviewed_by VARCHAR(50),
    processed_by VARCHAR(50),
    requested_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    approved_at TIMESTAMP,
    bank_info_submitted_at TIMESTAMP,
    processed_at TIMESTAMP,
    idempotency_key VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_return_requests_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_order_return_requests_status
        CHECK (status IN ('REQUESTED', 'AWAITING_BANK_INFO', 'BANK_INFO_SUBMITTED', 'REFUNDED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_order_return_requests_user_id
    ON order_return_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_order_return_requests_status
    ON order_return_requests(status);
CREATE INDEX IF NOT EXISTS idx_order_return_requests_created_at_desc
    ON order_return_requests(created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS ux_order_return_requests_idempotency
    ON order_return_requests(requested_by, order_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS order_return_attachments (
    id VARCHAR(50) PRIMARY KEY,
    return_request_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    owner VARCHAR(20) NOT NULL,
    kind VARCHAR(40) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    image_data BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_return_attachments_request
        FOREIGN KEY (return_request_id) REFERENCES order_return_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_return_attachments_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_order_return_attachments_owner
        CHECK (owner IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT chk_order_return_attachments_kind
        CHECK (kind IN ('CUSTOMER_PROOF', 'ADMIN_REJECTION', 'ADMIN_BILL'))
);

CREATE INDEX IF NOT EXISTS idx_order_return_attachments_request_id
    ON order_return_attachments(return_request_id);
CREATE INDEX IF NOT EXISTS idx_order_return_attachments_order_id
    ON order_return_attachments(order_id);
