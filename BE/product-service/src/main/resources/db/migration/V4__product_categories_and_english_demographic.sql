-- Product - Category becomes many-to-many (product_categories join table);
-- product-level season/style are removed (distinct from user-service style
-- profile, untouched); target_demographic is normalized to the English enum
-- MALE/FEMALE/UNISEX (Vietnamese labels stay frontend-only display).

CREATE TABLE IF NOT EXISTS product_categories (
    product_id VARCHAR(50) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id, category_id)
);

CREATE INDEX IF NOT EXISTS idx_product_categories_product_id ON product_categories(product_id);
CREATE INDEX IF NOT EXISTS idx_product_categories_category_id ON product_categories(category_id);

-- Backfill the existing one-to-many relation before dropping the column it
-- lived on. Fresh Docker starts already boot with the normalized join-table
-- schema, so only run this copy step when the legacy column still exists.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'products'
          AND column_name = 'category_id'
    ) THEN
        INSERT INTO product_categories (product_id, category_id)
        SELECT id, category_id FROM products WHERE category_id IS NOT NULL
        ON CONFLICT (product_id, category_id) DO NOTHING;
    END IF;
END $$;

-- Surface (not silently hide) any target_demographic value that isn't a known
-- alias before it gets defaulted to UNISEX below.
DO $$
DECLARE
    unmapped TEXT;
BEGIN
    SELECT string_agg(DISTINCT COALESCE(target_demographic, '<NULL>'), ', ')
    INTO unmapped
    FROM products
    WHERE UPPER(TRIM(COALESCE(target_demographic, ''))) NOT IN
        ('MALE', 'MEN', 'MAN', 'NAM', 'FEMALE', 'WOMEN', 'WOMAN', 'NU', 'NỮ', 'UNISEX');
    IF unmapped IS NOT NULL THEN
        RAISE NOTICE 'V4 migration: unmapped target_demographic values defaulted to UNISEX: %', unmapped;
    END IF;
END $$;

UPDATE products SET target_demographic = CASE
    WHEN UPPER(TRIM(target_demographic)) IN ('MALE', 'MEN', 'MAN', 'NAM') THEN 'MALE'
    WHEN UPPER(TRIM(target_demographic)) IN ('FEMALE', 'WOMEN', 'WOMAN', 'NU', 'NỮ') THEN 'FEMALE'
    WHEN UPPER(TRIM(target_demographic)) = 'UNISEX' THEN 'UNISEX'
    ELSE 'UNISEX'
END;

ALTER TABLE products
    ALTER COLUMN target_demographic SET DEFAULT 'UNISEX',
    ALTER COLUMN target_demographic SET NOT NULL,
    ADD CONSTRAINT chk_products_target_demographic CHECK (target_demographic IN ('MALE', 'FEMALE', 'UNISEX'));

ALTER TABLE products
    DROP COLUMN IF EXISTS category_id,
    DROP COLUMN IF EXISTS aesthetic_style,
    DROP COLUMN IF EXISTS seasonal_property;
