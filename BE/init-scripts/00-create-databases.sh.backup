#!/bin/sh
# Create multiple databases for StyleMind services AND run init scripts
# This script runs automatically on first postgres initialization

set -e

echo "Creating StyleMind databases..."

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

echo "Databases created successfully."

# Run init scripts for each database
for script in /docker-entrypoint-initdb.d/*.sql; do
    filename=$(basename "$script")

    case "$filename" in
        01-auth-db.sql)
            db_name="auth_db"
            ;;
        02-user-db.sql)
            db_name="user_db"
            ;;
        03-product-db.sql)
            db_name="product_db"
            ;;
        04-product-seed-normalized-from-3-datasets.sql)
            db_name="product_db"
            ;;
        05-cart-db.sql)
            db_name="cart_db"
            ;;
        06-order-db.sql)
            db_name="order_db"
            ;;
        07-payment-db.sql)
            db_name="payment_db"
            ;;
        08-ai-db.sql)
            db_name="ai_db"
            ;;
        09-notification-db.sql)
            db_name="notification_db"
            ;;
        *)
            echo "Skipping unknown SQL file: $filename"
            continue
            ;;
    esac

    echo "Running $script on database $db_name"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d "$db_name" -f "$script"
done

echo "All init scripts completed successfully."