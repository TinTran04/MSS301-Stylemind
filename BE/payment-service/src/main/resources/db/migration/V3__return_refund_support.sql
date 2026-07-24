-- Migration V3: Support Return Request Refunds and Payout Destination in Payment Service

ALTER TABLE refund_transactions
    ALTER COLUMN order_cancellation_id DROP NOT NULL;

ALTER TABLE refund_transactions
    ADD COLUMN IF NOT EXISTS return_request_id VARCHAR(64);

ALTER TABLE refund_transactions
    ADD COLUMN IF NOT EXISTS bank_code VARCHAR(32);

ALTER TABLE refund_transactions
    ADD COLUMN IF NOT EXISTS account_holder VARCHAR(150);

ALTER TABLE refund_transactions
    ADD COLUMN IF NOT EXISTS account_number VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_refund_transactions_return_request
    ON refund_transactions(return_request_id);
