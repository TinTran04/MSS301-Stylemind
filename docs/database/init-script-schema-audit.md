# Init Script Schema Audit

## Scope and Method

### 2026-07-20 structured-address extension

The current implementation adds `administrative_provinces`,
`administrative_wards`, structured delivery-address columns and nullable order
shipping snapshot columns. User Service applies these through Flyway V3; the
clean-install init scripts mirror the fields, and the existing Order Service
uses the non-destructive manual patch
`docs/database/manual-patches/2026-07-20-structured-shipping-snapshot.sql`.
Legacy address/order rows are intentionally not backfilled from free text.

Audit date: 2026-07-11.

The audit compares JPA entity mappings, shared `BaseEntity` audit fields,
repository query patterns, `BE/init-scripts`, and the service configuration
files. It is static verification only: the existing PostgreSQL volume was not
reset, dropped, truncated, or otherwise destructively changed.

Inspected schema sources:

- `BE/init-scripts/00-create-databases.sh`
- `BE/init-scripts/01-auth-db.sql` through `09-notification-db.sql`
- Flyway migrations under auth-service, user-service, and product-service
- Every JPA entity under the eight Spring Boot services
- `BE/common-lib/.../BaseEntity.java`

PostgreSQL is one Docker container with eight service databases: `auth_db`,
`user_db`, `product_db`, `cart_db`, `order_db`, `payment_db`, `ai_db`, and
`notification_db`.

## Results

| Service | Database | Entity/Table | Status | Missing columns | Extra columns | Constraint mismatches | Index mismatches | Recommended action |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| auth-service | auth_db | users, audit_log, pending_registrations | SYNCHRONIZED | None | None | Audit timestamps are now `NOT NULL` to match `BaseEntity` | Repository indexes present | Apply manual patch only to an existing volume with nullable audit fields |
| user-service | user_db | customer_style_profiles, delivery_addresses | SYNCHRONIZED | None | None | Audit timestamps and `is_default` now match entity nullability | User/default-address indexes present | Apply manual patch on existing volumes |
| product-service | product_db | categories, products, product_audit_log, product_variants, product_images | SYNCHRONIZED | None | `product_categories.created_at` is an intentional SQL creation trace | Audit timestamps and `is_primary` now match entity nullability; demographic check stores only `MALE`, `FEMALE`, `UNISEX` | Product/category/variant/image indexes present | No `payment_code`; retain current normalized category schema |
| product-service | product_db | product_categories | NEEDS_MANUAL_VERIFICATION | None | `created_at` has no JPA field | No issue for Hibernate validation because extra columns are allowed | Product and category indexes present | Keep the extra timestamp unless a product-history requirement says otherwise |
| cart-service | cart_db | shopping_carts, cart_items | SYNCHRONIZED | None | `cart_items.added_at` is not mapped by JPA | Audit timestamps and `is_ai_recommended` now match entity nullability | Existing cart and variant indexes cover current repository queries | Keep `added_at` as non-required compatibility metadata |
| order-service | order_db | orders, order_items, order_status_audit_log, checkout_idempotency | SYNCHRONIZED for fresh init | None | None | Fresh schema now uses `VARCHAR(30)` and current `OrderStatus` checks; audit timestamps and `is_ai_conversion` are non-null | Added `orders(order_status, created_at)` for status/timeout queries; idempotency unique key covers lookup | Existing volumes need manual review before narrowing status or adding enum checks |
| payment-service | payment_db | transactions, payment_webhook_events | SYNCHRONIZED | None | None | Audit timestamps now match `BaseEntity`; `transactions.transfer_content`, expiry, paid time, gateway id, event result, processed, and error message match entities | Order/user/status/transfer-content indexes and partial unique `(provider, gateway_transaction_id)` exist | Do not add `payment_code`; `transfer_content` remains authoritative |
| ai-agent-service | ai_db | chat_sessions, chat_messages, ai_curated_bundles, ai_curated_bundle_items, ai_index_jobs | SYNCHRONIZED | None | `ai_analytics_logs` has no current JPA entity | Audit timestamps and `has_product_block` now match entity nullability | Session/message/bundle/job indexes present | Keep `ai_analytics_logs` under manual ownership verification |
| notification-service | notification_db | notification_logs | SYNCHRONIZED | None | None | `BIGSERIAL` matches `Long`; audit timestamps are now non-null | User/status/created-at indexes present | Apply manual patch on existing volumes |

## Flyway and Init Strategy

All services use `spring.jpa.hibernate.ddl-auto=validate`.

- auth-service, user-service, and product-service have Flyway enabled with
  `baseline-on-migrate=true`.
- order-service and payment-service remain init-script-only by design; Flyway
  was not enabled or introduced by this work.

The older Flyway V1 baselines are historical migration records and were not
edited. Docker first creates the current init-script schema, then Flyway
baselines/migrates those services. The later auth/user/product migrations use
idempotent SQL for this path. Fresh-volume startup still needs runtime testing
before it can be called fully verified; this audit did not remove any current
volume to perform that destructive test.

## Existing Volume Patch

Fresh Docker installations receive the corrected constraints from the init
scripts. Existing local volumes do not rerun init scripts. Apply
`BE/init-scripts/manual-patches/2026-07-11-init-script-nullability-sync.sql`
only after reviewing local data. It is safe to rerun and only:

- fills null audit timestamps with the current timestamp;
- fills null entity-required booleans with `false`;
- adds defaults, non-null constraints, and the order status/created-at index.

It does not drop databases/tables, truncate rows, delete business data, remove
columns, narrow old status columns, or add enum checks to populated `orders`.

Earlier SePay-specific existing-volume patches remain under
`docs/database/manual-patches/` and must be applied first if an old volume is
missing `checkout_idempotency` or payment webhook columns.

## Read-Only Existing Volume Check

A read-only inspection of the running local PostgreSQL container on 2026-07-11
confirmed the expected split between fresh-schema definitions and an older
volume:

- `order_db.checkout_idempotency` already has all entity columns with the
  expected nullability and lengths.
- `order_db.orders.order_status` is still `VARCHAR(50)`, while fresh init now
  creates `VARCHAR(30)` with the current order-state check.
- `order_db.orders` audit timestamps, `order_items` audit timestamps, and
  `order_items.is_ai_conversion` are nullable in the existing volume.
- `payment_db.payment_webhook_events` already contains the current webhook
  fields, including `provider`, `processed`, and `error_message`.
- `payment_db.transactions.created_at` and `updated_at` are nullable in the
  existing volume.

This confirms the manual patch is applicable to the local volume for
nullability/default synchronization. It deliberately leaves the existing
`orders.order_status` length and enum constraint unchanged pending data review.

## Remaining Risks

- Static comparison cannot prove a fresh Docker volume completes all Flyway
  baselining paths; run the full stack against a disposable environment when
  such a test environment is available.
- `cart_items.added_at`, `product_categories.created_at`, and
  `ai_analytics_logs` are not JPA-owned fields/tables. They do not block
  Hibernate validation but should remain deliberate documented data ownership.
- Existing `orders.order_status` data must be reviewed before applying the
  narrower `VARCHAR(30)` or the fresh-schema enum checks to a populated volume.
