-- Manual non-destructive patch for order_db
-- Apply to the running Postgres container:
--   docker exec -i stylemind-postgres psql -U postgres -d order_db \
--     < docs/database/manual-patches/2026-07-09-order-db-sepay-schema.sql

-- Create the idempotency table if it is missing.
CREATE TABLE IF NOT EXISTS checkout_idempotency (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    order_id VARCHAR(50) REFERENCES orders(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PROCESSING',
    error_message TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, idempotency_key)
);

-- Add any missing columns to an older checkout_idempotency table without touching data.
ALTER TABLE checkout_idempotency
    ADD COLUMN IF NOT EXISTS user_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100),
    ADD COLUMN IF NOT EXISTS order_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS error_message TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Re-attach the order FK only if the column already exists without it.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'checkout_idempotency'
          AND column_name = 'order_id'
          AND table_schema = current_schema()
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
         AND tc.table_schema = kcu.table_schema
        WHERE tc.table_name = 'checkout_idempotency'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'order_id'
          AND tc.table_schema = current_schema()
    ) THEN
        ALTER TABLE checkout_idempotency
            ADD CONSTRAINT fk_checkout_idempotency_order_id
            FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Backfill defaults before enforcing not-null expectations from the entity.
UPDATE checkout_idempotency
SET status = COALESCE(status, 'PROCESSING'),
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE status IS NULL
   OR created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE checkout_idempotency
    ALTER COLUMN status SET DEFAULT 'PROCESSING',
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM checkout_idempotency
        WHERE user_id IS NULL OR idempotency_key IS NULL
    ) THEN
        ALTER TABLE checkout_idempotency
            ALTER COLUMN user_id SET NOT NULL,
            ALTER COLUMN idempotency_key SET NOT NULL;
    ELSE
        RAISE NOTICE 'Skipping NOT NULL enforcement on user_id/idempotency_key because legacy null rows already exist.';
    END IF;
END $$;

ALTER TABLE checkout_idempotency
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

-- Enforce one checkout row per user + idempotency key without deleting any rows.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_checkout_idempotency_user_key'
          AND conrelid = 'checkout_idempotency'::regclass
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM checkout_idempotency
            WHERE user_id IS NOT NULL
              AND idempotency_key IS NOT NULL
            GROUP BY user_id, idempotency_key
            HAVING COUNT(*) > 1
        ) THEN
            ALTER TABLE checkout_idempotency
                ADD CONSTRAINT uq_checkout_idempotency_user_key UNIQUE (user_id, idempotency_key);
        ELSE
            RAISE NOTICE 'Skipped uq_checkout_idempotency_user_key because duplicate user_id/idempotency_key rows already exist.';
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_checkout_idempotency_order_id ON checkout_idempotency(order_id);
