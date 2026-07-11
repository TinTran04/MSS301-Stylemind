# ADR-001: Store product images in Cloudinary

## Status
Accepted

## Date
2026-07-06

## Context

The product service previously uploaded images through an S3/MinIO-specific
implementation and built an AWS URL manually. The admin product flow needs a
hosted image URL, safe replacement, and enough metadata to delete an uploaded
asset without exposing storage credentials to the browser.

Existing seeded images are remote URLs and do not have provider-specific IDs.
The MVP must preserve those records and must not introduce binary database
storage or direct browser access to a provider secret.

## Decision

Use the Cloudinary Java SDK from `product-service`.

- Keep the existing admin product-image subresource API.
- Upload server-side with `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and
  `CLOUDINARY_API_SECRET`.
- Persist `secure_url` as `image_url` and `public_id` as nullable
  `image_public_id`.
- Keep `image_public_id` nullable so legacy and seeded image URLs remain valid.
- Validate image MIME type and a 10 MB maximum before provider upload.
- Delete cloud assets best-effort; cleanup failure must not undo a successful
  product/image update.

## Alternatives Considered

### Keep MinIO/S3

- Useful for local infrastructure and S3-compatible deployments.
- Rejected for product images because the required deployment uses Cloudinary
  delivery and replacement metadata.

### Direct frontend upload

- Reduces backend transfer work.
- Rejected for MVP because signed uploads add another contract and the
  Cloudinary API secret must never reach Vite/browser code.

### Store image binary data in PostgreSQL

- Keeps assets and metadata in one database.
- Rejected because it increases database size and removes CDN delivery benefits.

## Consequences

- Product-service has an external Cloudinary dependency and requires four
  environment variables, including the optional folder setting.
- The database stores URLs and provider IDs, never image bytes.
- Existing remote images continue to render but cannot be deleted from
  Cloudinary because they have no Cloudinary public ID.
- MinIO may remain in shared infrastructure for unrelated services, but
  product-service no longer uses it for product images.
