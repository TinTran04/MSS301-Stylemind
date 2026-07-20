-- Non-destructive Order Service schema patch for existing order_db volumes.
-- Safe to rerun. Do not backfill structured fields from legacy free text.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS source_address_id VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_recipient_name VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_phone VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_province_code VARCHAR(10);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_province_name VARCHAR(150);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_ward_code VARCHAR(10);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_ward_name VARCHAR(150);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_address_line TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_note TEXT;

CREATE INDEX IF NOT EXISTS idx_orders_source_address_id ON orders(source_address_id);
