-- V1 permitted NULL defaults. Normalize existing rows before Java sorting and
-- enforce the entity's non-null default flag for future writes.
UPDATE delivery_addresses
SET is_default = FALSE
WHERE is_default IS NULL;

ALTER TABLE delivery_addresses
    ALTER COLUMN is_default SET DEFAULT FALSE;

ALTER TABLE delivery_addresses
    ALTER COLUMN is_default SET NOT NULL;
