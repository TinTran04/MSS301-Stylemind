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

-- Seed Data for Orders
INSERT INTO orders (id, user_id, total_amount, order_status, shipping_address)
VALUES
    ('order_001', 'usr_customer', 687000.00, 'COMPLETED', 'Chung cư Sunrise City, Đường Nguyễn Hữu Thọ, Phường Tân Hưng, Quận 7, Hồ Chí Minh'),
    ('order_002', 'usr_customer', 1299000.00, 'PROCESSING', 'Tòa nhà FPT, Khu công nghệ cao, Quận 9, Hồ Chí Minh'),
    ('order_003', 'usr_admin', 229000.00, 'COMPLETED', '789 Đường Trần Hưng Đạo, Quận 5, TP. Hồ Chí Minh'),
    ('order_004', 'usr_001', 458000.00, 'SHIPPED', 'Số 15 Ngõ 102, Đường Khuất Duy Tiến, Quận Thanh Xuân, Hà Nội'),
    ('order_005', 'usr_002', 359000.00, 'PENDING', '245 Lê Duẩn, Phường Tân Chính, Quận Thanh Khê, Đà Nẵng'),
    ('order_006', 'usr_003', 849000.00, 'CONFIRMED', 'Căn hộ 15.04, Block B, Masteri Thảo Điền, Quận 2, Hồ Chí Minh'),
    ('order_007', 'usr_004', 1149000.00, 'CANCELLED', '45 Hoàng Diệu, Phường Phú Hội, Huế'),
    ('order_008', 'usr_005', 539000.00, 'PAID', 'Số 12 Nguyễn Văn Cừ, Quận Ninh Kiều, Cần Thơ')
ON CONFLICT (id) DO UPDATE SET user_id = EXCLUDED.user_id, total_amount = EXCLUDED.total_amount, order_status = EXCLUDED.order_status, shipping_address = EXCLUDED.shipping_address;

-- Seed Data for Order Items
INSERT INTO order_items (id, order_id, variant_id, quantity, price_at_purchase, is_ai_conversion)
VALUES
    ('order_item_001', 'order_001', 'SM-ATC018-S-CRM', 2, 219000.00, false),
    ('order_item_002', 'order_001', 'SM-APL020-M-NVY', 1, 249000.00, true),
    ('order_item_003', 'order_002', 'SM-AKG007-XL-BLK', 1, 949000.00, false),
    ('order_item_004', 'order_002', 'SM-QJN010-29-DNM', 2, 350000.00, true),
    ('order_item_005', 'order_003', 'SM-ATC001-M-WHT', 1, 229000.00, false),
    ('order_item_006', 'order_004', 'SM-ASM005-M-WHT', 1, 229000.00, true),
    ('order_item_007', 'order_004', 'SM-QKK012-29-CHR', 1, 229000.00, false),
    ('order_item_008', 'order_005', 'SM-ATC019-S-WHT', 1, 359000.00, true),
    ('order_item_009', 'order_006', 'SM-ASM006-M-BEI', 1, 449000.00, false),
    ('order_item_010', 'order_006', 'SM-QJG016-M-BEI', 1, 400000.00, true),
    ('order_item_011', 'order_007', 'SM-AKG007-M-SMK', 1, 1149000.00, false),
    ('order_item_012', 'order_008', 'SM-ATC018-M-PNK', 2, 269500.00, true)
ON CONFLICT (id) DO UPDATE SET order_id = EXCLUDED.order_id, variant_id = EXCLUDED.variant_id, quantity = EXCLUDED.quantity, price_at_purchase = EXCLUDED.price_at_purchase, is_ai_conversion = EXCLUDED.is_ai_conversion;
