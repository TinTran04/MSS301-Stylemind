#!/usr/bin/env bash
set -euo pipefail

AUTH_DATABASE_URL="${AUTH_DATABASE_URL:-postgresql://postgres:password@localhost:5432/auth_db}"
USER_DATABASE_URL="${USER_DATABASE_URL:-postgresql://postgres:password@localhost:5432/user_db}"
transfer_file="$(mktemp)"
trap 'rm -f "$transfer_file"' EXIT

psql "$USER_DATABASE_URL" -v ON_ERROR_STOP=1 -c \
  "ALTER TABLE customer_style_profiles ADD COLUMN IF NOT EXISTS display_name VARCHAR(150)"

psql "$AUTH_DATABASE_URL" -v ON_ERROR_STOP=1 \
  -c "\\copy (SELECT id, full_name FROM users WHERE full_name IS NOT NULL) TO '$transfer_file' WITH (FORMAT csv)"

psql "$USER_DATABASE_URL" -v ON_ERROR_STOP=1 <<SQL
CREATE TEMP TABLE profile_name_transfer (
    user_id VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(150)
);
\copy profile_name_transfer FROM '$transfer_file' WITH (FORMAT csv)
INSERT INTO customer_style_profiles (user_id, display_name, created_at, updated_at)
SELECT user_id, display_name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM profile_name_transfer
ON CONFLICT (user_id) DO UPDATE
SET display_name = COALESCE(customer_style_profiles.display_name, EXCLUDED.display_name),
    updated_at = CURRENT_TIMESTAMP;
SQL

psql "$AUTH_DATABASE_URL" -v ON_ERROR_STOP=1 -c \
  "UPDATE users SET full_name = NULL WHERE full_name IS NOT NULL"

echo "Transferred auth full names into user profile shells."
