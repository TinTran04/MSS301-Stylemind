-- Init script for cart_db
-- Shopping Carts and Cart Items

-- Shopping Carts (one per user or guest session)
CREATE TABLE IF NOT EXISTS shopping_carts (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Cart Items
CREATE TABLE IF NOT EXISTS cart_items (
    id VARCHAR(50) PRIMARY KEY,
    cart_id VARCHAR(50) NOT NULL REFERENCES shopping_carts(id) ON DELETE CASCADE,
    variant_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    is_ai_recommended BOOLEAN NOT NULL DEFAULT FALSE,
    source_bundle_id VARCHAR(50),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_variant_id ON cart_items(variant_id);
CREATE INDEX IF NOT EXISTS idx_shopping_carts_user_id ON shopping_carts(user_id);

-- Seed Data for Shopping Carts
INSERT INTO shopping_carts (id, user_id)
VALUES
    ('cart_customer', 'usr_customer'),
    ('cart_admin', 'usr_admin'),
    ('cart_001', 'usr_001'),
    ('cart_002', 'usr_002'),
    ('cart_003', 'usr_003'),
    ('cart_004', 'usr_004'),
    ('cart_005', 'usr_005')
ON CONFLICT (user_id) DO UPDATE SET id = EXCLUDED.id;

-- Seed Data for Cart Items
INSERT INTO cart_items (id, cart_id, variant_id, quantity, is_ai_recommended, added_at)
VALUES
    ('cart_item_001', 'cart_customer', 'SM-ATC018-S-CRM', 2, false, NOW() - INTERVAL '2 days'),
    ('cart_item_002', 'cart_customer', 'SM-QJN010-29-DNM', 1, true, NOW() - INTERVAL '1 day'),
    ('cart_item_003', 'cart_customer', 'SM-APL020-M-NVY', 1, false, NOW() - INTERVAL '3 hours'),
    ('cart_item_004', 'cart_001', 'SM-ASM005-M-WHT', 1, true, NOW() - INTERVAL '1 day'),
    ('cart_item_005', 'cart_001', 'SM-QKK012-29-CHR', 2, false, NOW() - INTERVAL '12 hours'),
    ('cart_item_006', 'cart_002', 'SM-ATC019-S-WHT', 1, true, NOW() - INTERVAL '2 days'),
    ('cart_item_007', 'cart_002', 'SM-APL020-S-WHT', 1, false, NOW() - INTERVAL '1 day'),
    ('cart_item_008', 'cart_003', 'SM-ASM006-M-BEI', 1, true, NOW() - INTERVAL '3 days'),
    ('cart_item_009', 'cart_003', 'SM-QJG016-M-BEI', 1, false, NOW() - INTERVAL '2 days'),
    ('cart_item_010', 'cart_004', 'SM-AKG007-M-SMK', 1, true, NOW() - INTERVAL '1 day'),
    ('cart_item_011', 'cart_005', 'SM-ATC018-M-PNK', 2, false, NOW() - INTERVAL '5 hours')
ON CONFLICT (id) DO UPDATE SET cart_id = EXCLUDED.cart_id, variant_id = EXCLUDED.variant_id, quantity = EXCLUDED.quantity, is_ai_recommended = EXCLUDED.is_ai_recommended, added_at = EXCLUDED.added_at;
