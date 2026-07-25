-- Preserve the only retained customer-profile field before retiring the
-- deprecated Style Profile schema. This migration never changes addresses.
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF to_regclass('public.customer_style_profiles') IS NOT NULL THEN
        INSERT INTO user_profiles (user_id, display_name, created_at, updated_at)
        SELECT user_id, display_name, COALESCE(created_at, CURRENT_TIMESTAMP), COALESCE(updated_at, CURRENT_TIMESTAMP)
        FROM customer_style_profiles
        ON CONFLICT (user_id) DO NOTHING;

        DROP TABLE customer_style_profiles;
    END IF;
END $$;

-- Keep one deterministic default for legacy users before enforcing the rule.
WITH ranked_defaults AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at ASC NULLS LAST, id ASC) AS row_number
    FROM delivery_addresses
    WHERE is_default = TRUE
)
UPDATE delivery_addresses address
SET is_default = FALSE
FROM ranked_defaults ranked
WHERE address.id = ranked.id
  AND ranked.row_number > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_addresses_one_default_per_user
    ON delivery_addresses (user_id)
    WHERE is_default = TRUE;
