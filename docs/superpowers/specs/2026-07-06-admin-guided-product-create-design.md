# Admin Guided Product Create Design

**Status:** Implemented
**Date:** 2026-07-06
**Scope:** product-service, admin product frontend, and product documentation

## Goal

Guide an administrator through creating product information, variants, and
optional images while preserving the invariant that every ACTIVE product has at
least one variant.

## Compatibility Constraints

- Do not change the database schema.
- Do not change `ProductResponse`.
- Do not add nested category data or `effectivePrice`.
- Do not add variants to the product create request.
- Keep all existing variant APIs.
- Keep public product paths and ACTIVE-only semantics.
- Keep frontend traffic on the shared API Gateway Axios client.
- Do not send identity headers from the frontend.

## Backend Domain Invariant

An ACTIVE product must have at least one variant.

The service layer enforces this invariant on every relevant write path:

1. `createProduct` always persists the new product as `INACTIVE`, regardless of
   the requested status.
2. `updateProduct` rejects a requested `ACTIVE` status when the product has no
   variants.
3. `updateProductStatus` rejects `ACTIVE` when the product has no variants.
4. `deleteVariant` rejects deletion when the product is ACTIVE and the target
   is its only remaining variant.

The final-variant rejection is:

- HTTP status: `409 Conflict`
- error code: `LAST_ACTIVE_VARIANT`
- message: `Cannot delete the last variant of an active product. Deactivate the product before deleting its final variant.`

Activation without variants returns:

- HTTP status: `409 Conflict`
- error code: `PRODUCT_REQUIRES_VARIANT`
- message: `Cannot activate a product without variants. Add at least one variant before publishing it.`

No status is changed implicitly when variant deletion fails.

## Public Visibility

Public product list, detail, and variant endpoints must never expose a product
without variants. ACTIVE status remains the primary publication flag, and
public repository/service queries add a defensive variant-existence condition
for legacy or externally-corrupted rows.

Admin queries continue to show INACTIVE products without variants so the guided
workflow can be resumed.

## Repository Additions

Add derived/query methods without schema changes:

- `existsByProductId(String productId)`
- `countByProductId(String productId)`
- a pessimistic product-row lookup used by activation/update and variant deletion
  to serialize invariant-sensitive mutations;
- an ACTIVE detail query that also requires an existing variant;
- a variant-existence condition in the public paginated product query.

## Guided Add Product Flow

The existing drawer becomes a three-step flow only when opened in Add mode.
Edit Product keeps its current persisted management behavior.

### Step 1: Product Info

- Collect the existing product fields.
- Category is required by the frontend.
- Status is displayed as INACTIVE and is not user-selectable.
- Submit the existing product create request with `status: "INACTIVE"`.
- Store the returned product as the active created product.
- Keep the drawer open and move automatically to Step 2.
- Show:
  `Product has been created as INACTIVE. Add at least one variant before publishing it.`

The backend still overrides status to INACTIVE, so the invariant does not rely
on frontend behavior.

### Step 2: Variants

- Reuse the existing persisted variant form and APIs.
- Allow add, edit, and delete.
- The newly-created product remains INACTIVE after its first variant is added.
- Continue/Finish/Publish controls remain disabled while variant count is zero.
- After at least one variant exists, allow movement to Step 3.

### Step 3: Images and Finish

- Reuse the existing image preview, validation, upload, replacement, and delete
  behavior.
- Images remain optional and use the existing product ID upload API.
- Image upload failure keeps the drawer open and does not remove the valid
  product or variants.
- `Finish Inactive` closes the drawer and refreshes the admin list.
- `Publish Active` calls the existing status PATCH endpoint, closes on success,
  and refreshes the list.
- A backend `409` is shown verbatim as the user-facing publish error.

## Close Behavior

Closing after Step 1 or Step 2 never deletes the product.

If the created product still has zero variants, show:
`This product has no variants and will remain inactive.`

Refresh the admin list so the incomplete INACTIVE product is visible and can be
resumed through Edit Product.

## Frontend State

Add focused state alongside the existing drawer state:

- drawer mode (`add` or `edit`);
- add-flow step (`product`, `variants`, or `images`);
- created product held in the existing `editingProduct` shape;
- flow-level warning/error.

Avoid duplicating the variant editor. Its handlers branch only on whether a
persisted product exists, which is true from Step 2 onward.

## Error Handling

All backend errors continue through `BusinessException`,
`GlobalExceptionHandler`, and the existing `ApiResponse` shape.

The frontend uses the normalized Axios error message and does not construct
identity/security headers.

## Tests

Backend tests:

- create request with ACTIVE is persisted as INACTIVE;
- create request with INACTIVE remains INACTIVE;
- activate with zero variants returns `PRODUCT_REQUIRES_VARIANT`/409;
- activate with at least one variant succeeds;
- PUT cannot bypass the activation guard;
- deleting the final variant of an ACTIVE product returns the exact
  `LAST_ACTIVE_VARIANT`/409 contract;
- deleting a variant from an INACTIVE product remains allowed;
- public list/detail exclude products without variants.

Frontend tests use the existing Node test setup for pure flow helpers:

- initial Add Product step is Product Info;
- successful create advances to Variants;
- zero variants cannot continue/publish;
- first variant enables continue/publish;
- status sent during Step 1 is INACTIVE.

The production build and existing product-service test suite remain required.

## Documentation Updates

Update:

- product-service contract and business rules;
- API catalogue error behavior;
- functional requirements and PRD;
- admin frontend requirements and user stories;
- delivery roadmap and changelog;
- the earlier catalogue design statement describing the initial create flow.

No migration, schema document, or manually-maintained OpenAPI file is needed.
