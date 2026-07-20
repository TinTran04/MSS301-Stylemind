# Structured Vietnamese Address and Checkout Plan

## Scope and constraints

- Preserve the existing checkout orchestration, cart ownership, product-price lookup, payment initialization, idempotency, and order-status rules.
- Keep `DeliveryAddress.phoneNumber` as an address-level recipient phone; do not add a phone field to Auth users.
- Keep legacy `city` and `orders.shipping_address` readable for compatibility, but do not treat legacy addresses as checkout-valid.
- Use the pinned local administrative dataset `thanglequoc/vietnamese-provinces-database` `v4.0.0` (commit `86361845ba60ee779905ef07f04d7db33c798d04`, MIT), with attribution in repository documentation.
- Do not call public address APIs during checkout, expose `/internal/v1/**` to the browser, log complete phone/address values, or modify existing order/user data manually.

## Verified contracts

- Public User Service routes are behind the Gateway at `/api/v1/users/**`; address CRUD remains under `/addresses`.
- Public administrative lookup routes will be `/api/v1/users/administrative/provinces` and `/api/v1/users/administrative/provinces/{provinceCode}/wards`.
- Order Service internal address lookup remains `/internal/v1/users/{userId}/addresses/{addressId}`, but it returns the structured address DTO and rejects non-`VALID` addresses.
- Authenticated order creation changes from `{shippingAddress, paymentMethod}` to `{addressId, paymentMethod}`. `OrderResponse.shippingAddress` remains present.
- Order Service uses `USER_SERVICE_URL` and the existing `X-Internal-Token` Feign interceptor for internal address ownership checks.

## Execution checklist

### 1. Administrative dataset and persistence

- [ ] Add the pinned Vietnamese province/ward data under `BE/user-service/src/main/resources/data/` with an attribution/license file.
- [ ] Add `AdministrativeProvince` and `AdministrativeWard` entities/repositories and an idempotent startup importer for the bundled data.
- [ ] Add tables/indexes/foreign-key constraints to the User Service Flyway migration and clean-install init script.
- [ ] Add public lookup service/controller methods that return DTOs, not entities.
- [ ] RED: test province lookup, ward-by-province lookup, code lookup, active filtering, and parent mismatch.
- [ ] GREEN: implement repositories/service/controller and importer.
- [ ] REFACTOR: keep dataset parsing and persistence isolated from address CRUD.

### 2. User Service phone validation

- [ ] Add the pinned `com.googlecode.libphonenumber:libphonenumber` dependency to `BE/user-service/pom.xml`.
- [ ] Implement a region-`VN` parser/normalizer that accepts local `0` and `+84` input, rejects blank/malformed/non-Vietnamese numbers, and returns E.164.
- [ ] Add safe validation error mapping without logging the value.
- [ ] RED: cover local input, `+84`, normalization, blank, malformed, and non-Vietnamese cases.
- [ ] GREEN: wire the validator into address create/update service logic.
- [ ] REFACTOR: centralize phone parsing and keep frontend checks advisory.

### 3. User database migration and address model

- [ ] Add the next forward-only Flyway migration under `BE/user-service/src/main/resources/db/migration/`.
- [ ] Add structured address columns, `validation_status`, and `administrative_data_version`; preserve `city` as legacy.
- [ ] Backfill only `LEGACY_UNVERIFIED` for existing rows; never infer codes/names from free text.
- [ ] Add lookup indexes and retain the existing default-address behavior.
- [ ] Update `BE/init-scripts/02-user-db.sql` with the final schema and legacy-safe seed values.
- [ ] Update `DeliveryAddress`, request/response DTOs, mapping, and repository methods.
- [ ] RED/GREEN: persistence, legacy status, ownership, default uniqueness, and schema mapping tests.

### 4. User Service public/internal APIs

- [ ] Change the public address write contract to receive recipient, phone, province code, ward code, address line, note, and default flag.
- [ ] Resolve canonical province/ward names server-side and persist `VALID` plus the dataset version.
- [ ] Return legacy status and structured fields from address list/create/update.
- [ ] Extend the internal address response with the approved snapshot fields and reject missing, wrong-owner, or non-`VALID` addresses.
- [ ] Add focused controller/service tests for every approved error code.

### 5. Order Service address selection and snapshot

- [ ] Change `BE/order-service/src/main/java/com/stylemind/order/dto/CreateOrderRequest.java` to require `addressId` and `paymentMethod`.
- [ ] Point `BE/order-service/src/main/java/com/stylemind/order/feign/UserClient.java` to `USER_SERVICE_URL` and preserve the common internal-token interceptor.
- [ ] Add an internal address DTO and resolve the address before cart/product/payment work.
- [ ] Add nullable structured snapshot fields to `Order`, `OrderResponse`, and mapping while preserving `shippingAddress`.
- [ ] Build the formatted legacy-compatible address from canonical snapshot data.
- [ ] Add a non-destructive order schema patch under `docs/database/manual-patches/` and synchronize `BE/init-scripts/06-order-db.sql`.
- [ ] RED/GREEN: missing/unknown/wrong-owner/legacy rejection; no payment/cart side effects; valid snapshot; immutability; COD/SePay/idempotency regressions.

### 6. Frontend address management

- [ ] Add Gateway API helpers in `FE/src/features/profile/profile.api.js` for provinces and wards.
- [ ] Replace the canonical `city` input in `FE/src/pages/auth/StyleProfilePage.jsx` with province/ward selectors, address line, phone, recipient, note, and default controls.
- [ ] Clear ward state on province changes, surface safe backend errors, display normalized phone, and mark legacy addresses for update.
- [ ] Add pure helpers/tests for form payload mapping and legacy/valid eligibility.

### 7. Frontend checkout

- [ ] Load saved addresses in `FE/src/pages/customer/CheckoutPage.jsx` and select a `VALID` default when available.
- [ ] Exclude or disable `LEGACY_UNVERIFIED` addresses and offer the existing profile route for updating/adding one.
- [ ] Submit only `{addressId, paymentMethod}` through `FE/src/features/payment/payment.store.js` and `FE/src/features/orders/order.api.js`.
- [ ] Preserve Idempotency-Key lifecycle and COD/SePay payment rendering.
- [ ] Add tests proving no order/payment request is made without a valid selected address.

### 8. Verification, E2E, and documentation

- [ ] Add the repository’s first frontend test command only if required by the existing Node test conventions; keep one framework and do not add duplicate harnesses.
- [ ] Add Playwright config/tests only once, using Gateway-backed flows and environment-provided credentials; no real personal data or internal URLs.
- [ ] RED: run address/checkout E2E before implementation and capture the expected missing-selector/old-contract failure.
- [ ] GREEN: run focused desktop/mobile E2E for address form, validation, checkout contract, COD/SePay, legacy handling, ownership, and snapshot behavior.
- [ ] Validate User Service Flyway upgrade and clean init in disposable databases; validate Order schema patch/init without touching persistent volumes.
- [ ] Update relevant `docs/` service/API/checkout/database docs and create `docs/reports/2026-07-20-structured-vietnamese-address-implementation-report.md` without overwriting the baseline audit.
- [ ] After fresh verification, update only affected `docs/AGENT_WORKSPACE/` files with verified status and residual limitations.
- [ ] Run Maven/frontend tests, type/lint/build checks, Playwright, `git diff --check`, secret/privacy scans, and review the final diff.

## Completion gates

- [ ] Every new entity field has matching migration, init script, DTO, mapping, and test coverage.
- [ ] Legacy addresses are visible but cannot be selected for checkout.
- [ ] Order creation validates ownership and status before payment initialization or cart clearing.
- [ ] Newly created orders have an immutable structured snapshot and a formatted `shippingAddress`.
- [ ] Historical orders remain readable without fabricated phone/codes.
- [ ] Browser traffic uses only Gateway paths and never `/internal/v1/**` or service ports.
- [ ] No secrets, personal data, database records, or Docker volumes were exposed or modified.
