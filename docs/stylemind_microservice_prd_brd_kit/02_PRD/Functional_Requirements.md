# Functional Requirements — StyleMind

## 1. Authentication

| ID | Requirement | Priority |
|---|---|---|
| AUTH-01 | User đăng ký bằng email/password | Must |
| AUTH-02 | User đăng nhập | Must |
| AUTH-03 | System cấp JWT | Must |
| AUTH-04 | Frontend gọi `/api/auth/me` | Must |
| AUTH-05 | Admin API yêu cầu role `ADMIN` | Must |

## 2. User Profile

| ID | Requirement | Priority |
|---|---|---|
| USER-01 | Customer xem profile | Must |
| USER-02 | Customer cập nhật profile | Must |
| USER-03 | Customer quản lý địa chỉ | Must |
| USER-04 | Customer quản lý style profile | Should |

## 3. Product Catalog

| ID | Requirement | Priority |
|---|---|---|
| PROD-01 | Xem product listing | Must |
| PROD-02 | Xem product detail | Must |
| PROD-03 | Filter theo category/price/keyword | Must |
| PROD-04 | Sort sản phẩm | Should |
| PROD-05 | Pagination | Must |
| PROD-06 | Admin CRUD product | Must |
| PROD-07 | Admin CRUD category | Must |

## 4. Cart

| ID | Requirement | Priority |
|---|---|---|
| CART-01 | Guest thêm item vào cart | Must |
| CART-02 | Customer thêm item vào cart | Must |
| CART-03 | Update quantity | Must |
| CART-04 | Remove item | Must |
| CART-05 | Merge guest cart sau login | Must |
| CART-06 | Clear cart sau checkout | Must |

## 5. Order

| ID | Requirement | Priority |
|---|---|---|
| ORDER-01 | Customer tạo order từ cart | Must |
| ORDER-02 | Customer xem order list | Must |
| ORDER-03 | Customer xem order detail | Must |
| ORDER-04 | Order service lấy giá từ product-service | Must |
| ORDER-05 | Clear cart sau order success | Must |
| ORDER-06 | Saga compensation | Should |

## 6. AI Stylist

| ID | Requirement | Priority |
|---|---|---|
| AI-01 | Customer chat với AI stylist | Must |
| AI-02 | Lưu chat history | Must |
| AI-03 | Gợi ý product/outfit | Must |
| AI-04 | Admin quản lý AI index jobs | Must |
| AI-05 | Qdrant vector search | Phase 2 |
| AI-06 | Neo4j graph reasoning | Phase 2 |
