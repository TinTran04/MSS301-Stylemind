# Product Catalog and Management Design

**Status:** Approved
**Selected layout:** Option B, Product-first toolbar
**Scope:** Product service, admin product management, and public product catalogue

## Goals

- Reuse existing product, category, variant, and image APIs.
- Replace the existing S3/MinIO product image implementation with server-side Cloudinary uploads.
- Persist the Cloudinary public ID beside each product image.
- Complete product creation, category management, image replacement, and variant editing in the existing admin drawer.
- Remove fabricated catalogue data and display only backend product, category, image, and variant information.
- Improve the public catalogue with an image-led, responsive fashion grid that remains native to StyleMind.

## Existing Boundaries

- `Product` owns catalogue metadata and a `categoryId`.
- `ProductVariant` owns SKU, size, color, material, and optional price override.
- `ProductImage` owns product image URL and primary-image state.
- Images and variants remain product subresources. No duplicate aggregate create API will be introduced.
- The public catalogue reads only active products.
- The admin product controller remains protected by `hasRole('ADMIN')`.

## Backend Design

The existing `POST /api/v1/admin/products/{productId}/images` endpoint remains the upload contract. A focused `ProductImageStorage` interface will isolate Cloudinary from `ProductService`. The Cloudinary implementation uploads image bytes with `resource_type=image` and the configured folder, returning the secure URL and public ID.

Uploads reject empty files, files larger than 10 MB, and content types outside JPEG, PNG, WebP, GIF, and AVIF. Storage failures return a stable business error without exposing provider messages or credentials.

`product_images.image_public_id` will be nullable so existing seeded remote images remain valid. A Flyway baseline will describe the current product schema, and a versioned migration will add the new column. The canonical Docker initialization script will receive the same column.

Deleting an image removes the database record and performs Cloudinary deletion best-effort when a public ID exists. Existing remote seed images have no public ID and are never sent to Cloudinary for deletion.

Product responses add `categoryName` and image responses add `publicId`. Existing fields remain unchanged. Category deletion checks both child categories and assigned products and returns HTTP 409 for either conflict.

## Admin Flow

The drawer saves base product information first. For a newly-created product, the returned ID becomes the active editing ID and the drawer remains open so image and variant operations use the existing subresource APIs.

Image selection immediately shows a local preview. Uploading shows progress state and persists the returned image. Replacing the primary image uploads the new asset first, refreshes the product response, and then removes the old image best-effort.

Variant editing uses only existing fields: SKU, size, color, material, and price override. No stock or variant-status field is introduced. Duplicate SKU errors remain backend-enforced and are surfaced as a clear form error.

Category management continues to use the existing public category list plus admin create/update/delete endpoints. Product category selection is populated from real categories.

## Public Catalogue

Option B uses:

- A compact collection heading and real result count.
- A horizontally scrollable category rail.
- A product-first toolbar with Filter and Sort controls.
- Four columns on wide desktop, three on tablet, and two on mobile.
- A 3:4 real product image as the card's primary element.
- Product name, category, VND price, and real size/color summaries.
- A neutral StyleMind empty-image treatment when no image exists.

The client retains backend page metadata instead of fetching 100 products and repaginating locally. Supported filters are search, category, minimum price, maximum price, and backend sort. Unsupported material and fabricated color filters are removed.

Fake fallback photos, AI match scores, ratings, reviews, labels, colors, sizes, and materials are removed. Product detail and home-page consumers continue to receive stable mapped image and variant data, but must render missing information honestly.

## Intentional Limits

- No stock or inventory behavior.
- No ratings, reviews, sale labels, bestseller labels, or conversion metrics.
- No direct browser-to-Cloudinary secret flow.
- No exact Uniqlo styling, measurements, branding, assets, typography, icons, or copy.
- No restoration of `/admin/inventory` or `/admin/customers`.

## Verification

- Product-service unit tests cover upload validation, storage result persistence, provider failure sanitization, best-effort deletion, category conflicts, and category names.
- Frontend lint and production build must pass.
- Browser verification covers desktop and mobile catalogue layouts, real image/empty image states, category/sort/filter behavior, and admin create/edit/image/variant workflows.
- Security verification confirms unauthenticated image upload is 401 and CUSTOMER upload is 403.

