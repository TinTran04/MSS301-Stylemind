-- Init script for product_db
-- Categories, Products, Product Variants, Product Images

-- Categories (hierarchical tree structure)
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT REFERENCES categories(id),
    slug VARCHAR(150) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Products (base product information)
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(50) PRIMARY KEY,
    category_id BIGINT REFERENCES categories(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    base_price DECIMAL(12, 2) NOT NULL,
    aesthetic_style VARCHAR(50),
    target_demographic VARCHAR(20),
    seasonal_property VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product Variants (SKU-level variants with size, color, material)
CREATE TABLE IF NOT EXISTS product_variants (
    id VARCHAR(50) PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(100) UNIQUE NOT NULL,
    size VARCHAR(20) NOT NULL,
    color VARCHAR(50) NOT NULL,
    material VARCHAR(50),
    price_override DECIMAL(12, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product Images
CREATE TABLE IF NOT EXISTS product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
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
