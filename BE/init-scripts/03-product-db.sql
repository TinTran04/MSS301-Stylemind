-- Init script for product_db
-- Categories, Products, Product-Category links, Product Variants, Product Images

-- Categories (hierarchical tree structure)
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT REFERENCES categories(id),
    slug VARCHAR(150) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Products (base product information). target_demographic is the English
-- enum MALE/FEMALE/UNISEX; Vietnamese labels are frontend display only.
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    base_price DECIMAL(12, 2) NOT NULL,
    target_demographic VARCHAR(20) NOT NULL DEFAULT 'UNISEX'
        CHECK (target_demographic IN ('MALE', 'FEMALE', 'UNISEX')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Product - Category is many-to-many: one product can belong to multiple
-- categories, one category can contain multiple products.
CREATE TABLE IF NOT EXISTS product_categories (
    product_id VARCHAR(50) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id, category_id)
);

-- Destructive admin action audit trail (SEC-10): product delete, variant
-- delete, image delete all record who did what and when.
CREATE TABLE IF NOT EXISTS product_audit_log (
    id VARCHAR(50) PRIMARY KEY,
    actor_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    detail VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_product_audit_log_product_id ON product_audit_log(product_id);

-- Product Variants (SKU-level variants with size, color, material, stock)
CREATE TABLE IF NOT EXISTS product_variants (
    id VARCHAR(50) PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(100) UNIQUE NOT NULL,
    size VARCHAR(20) NOT NULL,
    color VARCHAR(50) NOT NULL,
    material VARCHAR(50),
    price_override DECIMAL(12, 2),
    stock_quantity INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Product Images
CREATE TABLE IF NOT EXISTS product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    image_public_id VARCHAR(255),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_product_categories_product_id ON product_categories(product_id);
CREATE INDEX IF NOT EXISTS idx_product_categories_category_id ON product_categories(category_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_product_id ON product_variants(product_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_sku ON product_variants(sku);
CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id);

-- Insert sample categories
INSERT INTO categories (name, parent_id, slug) VALUES
('Áo', NULL, 'ao'),
('Quần', NULL, 'quan'),
('Đầm', NULL, 'dam'),
('Áo khoác', 1, 'ao-khoac'),
('Áo sơ mi', 1, 'ao-so-mi'),
('Áo thun', 1, 'ao-thun'),
('Quần tây', 2, 'quan-tay'),
('Quần jean', 2, 'quan-jean'),
('Quần short', 2, 'quan-short')
ON CONFLICT (slug) DO NOTHING;
