# Manual SePay schema patch for existing Docker volumes

This patch is for the case where the running PostgreSQL volume was created before the latest SePay hardening schema landed.

It is non-destructive:
- it does not drop databases
- it does not drop tables
- it does not truncate data
- it only creates missing tables, columns, indexes, and constraints when safe

## Why this is needed

`order-service` expects `checkout_idempotency`.

`payment-service` expects `payment_webhook_events.error_message` and the related SePay columns.

If the container was started with an older volume, Hibernate validation fails even though the repo now has the updated schema files.

## Backup first

```bash
docker exec -t stylemind-postgres pg_dumpall -U postgres > backup-before-sepay-schema-patch.sql
```

## Apply the order DB patch

```bash
docker exec -i stylemind-postgres psql -U postgres -d order_db \
  < docs/database/manual-patches/2026-07-09-order-db-sepay-schema.sql
```

## Apply the payment DB patch

```bash
docker exec -i stylemind-postgres psql -U postgres -d payment_db \
  < docs/database/manual-patches/2026-07-09-payment-db-sepay-schema.sql
```

## Restart the services

If you are using the full compose file from this repo:

```bash
docker compose -f BE/docker-compose.full.yml restart order-service payment-service
```

## Verify the schema

```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'checkout_idempotency'
ORDER BY ordinal_position;

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'payment_webhook_events'
ORDER BY ordinal_position;
```

You can run those checks with:

```bash
docker exec -i stylemind-postgres psql -U postgres -d order_db
docker exec -i stylemind-postgres psql -U postgres -d payment_db
```
