# Implementation Plan: Admin Order Detail Page

## Overview

Replace the admin order eye action's detail drawer with a dedicated,
read-only, Gateway-backed order detail route. Keep order and price snapshots
authoritative, enrich only data already available from Auth/Product/Payment
service contracts, and show safe fallbacks for fields the system does not
store.

## Tasks

### Phase 1: Contract and regression coverage

- [x] Add backend tests for the admin detail response's additive customer,
  variant, and payment enrichment if the contract is extended.
- [x] Add focused frontend utility regression tests for route/query handling,
  price snapshot arithmetic, and null-safe rendering helpers. The browser
  journey covers page states and navigation because this repository has no
  component-test runner configured.

**Checkpoint:** Tests fail for the missing dedicated route and do not fail due
to test setup errors.

### Phase 2: Backend detail contract

- [x] Add optional response DTO fields without changing existing list fields.
- [x] Enrich admin detail data through existing internal clients only where the
  source service already exposes the data.
- [x] Preserve snapshot unit prices, shipping snapshot, admin authorization,
  and best-effort behavior for optional lookups.

**Checkpoint:** Order-service focused tests pass and the admin endpoint remains
admin-protected.

### Phase 3: Dedicated frontend page

- [x] Add the `AdminOrderDetailPage` and focused composed sections.
- [x] Add `/admin/orders/:orderId` inside `RequireAdmin` and `AdminLayout`.
- [x] Change the eye action to navigate with a safe return URL and remove the
  drawer-only detail state from the order list.
- [x] Render supported fields, explicit empty states, safe fallbacks, and
  responsive layouts.

**Checkpoint:** Frontend tests pass and the production build compiles.

### Phase 4: Runtime verification and documentation

- [x] Run build, focused tests, and `git diff --check`; attempted lint, but the repository has no
  installed `eslint` binary (`npm run lint` exits 127).
- [x] Run Playwright against the real admin flow using runtime credentials
  supplied through environment variables, without committing them.
- [x] Capture desktop, item/variant, mobile, and not-found screenshots using
  the existing output convention.
- [x] Update AGENT_WORKSPACE docs only with verified API and runtime evidence.

**Checkpoint:** The real Gateway-backed journey works, no service-port or
internal API browser requests occur, and limitations are documented.

## Dependencies

1. Existing `GET /api/v1/admin/orders/{orderId}` contract and admin route.
2. Existing Auth/Product/Payment service data and internal Feign clients.
3. Existing React Router, API client, Tailwind theme, and admin layout.

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Product details are current catalog data, not purchase snapshots | Medium | Keep item ID and purchase price authoritative; label enrichment as variant details |
| Optional service lookup fails | Medium | Keep base order response usable and render `Chưa có thông tin` |
| Status history contract was added during implementation | Low | Read `statusHistory` from the existing admin detail response; show an empty state only when the API returns no records and never synthesize entries |
| Existing list state is local | Medium | Capture the current list URL and use a safe `/admin/orders` fallback |
| Runtime credentials or data leak into artifacts | High | Use environment variables, redact logs, and do not commit screenshots or secrets |

## Verification Commands

- `npm run lint`
- `npm run build`
- Focused frontend test command discovered from the repository setup
- Focused backend test command only if backend changes
- Playwright focused order-detail journey
- `git diff --check`
