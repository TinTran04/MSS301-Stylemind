# Admin Guided Product Creation Implementation Plan

> **For Codex:** Use `superpowers:executing-plans` to implement this plan task by task. Apply `superpowers:test-driven-development` for every behavior change and `superpowers:verification-before-completion` before reporting completion.

**Goal:** Change Add Product into a guided Product Info -> Variants -> Images/Finish workflow while enforcing that every ACTIVE product has at least one variant.

**Architecture:** Keep the existing product and variant APIs separate. Step 1 creates the product through the existing product endpoint and the service always stores it as `INACTIVE`; Step 2 reuses the existing variant endpoints; Step 3 reuses the existing image and status endpoints. The product-service owns and enforces the invariant at every status and final-variant mutation boundary, while public queries defensively require both `ACTIVE` status and at least one variant.

**Tech Stack:** Java 17, Spring Boot 3, Spring Data JPA, JUnit 5, Mockito, React, Vite, Axios, Node test runner.

**Constraints:**
- Do not change the database schema.
- Do not change `ProductResponse`, category fields, or add `effectivePrice`.
- Do not add variants to the create-product request.
- Keep the existing separate variant APIs and Edit Product flow.
- Frontend requests must use the existing gateway API client only.
- Do not introduce frontend calls to `/internal/**` or manually send identity headers.

---

## Task 1: Lock Product Creation and Activation Invariants with Tests

**Files:**
- Modify: `BE/product-service/src/test/java/com/stylemind/product/service/ProductServiceTest.java`
- Modify: `BE/product-service/src/main/java/com/stylemind/product/repository/ProductVariantRepository.java`
- Modify: `BE/product-service/src/main/java/com/stylemind/product/service/ProductService.java`

### Step 1: Add failing service tests

Add tests that prove:

```java
@Test
void createProduct_withRequestedActiveStatus_persistsInactive() {
    ProductRequest request = validProductRequest();
    request.setStatus("ACTIVE");

    when(categoryRepository.existsById(request.getCategoryId())).thenReturn(true);
    when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
        Product saved = invocation.getArgument(0);
        saved.setId("created-product");
        return saved;
    });
    when(categoryRepository.findById(request.getCategoryId()))
            .thenReturn(Optional.of(category));
    when(productVariantRepository.findByProductId("created-product"))
            .thenReturn(List.of());
    when(productImageRepository.findByProductId("created-product"))
            .thenReturn(List.of());

    ProductResponse response = productService.createProduct(request, "admin-1");

    ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(productCaptor.capture());
    assertEquals(Product.ProductStatus.INACTIVE, productCaptor.getValue().getStatus());
    assertEquals("INACTIVE", response.getStatus());
}

@Test
void updateProductStatus_activeWithoutVariants_throwsConflict() {
    when(productRepository.findById("p2")).thenReturn(Optional.of(inactiveProduct));
    when(productVariantRepository.existsByProductId("p2")).thenReturn(false);

    BusinessException exception = assertThrows(
            BusinessException.class,
            () -> productService.updateProductStatus(
                    "p2",
                    new StatusUpdateRequest("ACTIVE"),
                    "admin-1"
            )
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    assertEquals("PRODUCT_REQUIRES_VARIANT", exception.getErrorCode());
    assertEquals(
            "Cannot activate a product without variants. Add at least one variant before publishing it.",
            exception.getMessage()
    );
    verify(productRepository, never()).save(any());
}

@Test
void updateProductStatus_activeWithVariant_succeeds() {
    when(productRepository.findById("p2")).thenReturn(Optional.of(inactiveProduct));
    when(productVariantRepository.existsByProductId("p2")).thenReturn(true);
    when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    stubProductResponseRelations(inactiveProduct);

    ProductResponse response = productService.updateProductStatus(
            "p2",
            new StatusUpdateRequest("ACTIVE"),
            "admin-1"
    );

    assertEquals("ACTIVE", response.getStatus());
}

@Test
void updateProduct_activeWithoutVariants_throwsConflict() {
    ProductRequest request = validProductRequest();
    request.setStatus("ACTIVE");
    when(productRepository.findById("p2")).thenReturn(Optional.of(inactiveProduct));
    when(categoryRepository.existsById(request.getCategoryId())).thenReturn(true);
    when(productVariantRepository.existsByProductId("p2")).thenReturn(false);

    BusinessException exception = assertThrows(
            BusinessException.class,
            () -> productService.updateProduct("p2", request, "admin-1")
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    assertEquals("PRODUCT_REQUIRES_VARIANT", exception.getErrorCode());
    verify(productRepository, never()).save(any());
}
```

Add or adapt local test helpers so they match the current DTO constructors and fixture fields:

```java
private ProductRequest validProductRequest() {
    ProductRequest request = new ProductRequest();
    request.setName("Cotton Shirt");
    request.setDescription("Everyday shirt");
    request.setBasePrice(new BigDecimal("379000"));
    request.setCategoryId(1L);
    request.setAestheticStyle("Korean");
    request.setTargetDemographic("NU");
    request.setSeasonalProperty("ALL_SEASON");
    request.setStatus("INACTIVE");
    return request;
}

private void stubProductResponseRelations(Product product) {
    when(categoryRepository.findById(product.getCategoryId()))
            .thenReturn(Optional.of(category));
    when(productVariantRepository.findByProductId(product.getId()))
            .thenReturn(List.of());
    when(productImageRepository.findByProductId(product.getId()))
            .thenReturn(List.of());
}
```

Use the repository's actual `BusinessException` accessor names if they differ from the example. Do not weaken the assertions: status, error code, and message must all be verified.

### Step 2: Run the focused tests and confirm they fail

Run:

```bash
cd BE
mvn -pl product-service -Dtest=ProductServiceTest test
```

Expected: the new creation/status tests fail because creation still accepts `ACTIVE` and activation does not check variant existence.

### Step 3: Add the variant existence repository method

In `ProductVariantRepository` add:

```java
boolean existsByProductId(String productId);
```

### Step 4: Implement the service invariant

In `ProductService`, introduce one service-level guard:

```java
private void ensureProductCanBeActive(String productId, Product.ProductStatus status) {
    if (status == Product.ProductStatus.ACTIVE
            && !productVariantRepository.existsByProductId(productId)) {
        throw new BusinessException(
                HttpStatus.CONFLICT,
                "PRODUCT_REQUIRES_VARIANT",
                "Cannot activate a product without variants. Add at least one variant before publishing it."
        );
    }
}
```

In `createProduct`, ignore the requested status and build the new entity with:

```java
.status(Product.ProductStatus.INACTIVE)
```

In both `updateProduct` and `updateProductStatus`, parse and validate the requested status with the existing enum/error convention, then call:

```java
ensureProductCanBeActive(product.getId(), requestedStatus);
```

Call the guard before mutating or saving the product. Preserve all request/response DTO shapes.

### Step 5: Re-run the focused tests

Run:

```bash
cd BE
mvn -pl product-service -Dtest=ProductServiceTest test
```

Expected: creation and activation invariant tests pass.

### Step 6: Commit checkpoint

```bash
git add BE/product-service/src/main/java/com/stylemind/product/repository/ProductVariantRepository.java \
  BE/product-service/src/main/java/com/stylemind/product/service/ProductService.java \
  BE/product-service/src/test/java/com/stylemind/product/service/ProductServiceTest.java
git commit -m "feat(product): require variants before activation"
```

Do not commit if unrelated user changes are present in these files; keep the checkpoint logical and preserve their work.

---

## Task 2: Prevent Deletion of the Final Variant of an Active Product

**Files:**
- Modify: `BE/product-service/src/test/java/com/stylemind/product/service/ProductServiceTest.java`
- Modify: `BE/product-service/src/main/java/com/stylemind/product/repository/ProductVariantRepository.java`
- Modify: `BE/product-service/src/main/java/com/stylemind/product/service/ProductService.java`

### Step 1: Add failing deletion tests

Add:

```java
@Test
void deleteVariant_lastVariantOfActiveProduct_throwsConflict() {
    when(productVariantRepository.findById("v1")).thenReturn(Optional.of(variant));
    when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));
    when(productVariantRepository.countByProductId("p1")).thenReturn(1L);

    BusinessException exception = assertThrows(
            BusinessException.class,
            () -> productService.deleteVariant("p1", "v1", "admin-1")
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    assertEquals("LAST_ACTIVE_VARIANT", exception.getErrorCode());
    assertEquals(
            "Cannot delete the last variant of an active product. Deactivate the product before deleting its final variant.",
            exception.getMessage()
    );
    verify(productVariantRepository, never()).delete(any());
}

@Test
void deleteVariant_lastVariantOfInactiveProduct_deletesVariant() {
    variant.setProductId("p2");
    when(productVariantRepository.findById("v1")).thenReturn(Optional.of(variant));
    when(productRepository.findById("p2")).thenReturn(Optional.of(inactiveProduct));
    when(productVariantRepository.countByProductId("p2")).thenReturn(1L);

    productService.deleteVariant("p2", "v1", "admin-1");

    verify(productVariantRepository).delete(variant);
}

@Test
void deleteVariant_oneOfMultipleVariantsOfActiveProduct_deletesVariant() {
    when(productVariantRepository.findById("v1")).thenReturn(Optional.of(variant));
    when(productRepository.findById("p1")).thenReturn(Optional.of(activeProduct));
    when(productVariantRepository.countByProductId("p1")).thenReturn(2L);

    productService.deleteVariant("p1", "v1", "admin-1");

    verify(productVariantRepository).delete(variant);
}
```

Update the existing audit-log deletion test to stub `productRepository.findById(...)` and `countByProductId(...)`, so it exercises a legal deletion path.

### Step 2: Run the focused tests and confirm failure

```bash
cd BE
mvn -pl product-service -Dtest=ProductServiceTest test
```

Expected: final-variant deletion is currently allowed, so the conflict test fails.

### Step 3: Add the count repository method

In `ProductVariantRepository` add:

```java
long countByProductId(String productId);
```

### Step 4: Enforce the confirmed final-variant contract

In `ProductService.deleteVariant`, after confirming that the variant belongs to the path product and before deleting it:

```java
Product product = productRepository.findById(productId)
        .orElseThrow(() -> new BusinessException(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                "Product not found"
        ));

if (product.getStatus() == Product.ProductStatus.ACTIVE
        && productVariantRepository.countByProductId(productId) <= 1) {
    throw new BusinessException(
            HttpStatus.CONFLICT,
            "LAST_ACTIVE_VARIANT",
            "Cannot delete the last variant of an active product. Deactivate the product before deleting its final variant."
    );
}
```

Keep the existing audit logging and deletion behavior unchanged for legal paths. Do not auto-deactivate the product.

### Step 5: Re-run focused tests

```bash
cd BE
mvn -pl product-service -Dtest=ProductServiceTest test
```

Expected: all variant deletion tests and existing audit tests pass.

### Step 6: Commit checkpoint

```bash
git add BE/product-service/src/main/java/com/stylemind/product/repository/ProductVariantRepository.java \
  BE/product-service/src/main/java/com/stylemind/product/service/ProductService.java \
  BE/product-service/src/test/java/com/stylemind/product/service/ProductServiceTest.java
git commit -m "feat(product): protect last active variant"
```

---

## Task 3: Defensively Hide Variantless Products from Public APIs

**Files:**
- Modify: `BE/product-service/src/main/java/com/stylemind/product/repository/ProductRepository.java`
- Modify: `BE/product-service/src/main/java/com/stylemind/product/service/ProductService.java`
- Modify: `BE/product-service/src/test/java/com/stylemind/product/service/ProductServiceTest.java`

### Step 1: Add failing public service tests

Replace public-detail mocks that use `findByIdAndStatus` with a sellable-product query and add:

```java
@Test
void getProduct_variantlessActiveProduct_isNotPubliclyVisible() {
    when(productRepository.findSellableById("p1")).thenReturn(Optional.empty());

    BusinessException exception = assertThrows(
            BusinessException.class,
            () -> productService.getProduct("p1")
    );

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    verify(productRepository).findSellableById("p1");
}

@Test
void getVariants_variantlessActiveProduct_isNotPubliclyVisible() {
    when(productRepository.findSellableById("p1")).thenReturn(Optional.empty());

    BusinessException exception = assertThrows(
            BusinessException.class,
            () -> productService.getVariants("p1")
    );

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    verify(productVariantRepository, never()).findByProductId(anyString());
}
```

Adapt the current public success tests to mock `findSellableById`.

### Step 2: Run the focused tests and confirm failure

```bash
cd BE
mvn -pl product-service -Dtest=ProductServiceTest test
```

Expected: tests fail because public detail/variant access still uses status alone.

### Step 3: Add a sellable-product repository query

In `ProductRepository` add:

```java
@Query("""
        SELECT p
        FROM Product p
        WHERE p.id = :id
          AND p.status = 'ACTIVE'
          AND EXISTS (
              SELECT v.id
              FROM ProductVariant v
              WHERE v.productId = p.id
          )
        """)
Optional<Product> findSellableById(@Param("id") String id);
```

Add the same `EXISTS` predicate to the existing public `searchAndFilter` JPQL:

```sql
AND EXISTS (
    SELECT v.id
    FROM ProductVariant v
    WHERE v.productId = p.id
)
```

Do not add this predicate to admin queries.

### Step 4: Route public detail and variant reads through the sellable query

In `ProductService.getProduct` and the public `getVariants` method, replace status-only lookup with:

```java
productRepository.findSellableById(productId)
```

Map the empty result to the existing public product-not-found error. Keep admin retrieval unchanged.

### Step 5: Re-run focused tests and compile product-service

```bash
cd BE
mvn -pl product-service -Dtest=ProductServiceTest test
mvn -pl product-service -DskipTests compile
```

Expected: tests pass and Spring Data query parsing compiles.

### Step 6: Commit checkpoint

```bash
git add BE/product-service/src/main/java/com/stylemind/product/repository/ProductRepository.java \
  BE/product-service/src/main/java/com/stylemind/product/service/ProductService.java \
  BE/product-service/src/test/java/com/stylemind/product/service/ProductServiceTest.java
git commit -m "fix(product): hide variantless products from public APIs"
```

---

## Task 4: Extract and Test Guided-Flow Frontend Rules

**Files:**
- Create: `FE/src/features/products/admin-product-flow.js`
- Create: `FE/src/features/products/admin-product-flow.test.js`

### Step 1: Write failing pure-function tests

Create:

```javascript
import test from "node:test";
import assert from "node:assert/strict";
import {
  CREATE_PRODUCT_STEPS,
  buildInitialProductPayload,
  canPublishProduct,
  getNextCreateStep,
} from "./admin-product-flow.js";

test("initial product payload always requests INACTIVE", () => {
  assert.equal(
    buildInitialProductPayload({ name: "Shirt", status: "ACTIVE" }).status,
    "INACTIVE",
  );
});

test("successful product creation advances to variants", () => {
  assert.equal(
    getNextCreateStep(CREATE_PRODUCT_STEPS.PRODUCT_INFO),
    CREATE_PRODUCT_STEPS.VARIANTS,
  );
});

test("product cannot publish without variants", () => {
  assert.equal(canPublishProduct({ variants: [] }), false);
  assert.equal(canPublishProduct({}), false);
});

test("first persisted variant enables publishing", () => {
  assert.equal(canPublishProduct({ variants: [{ id: "variant-1" }] }), true);
});
```

### Step 2: Run and confirm failure

Use the frontend's current module mode:

```bash
cd FE
node --test src/features/products/admin-product-flow.test.js
```

Expected: the module does not exist.

### Step 3: Implement the flow helper

Create:

```javascript
export const CREATE_PRODUCT_STEPS = Object.freeze({
  PRODUCT_INFO: "product-info",
  VARIANTS: "variants",
  IMAGES: "images",
});

export function buildInitialProductPayload(formValues) {
  return {
    ...formValues,
    status: "INACTIVE",
  };
}

export function canPublishProduct(product) {
  return Array.isArray(product?.variants) && product.variants.length > 0;
}

export function getNextCreateStep(currentStep) {
  if (currentStep === CREATE_PRODUCT_STEPS.PRODUCT_INFO) {
    return CREATE_PRODUCT_STEPS.VARIANTS;
  }
  if (currentStep === CREATE_PRODUCT_STEPS.VARIANTS) {
    return CREATE_PRODUCT_STEPS.IMAGES;
  }
  return CREATE_PRODUCT_STEPS.IMAGES;
}
```

### Step 4: Re-run tests

```bash
cd FE
node --test src/features/products/admin-product-flow.test.js
```

Expected: four tests pass.

### Step 5: Commit checkpoint

```bash
git add FE/src/features/products/admin-product-flow.js \
  FE/src/features/products/admin-product-flow.test.js
git commit -m "test(admin-products): define guided create flow rules"
```

---

## Task 5: Convert Add Product to the Guided Three-Step Flow

**Files:**
- Modify: `FE/src/pages/admin/ProductManagementPage.jsx`
- Modify, only if a missing API wrapper is confirmed: `FE/src/features/products/admin-product.api.js`

### Step 1: Add explicit create-flow state

Import the helper:

```javascript
import {
  CREATE_PRODUCT_STEPS,
  buildInitialProductPayload,
  canPublishProduct,
  getNextCreateStep,
} from "../../features/products/admin-product-flow";
```

Add state next to the existing drawer/editing state:

```javascript
const [drawerMode, setDrawerMode] = useState("create");
const [createStep, setCreateStep] = useState(
  CREATE_PRODUCT_STEPS.PRODUCT_INFO,
);
const [createdProduct, setCreatedProduct] = useState(null);
const [flowMessage, setFlowMessage] = useState("");
const [isPublishing, setIsPublishing] = useState(false);
```

Derive:

```javascript
const flowProduct = createdProduct ?? editingProduct;
const hasVariants = canPublishProduct(flowProduct);
const isGuidedCreate = drawerMode === "create";
```

### Step 2: Reset create and edit entry points independently

The Add Product action must:

```javascript
setDrawerMode("create");
setCreateStep(CREATE_PRODUCT_STEPS.PRODUCT_INFO);
setCreatedProduct(null);
setEditingProduct(null);
setFlowMessage("");
setIsDrawerOpen(true);
```

The Edit action must:

```javascript
setDrawerMode("edit");
setCreatedProduct(null);
setFlowMessage("");
setEditingProduct(product);
setIsDrawerOpen(true);
```

Keep the existing form hydration for editing.

### Step 3: Split create-step submission from edit submission

For Product Info creation, preserve the existing request contract and force the payload inactive:

```javascript
const created = await adminProductApi.createProduct(
  buildInitialProductPayload(productPayload),
);

setCreatedProduct(created);
setEditingProduct(created);
setCreateStep(getNextCreateStep(CREATE_PRODUCT_STEPS.PRODUCT_INFO));
setFlowMessage(
  "Product has been created as INACTIVE. Add at least one variant before publishing it.",
);
await refetchProducts();
```

Do not close the drawer and do not upload images in Step 1. The backend remains authoritative even though the frontend sends `INACTIVE`.

Retain the current Edit Product update path and current success/error handling.

### Step 4: Reuse persisted variant handlers in Step 2

Render the existing Variants section when:

```javascript
drawerMode === "edit"
  || createStep === CREATE_PRODUCT_STEPS.VARIANTS
  || createStep === CREATE_PRODUCT_STEPS.IMAGES
```

Pass/use `flowProduct.id` for existing create/update/delete variant API calls. After each successful mutation, refresh the current product through the existing admin detail/list data path and update both:

```javascript
setCreatedProduct(refreshedProduct);
setEditingProduct(refreshedProduct);
```

Do not duplicate variant DTOs or API functions.

Place the Step 2 command row after the variant editor:

```jsx
<Button
  type="button"
  disabled={!hasVariants}
  onClick={() => setCreateStep(CREATE_PRODUCT_STEPS.IMAGES)}
>
  Continue to Images
</Button>
```

Show a concise inline validation state while `hasVariants` is false. Preserve backend errors, including duplicate SKU messages.

### Step 5: Move image controls to Step 3 and keep current upload semantics

Render the existing image section for Edit Product and for guided creation only when:

```javascript
createStep === CREATE_PRODUCT_STEPS.IMAGES
```

Continue using the current product-ID-based image endpoint. Keep current image type/size validation, upload progress, previews, replacement, and error handling.

### Step 6: Add Finish Inactive and Publish actions

Add:

```javascript
async function finishCreatedProduct() {
  await refetchProducts();
  closeDrawerWithoutDiscardingProduct();
}

async function publishCreatedProduct() {
  if (!createdProduct || !canPublishProduct(createdProduct)) {
    setError("Add at least one variant before publishing this product.");
    return;
  }

  setIsPublishing(true);
  try {
    await adminProductApi.updateProductStatus(createdProduct.id, "ACTIVE");
    await refetchProducts();
    closeDrawerWithoutDiscardingProduct();
  } catch (error) {
    setError(getApiErrorMessage(error));
  } finally {
    setIsPublishing(false);
  }
}
```

Use the actual existing API wrapper name and error-message helper. Do not add a second Axios instance.

The Step 3 command row, after Images, must contain:

```jsx
<Button type="button" variant="secondary" onClick={finishCreatedProduct}>
  Finish Inactive
</Button>
<Button
  type="button"
  disabled={!hasVariants || isPublishing}
  onClick={publishCreatedProduct}
>
  Publish Active
</Button>
```

### Step 7: Preserve incomplete products when closing

Route drawer close/cancel through one handler:

```javascript
function requestCloseDrawer() {
  if (
    drawerMode === "create"
    && createdProduct
    && !canPublishProduct(createdProduct)
  ) {
    setFlowMessage(
      "This product has no variants and will remain inactive.",
    );
  }

  closeDrawerWithoutDiscardingProduct();
}
```

Use the project's existing confirmation component if one already exists. Do not use a new browser-native prompt when the UI already has a dialog pattern. Whether confirmed or directly closed, never delete or activate the partial product.

### Step 8: Make status behavior clear in Product Info

For Add Product, show status as read-only `INACTIVE` or omit the editable status control and display an `INACTIVE` badge. For Edit Product, preserve the current status control, now backed by the server invariant.

Keep category required and use the existing real category options. Do not add `Uncategorized` as a valid creation choice.

### Step 9: Put command buttons after all content for each mode

Ensure:
- Step 1 commands follow Product Info.
- Step 2 commands follow Variants.
- Step 3 commands follow Images.
- Edit commands follow Product Info, Variants, and Images.

Do not leave a duplicate submit row above Variants or Images.

### Step 10: Run frontend tests and build

```bash
cd FE
node --test src/features/products/admin-product-flow.test.js
npm run build
```

Expected: helper tests pass and Vite production build succeeds.

### Step 11: Commit checkpoint

```bash
git add FE/src/pages/admin/ProductManagementPage.jsx \
  FE/src/features/products/admin-product-flow.js \
  FE/src/features/products/admin-product-flow.test.js \
  FE/src/features/products/admin-product.api.js
git commit -m "feat(admin-products): add guided product creation"
```

Stage `admin-product.api.js` only if it actually changed.

---

## Task 6: Update Product Business and API Documentation

**Files:**
- Modify: `docs/services/product-service.md`
- Modify: `docs/api/01-api-catalog.md`
- Modify: `docs/requirements/01-functional-requirements.md`
- Modify: `docs/product/01-prd.md`
- Modify: `docs/product/02-user-stories.md`
- Modify: `docs/frontend/01-frontend-requirements.md`
- Modify: `docs/delivery/01-roadmap.md`
- Modify: `docs/overview/03-changelog.md`
- Modify: `docs/superpowers/specs/2026-07-06-admin-create-product-with-variants-design.md`
- Preserve: `docs/superpowers/specs/2026-07-06-admin-guided-product-create-design.md`

### Step 1: Update the service contract

Document these exact rules in `docs/services/product-service.md`:

```text
- POST /api/v1/admin/products keeps its existing request and ProductResponse shapes.
- Initial product creation stores the product as INACTIVE, even if ACTIVE is requested.
- Variants are created afterward through the existing product-variant endpoints.
- A product can become ACTIVE only when at least one variant exists.
- Deleting the final variant of an ACTIVE product returns 409 LAST_ACTIVE_VARIANT.
- Public list, detail, and variant reads require ACTIVE status and at least one variant.
- No database migration is required.
```

Include the exact activation conflict and exact final-variant conflict contracts.

### Step 2: Update the API catalog

For:

```text
POST /api/v1/admin/products
```

Clarify that it creates basic product information only, returns the unchanged `ProductResponse`, and stores the product as `INACTIVE`.

For:

```text
PATCH /api/v1/admin/products/{productId}/status
```

Add:

```json
{
  "status": 409,
  "errorCode": "PRODUCT_REQUIRES_VARIANT",
  "message": "Cannot activate a product without variants. Add at least one variant before publishing it."
}
```

For final variant deletion add:

```json
{
  "status": 409,
  "errorCode": "LAST_ACTIVE_VARIANT",
  "message": "Cannot delete the last variant of an active product. Deactivate the product before deleting its final variant."
}
```

Use the repository's actual error envelope field names while preserving these values.

### Step 3: Update product and functional requirements

In the PRD, user stories, and functional requirements, state:

```text
Admin Add Product is a guided workflow:
1. Product Info creates an INACTIVE product.
2. Variants reuses the existing persisted variant APIs.
3. Images / Finish uploads product images and optionally publishes the product.
```

Add acceptance criteria:
- Closing after Step 1 retains an INACTIVE product.
- Publish is unavailable until one persisted variant exists.
- Backend activation without variants returns conflict.
- Final variant deletion from an ACTIVE product returns `LAST_ACTIVE_VARIANT`.
- Public customers cannot see variantless products.

### Step 4: Update frontend requirements and roadmap

Record the guided drawer states, disabled actions, progress/loading/error behavior, and gateway-only API rule. Mark the roadmap item implemented only after tests and browser verification pass.

### Step 5: Update changelog and supersession note

Add a dated `2026-07-06` changelog entry. Ensure the earlier atomic-create-with-variants design remains marked historical/superseded and points to:

```text
docs/superpowers/specs/2026-07-06-admin-guided-product-create-design.md
```

Do not rewrite unrelated historical documentation.

### Step 6: Review documentation terminology

Run:

```bash
rg -n "create.*variants.*same request|atomic.*product.*variant|requires at least one variant" docs
rg -n "LAST_ACTIVE_VARIANT|PRODUCT_REQUIRES_VARIANT|guided.*Product" docs
```

Expected: no active documentation incorrectly claims atomic aggregate creation; the confirmed errors and guided workflow are discoverable.

### Step 7: Commit checkpoint

```bash
git add docs/services/product-service.md \
  docs/api/01-api-catalog.md \
  docs/requirements/01-functional-requirements.md \
  docs/product/01-prd.md \
  docs/product/02-user-stories.md \
  docs/frontend/01-frontend-requirements.md \
  docs/delivery/01-roadmap.md \
  docs/overview/03-changelog.md \
  docs/superpowers/specs/2026-07-06-admin-create-product-with-variants-design.md \
  docs/superpowers/specs/2026-07-06-admin-guided-product-create-design.md
git commit -m "docs(product): record guided creation invariant"
```

Respect pre-existing documentation edits and do not overwrite them.

---

## Task 7: Full Verification and Browser Proof

**Files:**
- Verify all files changed in Tasks 1-6.
- Do not add unrelated production files.

### Step 1: Run backend verification

```bash
cd BE
mvn -pl product-service test
```

Expected: all product-service tests pass.

### Step 2: Run frontend verification

```bash
cd FE
node --test src/features/products/admin-product-flow.test.js
npm run build
npm run lint
```

Expected: tests and build pass. If lint cannot run because the repository has no configured ESLint command/dependency, report that exact limitation instead of claiming lint success.

### Step 3: Verify request-boundary guardrails

```bash
rg -n "/internal/|X-User-Id|X-User-Roles|localhost:808[0-9]" FE/src/features/products FE/src/pages/admin/ProductManagementPage.jsx
```

Expected: no product-management frontend call uses internal routes, manual identity headers, or service ports.

### Step 4: Start or reuse the local application

Use the repository's documented startup command. If the frontend or services are already running, verify their health before starting another process. Keep every started process alive until browser verification is complete.

### Step 5: Verify the guided workflow in the browser

Using the in-app browser:

1. Sign in as ADMIN and open `/admin/products`.
2. Open Add Product and confirm Step 1 is Product Info with `INACTIVE` status.
3. Submit valid basic information and confirm the drawer remains open on Step 2.
4. Confirm the exact inactive guidance message is visible.
5. Confirm Continue/Publish is disabled with zero variants.
6. Close once and confirm the product remains in the admin list as INACTIVE.
7. Reopen/edit as needed, add one variant through the existing API, and confirm continuation is enabled.
8. Continue to Images and verify existing image upload/preview behavior.
9. Publish and verify the product becomes ACTIVE.
10. Attempt to delete its final variant while ACTIVE and verify the UI displays the backend conflict message.
11. Deactivate it, delete the final variant, and verify deletion succeeds.
12. Confirm a variantless product does not appear on public list/detail endpoints.

Capture the browser console and network panel during the flow. Confirm requests target the API Gateway and no unexpected 4xx/5xx errors occur.

### Step 6: Review the final diff

```bash
git status --short
git diff --check
git diff --stat
git diff -- BE/product-service FE/src docs
```

Confirm:
- No database migrations or schema scripts changed.
- `ProductResponse` did not change.
- Create request did not gain a variants field.
- Existing variant endpoints remain.
- No unrelated service changed.
- Exact error code/message contracts are present.

### Step 7: Request code review

Use `superpowers:requesting-code-review` and review:
- activation invariant coverage,
- final-variant race/transaction behavior,
- public visibility query correctness,
- Add/Edit flow separation,
- request-boundary guardrails,
- documentation consistency.

Address any blocking findings and repeat relevant verification.

### Step 8: Completion checkpoint

Use `superpowers:verification-before-completion`. Report:
- changed files,
- backend behavior,
- frontend workflow,
- documentation updates,
- tests and browser checks run,
- any known limitations.

Do not claim completion without fresh command output.
