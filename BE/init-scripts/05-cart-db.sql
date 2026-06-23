-- Init script for cart_db
-- Shopping Carts and Cart Items

-- Shopping Carts (one per user or guest session)
CREATE TABLE IF NOT EXISTS shopping_carts (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Cart Items
CREATE TABLE IF NOT EXISTS cart_items (
    id VARCHAR(50) PRIMARY KEY,
    cart_id VARCHAR(50) NOT NULL REFERENCES shopping_carts(id) ON DELETE CASCADE,
    variant_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    is_ai_recommended BOOLEAN DEFAULT FALSE,
    source_bundle_id VARCHAR(50),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_variant_id ON cart_items(variant_id);
CREATE INDEX IF NOT EXISTS idx_shopping_carts_user_id ON shopping_carts(user_id);