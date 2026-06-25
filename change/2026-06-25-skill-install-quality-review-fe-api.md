# Changelog — 2026-06-25

## 1. Cài đặt Skill `microservices-architect`

**Nguồn:** `github.com/Jeffallan/claude-skills` (v1.1.0, MIT)

**Files được tạo mới:**

```
~/.claude/skills/microservices-architect/
  SKILL.md
  references/
    communication.md
    data.md
    decomposition.md
    observability.md
    patterns.md

BE/.claude/skills/microservices-architect/   (mirror trong project)
  SKILL.md
  references/ (5 files như trên)
```

**skills-lock.json** — thêm entry mới:
```json
"microservices-architect": {
  "source": "Jeffallan/claude-skills",
  "sourceType": "github",
  "skillPath": "skills/microservices-architect/SKILL.md",
  "computedHash": "d4a22ae7b850c047049209514d2ac2dcd02016472ec9e8f8dfaa51538b495246"
}
```

Skill có thể gọi bằng `/microservices-architect` hoặc `/BE:microservices-architect`.

---

## 2. Review Kiến trúc & Chất lượng Code Backend

**Report đầy đủ:** [`BE/docs/ARCHITECTURE_AND_QUALITY_REVIEW.md`](../BE/docs/ARCHITECTURE_AND_QUALITY_REVIEW.md)

Report được viết theo hướng **teaching document** — giải thích kiến trúc từ đầu (system diagram, per-service map, auth model, request walkthroughs) rồi mới layer findings chất lượng lên trên.

### Tóm tắt hệ thống

9 Spring Boot services + common-lib, một PostgreSQL instance với 8 schema riêng (database-per-service). Gateway :3001 → route đến từng service. Auth stateless JWT (gateway validate, inject `X-User-Id`). Service-to-service dùng Feign + `X-Internal-Token`.

```
API Gateway :3001
├─ auth-service    :8081  →  auth_db
├─ user-service    :8082  →  user_db
├─ product-service :8083  →  product_db
├─ cart-service    :8086  →  cart_db
├─ order-service   :8087  →  order_db  → (Feign) cart, product, payment
├─ payment-service :8088  →  payment_db
├─ notification-service :8089 → notification_db
└─ ai-agent-service :8085 → ai_db      → (Feign) product, order
```

### Findings được verify

| # | Mức độ | Vấn đề | File:line | Trạng thái |
|---|---|---|---|---|
| H1 | 🔴 High | JWT secret default khác nhau giữa services → token auth-service không validate được ở payment/notification | `payment-service/application.yml:36`, `notification-service/application.yml:33` | ✅ Đã fix |
| H2 | 🔴 High | Cart không được clear sau checkout — `mergeCart("")` là no-op (tìm `guest_` cart không tồn tại, return early) | `OrderService.java:88`, `CartService.java:119` | ✅ Annotated + warning log; cần endpoint `DELETE /api/cart` |
| H3 | 🔴 High | Saga không có compensation thật / không reserve inventory | `OrderService.java:67-90` | 📋 Recommended |
| M1 | 🟠 Medium | Feign không có timeout/circuit-breaker → downstream chậm làm treo order-service | Tất cả `*/feign/*.java` | ✅ Đã add timeouts |
| M2 | 🟠 Medium | Default value của secrets commit vào repo yếu/guessable | Nhiều `application.yml` | 📋 Recommended |
| M3 | 🟠 Medium | Giá lấy từ cart DTO (stale), không từ product-service authoritative | `OrderService.java:109` | 📋 Recommended |
| L1 | 🟡 Low | Dead code: `getVariantSku` không được gọi ở đâu | `OrderService.java:104` | ✅ Đã xóa |
| L2 | 🟡 Low | Unused imports | `OrderService.java` | ✅ Đã xóa |

---

## 3. Fixes An Toàn Đã Áp Dụng (Safe Fixes)

### 3.1 Fix H1 — JWT secret mismatch

**Vấn đề:** `payment-service` và `notification-service` dùng default `sm-secret-key-2026`, còn các service khác dùng `super-secure-stylemind-secret-key-signature-2026-xyz`. Với HS256, token sinh bởi auth-service sẽ fail validation → 401 khi chưa set env var.

**Files thay đổi:**

`BE/payment-service/src/main/resources/application.yml`
```yaml
# Trước:
  secret: ${JWT_SECRET:sm-secret-key-2026}
# Sau:
  secret: ${JWT_SECRET:super-secure-stylemind-secret-key-signature-2026-xyz}
```

`BE/notification-service/src/main/resources/application.yml`
```yaml
# Trước:
  secret: ${JWT_SECRET:sm-secret-key-2026}
# Sau:
  secret: ${JWT_SECRET:super-secure-stylemind-secret-key-signature-2026-xyz}
```

### 3.2 Fix H2 (partial) — Annotate cart-clear no-op

**File thay đổi:** `BE/order-service/src/main/java/com/stylemind/order/service/OrderService.java`

Xóa lời gọi `cartClient.mergeCart(authHeader, {guestSessionId: ""})` (no-op gây hiểu nhầm) → thay bằng `log.warn` và FIXME comment giải thích rõ bug. Cart vẫn chưa được clear; cần thêm `DELETE /api/cart` endpoint.

### 3.3 Fix M1 — Thêm Feign timeouts

**Files thay đổi:**

`BE/order-service/src/main/resources/application.yml` — thêm:
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 3000
        readTimeout: 5000
```

`BE/ai-agent-service/src/main/resources/application.yml` — thêm (tương tự).

### 3.4 Fix L1/L2 — Dead code và unused imports

**File thay đổi:** `BE/order-service/src/main/java/com/stylemind/order/service/OrderService.java`
- Xóa method `getVariantSku()` (không có caller)
- Xóa `import java.time.Instant` (unused)
- Xóa `import com.stylemind.cart.dto.CartMergeRequest` (unused sau khi remove no-op call)
- Giữ `productClient` field (dùng cho fix M3 sau này)

**Verification:** `mvn -pl order-service -am clean compile` → **BUILD SUCCESS** ✓

---

## 4. Nối API Frontend ↔ Backend

### Tổng quan

Nối toàn bộ core flow: auth → products → cart → checkout/order/payment. AI stylist, admin analytics, notifications giữ mock có chủ đích cho phase sau.

Base URL: `http://localhost:3001/api` (qua API Gateway)

### 4.1 Foundation

**`FE/src/services/apiClient.js`**
- `axios` instance với `Authorization: Bearer <token>` auto-inject
- Response interceptor unwrap `ApiResponse<T>` → trả về `.data`
- Error normalize: `{ message, errorCode, status }` để UI hiển thị được
- 401 → tự clear session, redirect `/login`
- Helpers: `getAuthToken`, `getStoredUser`, `setAuthSession`, `clearAuthSession`, `getGuestSessionId`

**`FE/src/services/endpoints.js`**
- Tất cả endpoints trỏ về `VITE_API_GATEWAY || VITE_API_BASE_URL || http://localhost:3001/api`

### 4.2 Auth Slice

**`FE/src/features/auth/auth.api.js`**
- `loginUser(email, password)` → `POST /api/auth/login`
- `registerUser({ name, email, password })` → `POST /api/auth/register`
- `getCurrentUser()` → `GET /api/auth/me`
- `logoutUser()` → `POST /api/auth/logout` + clear session
- `mapUser()`: normalize `fullName/name`, lowercase role

**`FE/src/features/auth/auth.store.js`** (Zustand)
- Hydrate từ localStorage khi app load
- Actions: `login(session)`, `logout()`, `setUser(user)`, `setLoading(bool)`

**UI:** `LoginPage.jsx`, `RegisterPage.jsx` — wired đầy đủ, có loading state, disabled button khi đang submit, hiển thị lỗi từ backend.

### 4.3 Product Slice

**`FE/src/features/products/product.api.js`**
- `getProducts(filters)` → `GET /api/products?page&size&search&category&minPrice&maxPrice&sort`
- `getProductById(id)` → `GET /api/products/{id}`
- `getCategories()` → `GET /api/categories`
- `mapProduct()` adapter: map `basePrice` → `price`, `images[]` → `images[primaryUrl]`, `variants` → `colors/sizes/material/availableVariantId`, thêm `aiMatchScore` (deterministic từ id), `isNew` (30 ngày)

**UI:**
- `HomePage.jsx` — skeleton loading, error banner, empty state
- `ProductCatalogPage.jsx` — real categories từ backend, filters qua API, pagination client-side, loading/error/empty states
- `ProductDetailPage.jsx` — `hasVariant` check → disabled "Add to Bag" nếu không có variant

**`FE/src/components/customer/ProductFilter.jsx`** — dùng categories thật từ backend nếu có, fallback constants nếu chưa có

### 4.4 Cart Slice

**`FE/src/features/cart/cart.api.js`**
- Tự động gắn `X-Guest-Session-Id` header (guest UUID từ localStorage)
- `getCart()` → `GET /api/cart`
- `addToCart({ variantId, quantity })` → `POST /api/cart`
- `updateCartItem(itemId, quantity)` → `PUT /api/cart/{itemId}?quantity=...`
- `removeCartItem(id)` → `DELETE /api/cart/{id}`
- `mergeCart(guestSessionId)` → `POST /api/cart/merge`
- `mapCartItem()`: map `variant.product` → `name/price/images/size/color/material`

**`FE/src/features/cart/cart.store.js`** (Zustand)
- `loadCart()` — async, load từ backend
- `addItem(product, qty, size, color)` — resolve variantId từ `product.variants`, call API
- `removeItem(cartItemId)` — optimistic delete
- `updateQuantity(cartItemId, qty)` — optimistic update
- `clearCart()` — xóa từng item qua API (không có bulk endpoint)

**`FE/src/hooks/useCart.js`** — expose `items, addItem, removeItem, updateQuantity, clearCart, loadCart, itemCount, subtotal, loading, error`

**`FE/src/layouts/CustomerLayout.jsx`** — gọi `loadCart()` khi mount → sync cart count trên nav

**UI:** `CartPage.jsx` — loading/error/empty states

**`FE/src/components/customer/ProductCard.jsx`** — "Add to Bag" disabled nếu `availableVariantId` null

### 4.5 Order & Payment Slice

**`FE/src/features/orders/order.api.js`**
- `createOrder({ shippingAddress, paymentMethod, transactionId? })` → `POST /api/orders`
- `getOrders()` → `GET /api/orders`
- `getOrderById(id)` → `GET /api/orders/{id}`
- `mapOrder()`: normalize `orderStatus` → `status` (`FULFILLED`→`delivered`, `CANCELLED`→`cancelled`), build `timeline` từ status steps

**`FE/src/features/payment/payment.api.js`** *(file mới)*
- `checkoutPayment(payload)` → `POST /api/payment/checkout`
- `processPayment(payload)` → `POST /api/payment/process`

**`FE/src/features/payment/payment.store.js`** (Zustand)
- `processPayment(orderData)` — xử lý cả `cod` và `online_simulated`:
  - **COD:** `createOrder({ shippingAddress, paymentMethod: 'cod' })`
  - **online_simulated:** sinh `crypto.randomUUID()` làm `transactionId`, gọi `createOrder({ shippingAddress, paymentMethod: 'online_simulated', transactionId })`
- Steps UI: creating → payment → confirming
- `lastOrder` lưu order thật từ backend → hiển thị order ID thật trong success screen

**`FE/src/pages/customer/CheckoutPage.jsx`**
- Thay hardcoded mock addresses bằng `<textarea>` nhập địa chỉ tự do
- Validate địa chỉ trống trước khi submit
- Hiển thị `lastOrder?.id` thật sau khi đặt hàng thành công

**`FE/src/pages/customer/OrderTrackingPage.jsx`**
- Load real orders từ `GET /api/orders`
- Loading/error/empty states
- Hiển thị timeline từ backend order status

### 4.6 Env

**`FE/.env.example`**
```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_API_GATEWAY=http://localhost:3001/api
VITE_APP_NAME=StyleMind
```

### 4.7 Build Verification

```
npm run build  →  ✓ built in 1.64s  (no errors)
```

---

## 5. Những gì KHÔNG thay đổi (giữ mock có chủ đích)

| Tính năng | Lý do giữ mock |
|---|---|
| AI Stylist chat | Backend contract chưa ổn định, vector search chưa implement |
| Admin analytics | Ngoài scope phase này |
| Notifications | Notification service chỉ là stub, không có realtime |
| Style profile nâng cao | Ngoài scope core flow |

---

## 6. Việc cần làm tiếp theo (Recommended)

1. **H2 — Clear cart sau checkout:** Thêm `DELETE /api/cart` endpoint vào cart-service và gọi trong order-service sau khi order `FULFILLED`.
2. **H3 — Saga compensation:** Model saga steps rõ ràng với execute/compensate cho order flow.
3. **M1 — Circuit breaker:** Thêm `spring-cloud-starter-circuitbreaker-resilience4j` + fallback cho Feign clients của order-service.
4. **M3 — Authoritative pricing:** Fetch giá từ product-service khi tạo order thay vì tin giá từ cart DTO.
5. **Chunk splitting:** FE bundle 892KB — cân nhắc `React.lazy` / `Suspense` cho các pages lớn.
