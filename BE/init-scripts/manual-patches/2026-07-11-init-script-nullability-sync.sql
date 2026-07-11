-- Non-destructive schema synchronization for existing local StyleMind volumes.
--
-- Run once from the repository root after the existing 2026-07-09 SePay patches
-- when applicable:
--   docker exec -i stylemind-postgres psql -U postgres -d postgres \
--     < BE/init-scripts/manual-patches/2026-07-11-init-script-nullability-sync.sql
--
-- Safe to rerun. This patch never drops objects or deletes rows. It only
-- backfills null audit/boolean values, then applies the defaults and NOT NULL
-- constraints required by the current JPA entities. It intentionally does not
-- narrow orders.order_status or add enum CHECK constraints to existing data.

\set ON_ERROR_STOP on

\connect auth_db
UPDATE users SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE audit_log SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE pending_registrations SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
ALTER TABLE users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE audit_log ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE pending_registrations ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;

\connect user_db
UPDATE customer_style_profiles SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE delivery_addresses SET is_default = COALESCE(is_default, FALSE), created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE is_default IS NULL OR created_at IS NULL OR updated_at IS NULL;
ALTER TABLE customer_style_profiles ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE delivery_addresses ALTER COLUMN is_default SET DEFAULT FALSE, ALTER COLUMN is_default SET NOT NULL, ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;

\connect product_db
UPDATE categories SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE products SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE product_audit_log SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE product_variants SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE product_images SET is_primary = COALESCE(is_primary, FALSE), created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE is_primary IS NULL OR created_at IS NULL OR updated_at IS NULL;
ALTER TABLE categories ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE products ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE product_audit_log ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE product_variants ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE product_images ALTER COLUMN is_primary SET DEFAULT FALSE, ALTER COLUMN is_primary SET NOT NULL, ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;

\connect cart_db
UPDATE shopping_carts SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE cart_items SET is_ai_recommended = COALESCE(is_ai_recommended, FALSE), created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE is_ai_recommended IS NULL OR created_at IS NULL OR updated_at IS NULL;
ALTER TABLE shopping_carts ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE cart_items ALTER COLUMN is_ai_recommended SET DEFAULT FALSE, ALTER COLUMN is_ai_recommended SET NOT NULL, ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;

\connect order_db
UPDATE orders SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE order_items SET is_ai_conversion = COALESCE(is_ai_conversion, FALSE), created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE is_ai_conversion IS NULL OR created_at IS NULL OR updated_at IS NULL;
UPDATE order_status_audit_log SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE checkout_idempotency SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
ALTER TABLE orders ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE order_items ALTER COLUMN is_ai_conversion SET DEFAULT FALSE, ALTER COLUMN is_ai_conversion SET NOT NULL, ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE order_status_audit_log ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE checkout_idempotency ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_orders_status_created_at ON orders(order_status, created_at);

\connect payment_db
UPDATE transactions SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE payment_webhook_events SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
ALTER TABLE transactions ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE payment_webhook_events ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;

\connect ai_db
UPDATE chat_sessions SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE chat_messages SET has_product_block = COALESCE(has_product_block, FALSE), created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE has_product_block IS NULL OR created_at IS NULL OR updated_at IS NULL;
UPDATE ai_curated_bundles SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE ai_analytics_logs SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
UPDATE ai_index_jobs SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
ALTER TABLE chat_sessions ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE chat_messages ALTER COLUMN has_product_block SET DEFAULT FALSE, ALTER COLUMN has_product_block SET NOT NULL, ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE ai_curated_bundles ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE ai_analytics_logs ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE ai_index_jobs ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;

\connect notification_db
UPDATE notification_logs SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP), updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP) WHERE created_at IS NULL OR updated_at IS NULL;
ALTER TABLE notification_logs ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP, ALTER COLUMN created_at SET NOT NULL, ALTER COLUMN updated_at SET NOT NULL;
