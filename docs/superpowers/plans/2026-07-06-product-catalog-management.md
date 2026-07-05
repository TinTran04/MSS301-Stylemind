# Product Catalog and Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete StyleMind product/category/variant/image management and ship an honest, responsive product-first public catalogue.

**Architecture:** Preserve the existing REST resources and place Cloudinary behind a storage interface used by `ProductService`. Extend existing DTOs without removing fields, migrate `product_images` with Flyway and the canonical init script, then wire the current admin drawer and public catalogue to real API data.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, Flyway, Cloudinary Java SDK, PostgreSQL, React 18, Vite, Axios, Tailwind CSS, Lucide React.

---

### Task 1: Establish product schema migration support

**Files:**
- Modify: `BE/product-service/pom.xml`
- Modify: `BE/product-service/src/main/resources/application.yml`
- Create: `BE/product-service/src/main/resources/db/migration/V1__baseline_product_schema.sql`
- Create: `BE/product-service/src/main/resources/db/migration/V2__product_image_public_id.sql`
- Modify: `BE/init-scripts/03-product-db.sql`

- [ ] Add `flyway-core` and `flyway-database-postgresql`.
- [ ] Configure Flyway with `baseline-on-migrate: true` and baseline version `1`.
- [ ] Reproduce the current product schema in the baseline without changing request/response behavior.
- [ ] Add `ALTER TABLE product_images ADD COLUMN IF NOT EXISTS image_public_id VARCHAR(255)`.
- [ ] Add the same nullable column to `BE/init-scripts/03-product-db.sql`.
- [ ] Run `mvn -pl product-service -am test -DskipTests` and expect compilation success.

### Task 2: Add a tested Cloudinary storage boundary

**Files:**
- Modify: `BE/product-service/pom.xml`
- Delete: `BE/product-service/src/main/java/com/stylemind/product/config/S3Config.java`
- Create: `BE/product-service/src/main/java/com/stylemind/product/config/CloudinaryConfig.java`
- Create: `BE/product-service/src/main/java/com/stylemind/product/service/image/ProductImageStorage.java`
- Create: `BE/product-service/src/main/java/com/stylemind/product/service/image/StoredProductImage.java`
- Create: `BE/product-service/src/main/java/com/stylemind/product/service/image/CloudinaryProductImageStorage.java`
- Create: `BE/product-service/src/test/java/com/stylemind/product/service/image/CloudinaryProductImageStorageTest.java`
- Modify: product-service application YAML files

- [ ] Write failing tests for secure URL/public ID mapping and delete delegation.
- [ ] Replace the AWS S3 dependency with `com.cloudinary:cloudinary-http5`.
- [ ] Bind `cloudinary.cloud-name`, `api-key`, `api-secret`, and `folder`.
- [ ] Construct `Cloudinary` only when credentials are present; emit a stable configuration error otherwise.
- [ ] Upload bytes using `folder`, `resource_type=image`, and `secure_url`.
- [ ] Delete by public ID with `invalidate=true`.
- [ ] Run the focused storage tests and expect PASS.

### Task 3: Persist image metadata and validate uploads

**Files:**
- Modify: `BE/product-service/src/main/java/com/stylemind/product/entity/ProductImage.java`
- Modify: `BE/product-service/src/main/java/com/stylemind/product/dto/ProductImageResponse.java`
- Modify: `BE/product-service/src/main/java/com/stylemind/product/service/ProductService.java`
- Modify: `BE/product-service/src/test/java/com/stylemind/product/service/ProductServiceTest.java`

- [ ] Write failing tests for empty files, invalid MIME types, oversized files, public-ID persistence, and sanitized storage failures.
- [ ] Replace direct `S3Client` use with `ProductImageStorage`.
- [ ] Allow only `image/jpeg`, `image/png`, `image/webp`, `image/gif`, and `image/avif`.
- [ ] Enforce a 10 MB service limit.
- [ ] Persist `imageUrl` and `imagePublicId`.
- [ ] On deletion, delete Cloudinary assets best-effort only when a public ID exists.
- [ ] Never include provider exception text in API errors.
- [ ] Run `mvn -pl product-service -Dtest=ProductServiceTest test` and expect PASS.

### Task 4: Complete category and product response contracts

**Files:**
- Modify: `BE/product-service/src/main/java/com/stylemind/product/dto/ProductResponse.java`
- Modify: `BE/product-service/src/main/java/com/stylemind/product/repository/ProductImageRepository.java`
- Modify: `BE/product-service/src/main/java/com/stylemind/product/repository/ProductVariantRepository.java`
- Modify: `BE/product-service/src/main/java/com/stylemind/product/service/ProductService.java`
- Modify: `BE/product-service/src/main/java/com/stylemind/product/service/CategoryService.java`
- Create: `BE/product-service/src/test/java/com/stylemind/product/service/CategoryServiceTest.java`
- Modify: `BE/product-service/src/test/java/com/stylemind/product/service/ProductServiceTest.java`

- [ ] Write failing tests asserting `categoryName` and HTTP-409 category conflicts.
- [ ] Add `categoryName` without removing any response field.
- [ ] Batch-load categories, images, and variants for product pages to avoid per-card query multiplication.
- [ ] Block deletion when children or products reference a category with stable conflict codes.
- [ ] Run all product-service tests and expect PASS.

### Task 5: Update deployment configuration

**Files:**
- Modify: `BE/docker-compose.full.yml`
- Modify: `BE/product-service/src/main/resources/application-docker.yml`
- Modify: `BE/product-service/src/main/resources/application-local.yml`
- Modify: repository backend environment example if present

- [ ] Add `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, and `CLOUDINARY_FOLDER`.
- [ ] Remove product-service S3 variables while leaving unrelated MinIO infrastructure untouched.
- [ ] Confirm no frontend environment file contains a Cloudinary secret.
- [ ] Search for product-service `S3Client`, `S3_SECRET_KEY`, and hard-coded Cloudinary URLs; expect no product-service matches.

### Task 6: Make the frontend product mapper honest and paginated

**Files:**
- Modify: `FE/src/features/products/product.api.js`
- Create: `FE/src/features/products/product.mapper.js`
- Create: `FE/src/components/customer/ProductImage.jsx`
- Modify: `FE/src/pages/customer/HomePage.jsx`
- Modify: `FE/src/pages/customer/ProductDetailPage.jsx`

- [ ] Extract pure mapping helpers that preserve page metadata.
- [ ] Remove fallback photos, AI scores, ratings, reviews, fake colors, fake sizes, and fake materials.
- [ ] Return `primaryImageUrl: null` when no image exists.
- [ ] Keep `variants`, real `colors`, real `sizes`, and first available variant ID.
- [ ] Add a shared neutral image state that does not fetch external assets.
- [ ] Update home/detail consumers for nullable images and honest variant data.
- [ ] Run `npm run lint` and `npm run build` in `FE`; expect PASS.

### Task 7: Complete the admin product drawer

**Files:**
- Modify: `FE/src/pages/admin/ProductManagementPage.jsx`
- Modify: `FE/src/features/products/admin-product.api.js`
- Modify: `FE/src/features/products/admin-category.api.js`
- Reuse: `FE/src/components/common/Drawer.jsx`, `Button.jsx`, `Input.jsx`

- [ ] Call `createProduct` in create mode and retain the returned product as the editing target.
- [ ] Add selected-file preview, upload loading, validation error, replacement, and success states.
- [ ] Upload only after a product ID exists.
- [ ] Delete the previous primary image after a successful replacement when safe.
- [ ] Keep variant fields limited to SKU, size, color, material, and price override.
- [ ] Surface duplicate-SKU and category-conflict messages.
- [ ] Replace the external table placeholder with the shared neutral image state.
- [ ] Keep category CRUD and real category selection in the existing drawer flow.
- [ ] Run frontend lint/build and expect PASS.

### Task 8: Implement Option B public catalogue

**Files:**
- Modify: `FE/src/pages/customer/ProductCatalogPage.jsx`
- Modify: `FE/src/components/customer/ProductFilter.jsx`
- Modify: `FE/src/components/customer/ProductCard.jsx`

- [ ] Render the collection heading and real backend result count.
- [ ] Add a horizontally scrollable real-category rail.
- [ ] Add compact Filter and Sort controls using existing StyleMind tokens.
- [ ] Use backend pagination, search, category, price, newest, and price sort.
- [ ] Remove unsupported material/fake color controls.
- [ ] Render a 4/3/2-column responsive grid.
- [ ] Render real image, name, category, VND price, and real color/size summary.
- [ ] Preserve cart behavior through real variant IDs.
- [ ] Run frontend lint/build and expect PASS.

### Task 9: End-to-end verification

**Files:**
- Verify only; fix scoped defects in files above.

- [ ] Run `mvn -pl product-service test`.
- [ ] Run `npm run lint` and `npm run build` in `FE`.
- [ ] Start the product service dependencies and frontend using available local configuration.
- [ ] Verify `/shop` at desktop, tablet, and mobile widths with screenshots and canvas/pixel checks where applicable.
- [ ] Verify active-only public products, category filtering, result count, sorting, pagination, real images, and neutral missing-image state.
- [ ] Verify admin create, category assignment, image upload/replacement, and variant create/edit.
- [ ] Verify unauthenticated upload is 401 and CUSTOMER upload is 403.
- [ ] Confirm `/admin/inventory` and `/admin/customers` remain absent.
- [ ] Confirm no fake product data or fake product images remain in the product catalogue path.

