-- Migration V6: Add restock_logs table for idempotent inventory restock

CREATE TABLE IF NOT EXISTS restock_logs (
    id VARCHAR(64) PRIMARY KEY,
    operation_key VARCHAR(128) NOT NULL UNIQUE,
    variant_id VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    reason VARCHAR(64),
    reference_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_restock_logs_variant_id ON restock_logs(variant_id);
CREATE INDEX IF NOT EXISTS idx_restock_logs_operation_key ON restock_logs(operation_key);
