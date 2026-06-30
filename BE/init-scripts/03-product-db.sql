-- Init script for product_db
-- Categories, Products, Product Variants, Product Images

-- Categories (hierarchical tree structure)
CREATE TABLE IF NOT EXISTS categories (
                                          id SERIAL PRIMARY KEY,
                                          name VARCHAR(100) NOT NULL,
    parent_id INT REFERENCES categories(id),
    slug VARCHAR(150) UNIQUE NOT NULL
    );

-- Products (base product information)
CREATE TABLE IF NOT EXISTS products (
                                        id VARCHAR(50) PRIMARY KEY,
    category_id INT REFERENCES categories(id),
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
                                              id SERIAL PRIMARY KEY,
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
INSERT INTO categories (id, name, parent_id, slug) VALUES
                                                       (1, 'Áo', NULL, 'ao'),
                                                       (2, 'Quần', NULL, 'quan'),
                                                       (3, 'Đầm', NULL, 'dam'),
                                                       (4, 'Áo khoác', 1, 'ao-khoac'),
                                                       (5, 'Áo sơ mi', 1, 'ao-so-mi'),
                                                       (6, 'Áo thun', 1, 'ao-thun'),
                                                       (7, 'Quần tây', 2, 'quan-tay'),
                                                       (8, 'Quần jean', 2, 'quan-jean'),
                                                       (9, 'Quần short', 2, 'quan-short')
    ON CONFLICT (slug) DO NOTHING;