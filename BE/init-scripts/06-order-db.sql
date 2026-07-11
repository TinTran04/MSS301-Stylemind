-- Init script for order_db
-- Orders and Order Items

-- Orders
CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CONSTRAINT ck_orders_order_status CHECK (order_status IN (
            'PENDING', 'PAYMENT_PENDING', 'PAID', 'CONFIRMED', 'PROCESSING',
            'SHIPPED', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'FAILED'
        )),
    shipping_address TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Order Items
CREATE TABLE IF NOT EXISTS order_items (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    price_at_purchase DECIMAL(12,2) NOT NULL,
    is_ai_conversion BOOLEAN NOT NULL DEFAULT FALSE,
    source_bundle_id VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Order status audit trail (SEC-10): every transition through
-- OrderStatusService.changeStatus() is persisted here, not just logged.
CREATE TABLE IF NOT EXISTS order_status_audit_log (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    actor_id VARCHAR(50) NOT NULL,
    from_status VARCHAR(30)
        CONSTRAINT ck_order_status_audit_from_status CHECK (from_status IS NULL OR from_status IN (
            'PENDING', 'PAYMENT_PENDING', 'PAID', 'CONFIRMED', 'PROCESSING',
            'SHIPPED', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'FAILED'
        )),
    to_status VARCHAR(30) NOT NULL
        CONSTRAINT ck_order_status_audit_to_status CHECK (to_status IN (
            'PENDING', 'PAYMENT_PENDING', 'PAID', 'CONFIRMED', 'PROCESSING',
            'SHIPPED', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'FAILED'
        )),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS checkout_idempotency (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    order_id VARCHAR(50) REFERENCES orders(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, idempotency_key)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status_created_at ON orders(order_status, created_at);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_variant_id ON order_items(variant_id);
CREATE INDEX IF NOT EXISTS idx_order_status_audit_log_order_id ON order_status_audit_log(order_id);
CREATE INDEX IF NOT EXISTS idx_checkout_idempotency_order_id ON checkout_idempotency(order_id);
