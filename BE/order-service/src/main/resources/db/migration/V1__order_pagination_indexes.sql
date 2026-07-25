-- Adds the stable customer order-list index.
-- Forward-only and safe to rerun on existing PostgreSQL databases.

CREATE INDEX IF NOT EXISTS idx_orders_user_created_at_id
    ON orders(user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_orders_user_status_created_at_id
    ON orders(user_id, order_status, created_at DESC, id DESC);
