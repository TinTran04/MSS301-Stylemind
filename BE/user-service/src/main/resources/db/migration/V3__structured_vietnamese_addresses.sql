-- Forward-only migration for structured Vietnamese delivery addresses.
-- Existing delivery addresses are intentionally not inferred from free text.
CREATE TABLE IF NOT EXISTS administrative_provinces (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    data_version VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS administrative_wards (
    code VARCHAR(10) PRIMARY KEY,
    province_code VARCHAR(10) NOT NULL REFERENCES administrative_provinces(code),
    name VARCHAR(150) NOT NULL,
    type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    data_version VARCHAR(50) NOT NULL
);

ALTER TABLE delivery_addresses ADD COLUMN IF NOT EXISTS province_code VARCHAR(10);
ALTER TABLE delivery_addresses ADD COLUMN IF NOT EXISTS province_name VARCHAR(150);
ALTER TABLE delivery_addresses ADD COLUMN IF NOT EXISTS ward_code VARCHAR(10);
ALTER TABLE delivery_addresses ADD COLUMN IF NOT EXISTS ward_name VARCHAR(150);
ALTER TABLE delivery_addresses ADD COLUMN IF NOT EXISTS shipping_note TEXT;
ALTER TABLE delivery_addresses ADD COLUMN IF NOT EXISTS validation_status VARCHAR(30);
ALTER TABLE delivery_addresses ADD COLUMN IF NOT EXISTS administrative_data_version VARCHAR(50);

UPDATE delivery_addresses
SET validation_status = 'LEGACY_UNVERIFIED'
WHERE validation_status IS NULL;

ALTER TABLE delivery_addresses
    ALTER COLUMN validation_status SET DEFAULT 'LEGACY_UNVERIFIED';
ALTER TABLE delivery_addresses
    ALTER COLUMN validation_status SET NOT NULL;

ALTER TABLE delivery_addresses
    DROP CONSTRAINT IF EXISTS ck_delivery_addresses_validation_status;
ALTER TABLE delivery_addresses
    ADD CONSTRAINT ck_delivery_addresses_validation_status
    CHECK (validation_status IN ('VALID', 'LEGACY_UNVERIFIED'));

CREATE INDEX IF NOT EXISTS idx_admin_provinces_active_name
    ON administrative_provinces(active, name);
CREATE INDEX IF NOT EXISTS idx_admin_wards_province_active_name
    ON administrative_wards(province_code, active, name);
CREATE INDEX IF NOT EXISTS idx_delivery_addresses_validation_status
    ON delivery_addresses(user_id, validation_status);
