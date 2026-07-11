-- Manual non-destructive patch for payment_db
-- Apply to the running Postgres container:
--   docker exec -i stylemind-postgres psql -U postgres -d payment_db \
--     < docs/database/manual-patches/2026-07-09-payment-db-sepay-schema.sql

-- Transactions: add the fields the current entity validates against.
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

-- Webhook audit table: add any missing entity columns and backfill defaults safely.
ALTER TABLE payment_webhook_events
    ADD COLUMN IF NOT EXISTS provider VARCHAR(30),
    ADD COLUMN IF NOT EXISTS gateway_transaction_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS transfer_content VARCHAR(200),
    ADD COLUMN IF NOT EXISTS amount DECIMAL(12, 2),
    ADD COLUMN IF NOT EXISTS result VARCHAR(30),
    ADD COLUMN IF NOT EXISTS processed BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS error_message TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE payment_webhook_events
SET provider = COALESCE(provider, 'SEPAY'),
    result = COALESCE(result, 'RECEIVED'),
    processed = COALESCE(processed, FALSE),
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE provider IS NULL
   OR result IS NULL
   OR processed IS NULL
   OR created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE payment_webhook_events
    ALTER COLUMN provider SET DEFAULT 'SEPAY',
    ALTER COLUMN result SET DEFAULT 'RECEIVED',
    ALTER COLUMN processed SET DEFAULT FALSE,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE payment_webhook_events
    ALTER COLUMN provider SET NOT NULL,
    ALTER COLUMN result SET NOT NULL,
    ALTER COLUMN processed SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_webhook_events_gateway_txn_id ON payment_webhook_events(gateway_transaction_id);
CREATE INDEX IF NOT EXISTS idx_webhook_events_transaction_id ON payment_webhook_events(transaction_id);
CREATE INDEX IF NOT EXISTS idx_webhook_events_created_at ON payment_webhook_events(created_at);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = current_schema()
          AND tablename = 'payment_webhook_events'
          AND indexname = 'uq_payment_webhook_events_provider_gateway_txn'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM payment_webhook_events
            WHERE gateway_transaction_id IS NOT NULL
            GROUP BY provider, gateway_transaction_id
            HAVING COUNT(*) > 1
        ) THEN
            CREATE UNIQUE INDEX uq_payment_webhook_events_provider_gateway_txn
                ON payment_webhook_events(provider, gateway_transaction_id)
                WHERE gateway_transaction_id IS NOT NULL;
        ELSE
            RAISE NOTICE 'Skipped uq_payment_webhook_events_provider_gateway_txn because duplicate provider/gateway_transaction_id rows already exist.';
        END IF;
    END IF;
END $$;
