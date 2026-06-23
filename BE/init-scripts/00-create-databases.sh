#!/bin/bash
# Create multiple databases for StyleMind services AND run init scripts
# This script runs automatically on first postgres initialization

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE auth_db;
    CREATE DATABASE user_db;
    CREATE DATABASE product_db;
    CREATE DATABASE cart_db;
    CREATE DATABASE order_db;
    CREATE DATABASE payment_db;
    CREATE DATABASE ai_db;
    CREATE DATABASE notification_db;
EOSQL

# Run init scripts for each database
for script in /docker-entrypoint-initdb.d/*.sql; do
    if [[ "$script" != "/docker-entrypoint-initdb.d/00-create-databases.sh" ]]; then
        # Extract database name from script filename (01-auth-db.sql -> auth_db)
        db_name=$(basename "$script" | sed -E 's/^[0-9]+-([a-z-]+)-db\.sql$/\1_db/')
        echo "Running $script on database $db_name"
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d "$db_name" -f "$script"
    fi
done
