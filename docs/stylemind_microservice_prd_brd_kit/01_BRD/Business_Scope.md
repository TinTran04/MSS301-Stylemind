# Business Scope — StyleMind

## 1. In Scope

| Area | Capability |
|---|---|
| Customer shopping | Product browsing, product details, cart, checkout, order tracking |
| Authentication | Register, login, JWT authentication |
| User profile | Customer profile, style profile, delivery addresses |
| Product catalog | Categories, products, product variants, product images |
| Cart | Guest cart, authenticated cart, merge cart after login |
| Order | Order creation and checkout flow |
| Payment | COD and simulated online payment |
| Notification | Notification log/stub |
| AI stylist | AI chat, outfit/product recommendation, AI bundles, index jobs |
| Admin | Product/category management, AI index job management, notification management |

## 2. Out of Scope for MVP

| Ngoài phạm vi | Ghi chú |
|---|---|
| Full inventory tracking | Không tracking stock quantity thật trong MVP |
| Inventory reservation | Không reserve inventory trong checkout saga |
| Payment gateway thật | Chỉ COD và simulated online payment |
| Email/SMS delivery thật | Notification mới ở mức log/stub |
| Full AI vector/graph reasoning | Đưa sang Phase 2 |

## 3. MVP Boundary

MVP tập trung vào:

```text
Auth → Product browsing → Cart → Checkout → Payment simulation → Order tracking
```

AI stylist có thể hoạt động bằng mock/partial response trong MVP, nhưng kiến trúc phải chuẩn bị cho retrieval thật.
