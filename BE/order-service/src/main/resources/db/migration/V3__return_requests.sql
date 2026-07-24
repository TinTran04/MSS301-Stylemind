-- Migration V3: Add Return Requests and related tables

CREATE TABLE IF NOT EXISTS return_requests (
    id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(32) NOT NULL,
    customer_note TEXT,
    admin_note TEXT,
    rejection_reason TEXT,
    is_physical_return BOOLEAN DEFAULT TRUE,
    payout_state VARCHAR(32) DEFAULT 'NOT_REQUIRED',
    refund_id VARCHAR(64),
    reviewed_by VARCHAR(64),
    requested_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    qc_completed_at TIMESTAMP,
    closed_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_return_requests_order_id ON return_requests(order_id);
CREATE INDEX IF NOT EXISTS idx_return_requests_user_id ON return_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_return_requests_status ON return_requests(status);
CREATE INDEX IF NOT EXISTS idx_return_requests_requested_at ON return_requests(requested_at);

CREATE TABLE IF NOT EXISTS return_items (
    id VARCHAR(64) PRIMARY KEY,
    return_request_id VARCHAR(64) NOT NULL REFERENCES return_requests(id) ON DELETE CASCADE,
    order_item_id BIGINT NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    variant_id VARCHAR(64),
    quantity INT NOT NULL,
    restock_status VARCHAR(32) DEFAULT 'PENDING'
);

CREATE INDEX IF NOT EXISTS idx_return_items_request_id ON return_items(return_request_id);
CREATE INDEX IF NOT EXISTS idx_return_items_order_item_id ON return_items(order_item_id);

CREATE TABLE IF NOT EXISTS return_evidences (
    id VARCHAR(64) PRIMARY KEY,
    return_request_id VARCHAR(64) NOT NULL REFERENCES return_requests(id) ON DELETE CASCADE,
    public_id VARCHAR(255),
    secure_url TEXT NOT NULL,
    resource_type VARCHAR(32) DEFAULT 'image',
    uploaded_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_return_evidences_request_id ON return_evidences(return_request_id);

CREATE TABLE IF NOT EXISTS return_shipments (
    id VARCHAR(64) PRIMARY KEY,
    return_request_id VARCHAR(64) NOT NULL UNIQUE REFERENCES return_requests(id) ON DELETE CASCADE,
    tracking_code VARCHAR(128) NOT NULL,
    carrier_name VARCHAR(128),
    shipped_at TIMESTAMP,
    received_at TIMESTAMP
);
