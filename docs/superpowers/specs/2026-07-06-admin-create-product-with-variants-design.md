# Admin Create Product with Variants Design

**Status:** Superseded by `2026-07-06-admin-guided-product-create-design.md`
**Date:** 2026-07-06
**Scope:** product-service, admin product frontend, and product documentation

> Historical proposal only. Do not implement the atomic create behavior below.

## Goal

This atomic-create proposal was superseded before implementation. The accepted
design preserves the existing product create request and uses a guided
product-info, variant, and image workflow.

## Existing Contracts Preserved

- `POST /api/v1/admin/products` remains the create endpoint.
- `PUT /api/v1/admin/products/{id}` continues to update product information
  without replacing variants.
- Existing variant create/update/delete subresource APIs remain available for
  Edit Product.
- `ProductResponse` retains `categoryId`, `categoryName`, `variants`, and
  `images`. The create change does not replace these fields with a nested
  category object and does not add `effectivePrice`.
- Public listing/detail behavior remains ACTIVE-only.
- All frontend calls continue through the shared gateway Axios client. The
  frontend does not call internal endpoints or inject identity headers.

## Backend Request Contract

Introduce `ProductCreateRequest` for POST only. Keep `ProductRequest` for PUT.

`ProductCreateRequest` contains:

- required product name;
- required positive base price;
- required category ID;
- optional description and merchandising attributes;
- valid status (`ACTIVE`, `INACTIVE`, or `DISCONTINUED`);
- a non-empty `List<@Valid ProductVariantRequest> variants`.

`ProductVariantRequest` remains the shared variant input:

- required SKU, size, and color;
- optional material;
- optional positive price override.

This avoids validation groups and prevents the new create-only variant rule
from breaking Edit Product.

## Atomic Create Flow

`ProductService.createProduct(ProductCreateRequest)` receives an explicit
`@Transactional` annotation.

Before writing:

1. Verify the category exists.
2. Verify at least one variant was submitted.
3. Normalize SKU comparison by trimming and comparing case-insensitively.
4. Reject duplicate SKUs within the request.
5. Query existing variants with `findBySkuIn` and reject any globally-used SKU.

After validation:

1. Save the product to obtain its generated ID.
2. Build all variants with that product ID.
3. Save variants as one collection.
4. Return the existing product response with the saved variants and an empty
   image list.

Any unchecked validation or persistence failure after the product save marks
the transaction for rollback, so no product is committed without its variants.

## Error Semantics

All errors use the existing `ApiResponse` and `GlobalExceptionHandler`.

| Condition | HTTP | Error code |
|---|---:|---|
| Missing/empty variants | 400 | `VALIDATION_ERROR` |
| Missing required category | 400 | `VALIDATION_ERROR` |
| Unknown category | 404 | `CATEGORY_NOT_FOUND` |
| Invalid base price or price override | 400 | `VALIDATION_ERROR` |
| Duplicate SKU in submitted variants | 409 | `DUPLICATE_VARIANT_SKU` |
| SKU already exists globally | 409 | `SKU_EXISTS` |
| Variant persistence failure | 500 | existing generic internal error |

Existing separate variant endpoints remain behaviorally compatible. Their
request paths and response shapes do not change.

## Add Product Frontend Flow

The existing drawer gains draft variant behavior without changing Edit Product:

- `draftVariants` stores unsaved variants while `editingProduct` is null.
- The existing variant form adds or updates draft entries locally in Add mode.
- Draft entries use a local-only ID for stable rendering.
- Delete removes the draft locally without an API call.
- Duplicate draft SKUs are rejected case-insensitively.
- Edit mode continues to call the existing persisted variant APIs.

Drawer section order:

1. Product Info
2. Variants
3. Images
4. Submit/Close actions

Add Product cannot submit until product fields are valid, a category is
selected, and at least one valid draft variant exists. The create payload
includes the draft variants.

## Image Behavior

Image type, size validation, and local preview remain unchanged.

After atomic product creation succeeds:

- if no image was selected, refresh the list and close the drawer;
- if an image was selected, upload it through
  `POST /api/v1/admin/products/{productId}/images`;
- if upload succeeds, refresh and close;
- if upload fails, keep the created product, switch the drawer into Edit mode,
  retain a visible warning, and allow retry. Image failure never attempts to
  undo the valid product and variants.

## Validation Structure

Create a small pure frontend helper for:

- normalizing draft variant values;
- validating required product/create fields;
- detecting duplicate draft SKUs;
- constructing the create payload.

This helper is covered with Node's existing built-in test setup. No new React
test framework is introduced for this feature.

## Tests

Backend unit tests cover:

- one-variant success;
- multiple-variant success;
- missing variants;
- duplicate request SKU;
- globally-existing SKU;
- invalid category;
- invalid optional price override through bean validation;
- returned response contains saved variants.

A transaction integration test uses a real product repository and a failing
variant repository boundary to prove a runtime variant-save failure rolls back
the product row.

Frontend helper tests cover:

- submit validation rejects no variants;
- adding/normalizing a draft variant;
- duplicate draft SKU rejection;
- create payload includes variants.

Existing product-service tests, mapper tests, and frontend production build
remain required verification gates.

## Documentation Updates

Update the canonical product service, API catalogue, functional requirements,
PRD, admin frontend requirements, user stories, roadmap, changelog, and the
earlier catalogue design where it currently states that variants are always
created after the product.

No schema migration is required because products and variants already use the
same product database and existing tables.
