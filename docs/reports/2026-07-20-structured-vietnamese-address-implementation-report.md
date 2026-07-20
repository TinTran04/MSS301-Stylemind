# Structured Vietnamese Address and Checkout Implementation Report

## 1. Scope and outcome

This report records the implementation requested for structured Vietnamese
shipping addresses, Vietnamese phone validation, and addressId-only checkout.
The existing baseline audit at
`docs/reports/2026-07-20-user-address-phone-order-current-state-audit.md` was
left unchanged.

The implementation is additive where historical data requires compatibility:
existing free-text addresses are retained and marked `LEGACY_UNVERIFIED`,
while newly created or edited addresses are validated and stored as `VALID`.
No province or ward was inferred for legacy rows.

During the real checkout verification, the first rebuilt run exposed a
configuration defect rather than a business-rule defect: User Service did not
receive the canonical internal token, so its protected address lookup returned
403 before ownership validation. The minimal Compose mapping was added and
the final Gateway-backed Playwright flow passed.

## 2. Final runtime contract

### User Service public endpoints

All browser requests use the API Gateway `/api/v1/**` surface:

| Method | Gateway path | Purpose |
|---|---|---|
| `GET` | `/api/v1/users/administrative/provinces` | List the bundled active province dataset |
| `GET` | `/api/v1/users/administrative/provinces/{provinceCode}/wards` | List wards for a verified province |
| `GET` | `/api/v1/users/addresses` | List the authenticated user's addresses |
| `POST` | `/api/v1/users/addresses` | Validate and create an address |
| `PUT` | `/api/v1/users/addresses/{addressId}` | Validate and update an address |
| `DELETE` | `/api/v1/users/addresses/{addressId}` | Delete an owned address |

The create/update body uses `provinceCode` and `wardCode`; province and ward
names are resolved server-side. The service validates the administrative
relationship and normalizes the phone number to E.164 with Google's
`libphonenumber` library using region `VN`.

### Internal address lookup

Order Service calls User Service through the protected internal endpoint:

`GET /internal/v1/users/{userId}/addresses/{addressId}`

The request carries the existing `X-Internal-Token` through the shared Feign
configuration. User Service rejects a missing address, an address owned by a
different user, and an address whose status is not `VALID`.

### Checkout request

The order creation body is now:

```json
{
  "addressId": "<ADDRESS_ID>",
  "paymentMethod": "cod"
}
```

The browser no longer sends an arbitrary `shippingAddress` string. It also does
not call `/internal/v1/**`, service ports, or inject identity headers.

### Order snapshot

Order Service preserves the existing non-null `shipping_address` field for
compatibility and additionally persists:

- `source_address_id`
- `shipping_recipient_name`
- `shipping_phone`
- `shipping_province_code`
- `shipping_province_name`
- `shipping_ward_code`
- `shipping_ward_name`
- `shipping_address_line`
- `shipping_note`

The snapshot is populated before cart/product/payment processing and is never
re-read from the user's mutable address during display of a historical order.

## 3. Data and migration strategy

The bundled administrative dataset is:

- repository: `thanglequoc/vietnamese-provinces-database`
- release: `v4.0.0`
- pinned commit: `86361845ba60ee779905ef07f04d7db33c798d04`
- license: MIT
- local resource: `BE/user-service/src/main/resources/data/vietnam-admin-units-v4.0.0.json`

Attribution and import details are recorded in the adjacent
`BE/user-service/src/main/resources/data/README.md`.

User Service uses Flyway migration
`V3__structured_vietnamese_addresses.sql` to create administrative tables and
add nullable structured columns before marking legacy rows
`LEGACY_UNVERIFIED`. The importer only seeds the bundled province/ward data
when the administrative tables are empty.

Order Service has no Flyway history in this repository. Fresh initialization
was updated in `BE/init-scripts/06-order-db.sql`; existing volumes have a
separate rerunnable patch at
`docs/database/manual-patches/2026-07-20-structured-shipping-snapshot.sql`.
The patch only adds missing columns/indexes and performs no backfill or
destructive operation.

## 4. Files changed

### Backend

- User Service address DTO/entity/controller/service/repository additions.
- `VietnamesePhoneNumberService` using `libphonenumber`.
- Administrative province/ward entities, repositories, importer, and dataset.
- User Service Flyway migration and fresh init script synchronization.
- Order Service `CreateOrderRequest`, `Order`, `OrderResponse`, `OrderService`,
  and protected `UserAddressClient`.
- Docker Compose propagation of `USER_SERVICE_URL` to Order Service.
- Order fresh init script and non-destructive manual schema patch.
- Focused User and Order Service regression tests.

### Frontend

- Structured profile address form with province/ward selectors and phone
  feedback.
- Checkout address selection restricted to `VALID` owned addresses.
- Order creation payload changed to `addressId` plus payment method.
- Focused address/phone utility tests.
- First repository Playwright setup and a Gateway-only checkout test using
  environment-provided credentials.

### Documentation

- API, checkout-saga, frontend requirements, service, changelog, and schema
  audit documentation updated.
- Implementation plan:
  `docs/superpowers/plans/2026-07-20-structured-vietnamese-address-checkout.md`.
- This implementation report.
- The baseline current-state audit was not overwritten.

## 5. Compatibility and safety rules

- Existing legacy address rows retain their original free-text values.
- Legacy rows are not eligible for checkout until the user edits them through
  the validated form.
- Historical orders keep their existing `shipping_address` value and remain
  readable.
- Purchased product prices and payment behavior were not changed.
- No database row was manually edited by hand. The approved User Service
  Flyway migration ran on startup, added the schema, and explicitly marked
  pre-existing addresses `LEGACY_UNVERIFIED`; the Playwright flow also created
  one test address and order through the normal UI/API path.
- No Docker volume was deleted or reset.
- No credentials, tokens, real customer data, or phone values are included in
  this report.

## 6. Verification

### Passing checks

- `mvn -pl user-service -DskipTests compile` — passed.
- `mvn -pl order-service -DskipTests compile` — passed.
- User Service focused tests — 15 tests passed.
- Order Service focused tests (`OrderServiceTest`,
  `OrderAddressCheckoutTest`) — 18 tests passed.
- Frontend unit tests — 102 tests passed.
- `npm run build` — passed; Vite emitted only its existing chunk-size warning.
- `docker compose --profile all config --quiet` — passed.
- `git diff --check` — passed at the last source verification point.

`npm run lint` could not run because the existing frontend dependency set does
not contain an ESLint binary. No second lint framework was added.

The full Maven reactor remains affected by a pre-existing `common-lib` JWT
signature test failure on the local JDK/runtime; the User and Order focused
tests listed above are independent and passing.

### Playwright status

The repository had no existing Playwright harness, so a focused ESM config and
one real-flow spec were added using environment variables for credentials. The
browser binary installed successfully and the first runtime attempt reached the
structured profile page, but the old User/Order containers were still serving
the pre-change image and the province options were empty. That run failed at
province selection and is not counted as a successful end-to-end verification.

The affected application containers were then rebuilt with `--no-deps`; the
databases and volumes were not recreated. The rebuilt runtime exposed a second
confirmed issue: User Service's `InternalAuthFilter` rejected Order Service's
address lookup because the User Service Compose block did not receive the
canonical `INTERNAL_TOKEN`. A focused Compose regression test failed before
the mapping and passed after `INTERNAL_TOKEN: ${INTERNAL_TOKEN}` was added.

The final Playwright run passed 1/1. It logged in with environment-provided
credentials, created a validated address, submitted `POST /api/v1/orders` with
`addressId` and no `shippingAddress`, completed checkout, and observed no
browser request to `/internal/v1/**`, Docker service hostnames, or backend
service ports.

## 7. Remaining follow-up

1. Add a CI lint dependency only through a separate approved tooling task.
2. Review the non-destructive Order manual patch against every already-created
   Order database volume before applying it operationally.
