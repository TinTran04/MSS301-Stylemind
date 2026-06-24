# MSS301 - StyleMind

**StyleMind** là nền tảng thương mại điện tử thời trang tích hợp **AI Stylist**, được xây dựng với **ReactJS** cho Frontend và **Spring Boot Microservices** cho Backend.

Hệ thống hỗ trợ khách hàng mua sắm sản phẩm thời trang, tạo hồ sơ phong cách cá nhân, trò chuyện với AI để nhận gợi ý phối đồ, thêm sản phẩm vào giỏ hàng, đặt hàng và theo dõi đơn hàng. Đồng thời, Admin có thể quản lý sản phẩm, đơn hàng, khách hàng, pipeline AI, Knowledge Graph và các chỉ số phân tích hiệu quả gợi ý của AI.

Mục tiêu chính của hệ thống là cung cấp trải nghiệm **tư vấn thời trang cá nhân hóa**, đồng thời đảm bảo AI chỉ gợi ý các sản phẩm **thật sự tồn tại và còn hàng trong hệ thống** (chống ảo giác).

**📋 Source of Truth:** Mô hình dữ liệu chuẩn được định nghĩa trong **[DATA_MODEL_DOCUMENTATION](docs/DATA_MODEL_DOCUMENTATION)** - tất cả tài liệu khác phải tuân thủ tài liệu này.

---

## Tính năng chính

### Khách hàng

* Đăng ký và đăng nhập (`auth-service`: users, JWT, provider LOCAL/GOOGLE/FACEBOOK)
* Tạo và cập nhật hồ sơ phong cách cá nhân (`user-service`: customer_style_profiles, delivery_addresses)
* Xem danh mục sản phẩm thời trang (cây danh mục đệ quy: `categories` với parent_id, slug)
* Tìm kiếm, lọc và sắp xếp sản phẩm
* Xem chi tiết sản phẩm (variants: sku, size, color, material, price_override)
* Chat với AI Stylist để được tư vấn phối đồ (`ai-agent-service`: chat_sessions, chat_messages)
* Nhận gợi ý outfit từ các sản phẩm còn hàng (`ai_curated_bundles`, `ai_curated_bundle_items`)
* Thêm từng sản phẩm hoặc toàn bộ outfit AI đề xuất vào giỏ hàng (`cart_items`: is_ai_recommended, source_bundle_id)
* Quản lý giỏ hàng (`shopping_carts`, `cart_items` với variant_id FK)
* Checkout và thanh toán giả lập (`payment-service`: transactions)
* Tạo đơn hàng với snapshot giá (`orders`: order_status PENDING/PROCESSING/COMPENSATING_ROLLBACK/FULFILLED/CANCELLED; `order_items`: price_at_purchase, is_ai_conversion, source_bundle_id)
* Theo dõi trạng thái đơn hàng
* Xem lịch sử mua hàng

### Admin / Chủ cửa hàng

* Xem dashboard tổng quan
* Quản lý sản phẩm (CRUD products, variants, images, categories tree)
* Quản lý biến thể sản phẩm như size, màu sắc, SKU, material, price_override
* Quản lý đơn hàng (status: PENDING, PROCESSING, COMPENSATING_ROLLBACK, FULFILLED, CANCELLED)
* Quản lý khách hàng (xem Style Profile, Addresses)
* Theo dõi trạng thái đồng bộ AI Pipeline (`ai_index_jobs`)
* Quản lý Knowledge Graph / luật thời trang (Neo4j nodes & relationships)
* Xem báo cáo phân tích hiệu quả gợi ý của AI (`ai_analytics_logs` funnel)
* Cấu hình hệ thống ở mức giao diện quản trị

---

## Kiến trúc hệ thống

Dự án được tổ chức theo hướng:

```text
Frontend ReactJS
        ↓
API Gateway (Port 3001)
        ↓
Spring Boot Microservices (8 services + 8 DBs)
        ↓
Database / Message Broker / AI Services (PostgreSQL, Qdrant, Neo4j, MinIO, Redis)
```

Frontend chỉ giao tiếp với **API Gateway**, không gọi trực tiếp từng service backend.

---

## Công nghệ sử dụng

### Frontend

* ReactJS
* Vite
* React Router
* Zustand
* Axios
* Tailwind CSS / CSS
* Recharts
* Lucide React
* Framer Motion

### Backend

* Spring Boot 3.x
* Spring Cloud Gateway
* Spring Security (JWT, RBAC)
* Spring Data JPA
* PostgreSQL (8 databases: auth_db, user_db, product_db, cart_db, order_db, payment_db, ai_db, notification_db)
* Eureka Discovery Service (optional)
* REST API (OpenFeign for service-to-service)
* Docker

### AI / Recommendation

* AI Stylist Service (`ai-agent-service`)
* Vector Search (Qdrant)
* Knowledge Graph (Neo4j)
* Recommendation System (Hybrid Search: Vector + Keyword + Graph + Personalization)
* Runtime API Fetching (Function Calling) - Anti-Hallucination

---

## Cấu trúc thư mục

```text
MSS301-Stylemind/
├── FE/
│   └── ReactJS + Vite Frontend
│
├── BE/
│   └── Spring Boot Microservices Backend
│
├── docs/
│   ├── DATA_MODEL_DOCUMENTATION    # 📋 SOURCE OF TRUTH - Data model chuẩn
│   ├── MICROSERVICE_ARCHITECTURE.md # Kiến trúc chi tiết + Schema DB
│   ├── API_CONTRACT.md             # API spec toàn hệ thống
│   ├── DEPLOYMENT_GUIDE.md         # Hướng dẫn deploy local & Docker
│   ├── MIGRATION_ROADMAP.md        # Lộ trình 7 bước chuyển đổi
│   ├── PROJECT_ANALYSIS.md         # Phân tích hiện trạng FE
│   └── README.md                   # File này
│
├── .gitignore
└── README.md
```

### Frontend

```text
FE/
├── public/
├── src/
│   ├── app/
│   ├── layouts/
│   ├── pages/
│   ├── components/
│   ├── features/
│   ├── services/
│   ├── hooks/
│   ├── utils/
│   └── data/
├── package.json
├── vite.config.js
└── index.html
```

### Backend (8 Microservices)

```text
BE/
├── api-gateway/           # Port 3001 - Entry point
├── auth-service/          # Port 8081 - auth_db (users)
├── user-service/          # Port 8082 - user_db (customer_style_profiles, delivery_addresses)
├── product-service/       # Port 8083 - product_db (categories, products, variants, images)
├── cart-service/          # Port 8086 - cart_db (shopping_carts, cart_items)
├── order-service/         # Port 8087 - order_db (orders, order_items)
├── payment-service/       # Port 8088 - payment_db (transactions)
├── ai-agent-service/      # Port 8085 - ai_db + Qdrant + Neo4j (chat_sessions, messages, bundles, analytics, index_jobs)
├── notification-service/  # Port 8089 - notification_db (notification_logs)
└── common-lib/            # Shared DTOs, exceptions, configs
```

---

## Các service backend & Data Ownership (theo DATA_MODEL_DOCUMENTATION)

| Service | Database | Tables Owned | Trách nhiệm chính |
| :--- | :--- | :--- | :--- |
| `auth-service` | `auth_db` | `users` | Đăng ký, đăng nhập, JWT, roles (CUSTOMER/ADMIN), provider SSO |
| `user-service` | `user_db` | `customer_style_profiles`, `delivery_addresses` | Hồ sơ sinh trắc học, gu thẩm mỹ (style_personas JSONB), sổ địa chỉ |
| `product-service` | `product_db` | `categories`, `products`, `product_variants`, `product_images` | Danh mục cây, sản phẩm (base_price, aesthetic_style, target_demographic, seasonal_property), biến thể (sku, size, color, material, price_override), ảnh |
| `cart-service` | `cart_db` | `shopping_carts`, `cart_items` | Giỏ hàng (Guest/User), tracking AI (is_ai_recommended, source_bundle_id) |
| `order-service` | `order_db` | `orders`, `order_items` | Đơn hàng (Saga), snapshot giá (price_at_purchase), AI conversion tracking |
| `payment-service` | `payment_db` | `transactions` | Thanh toán online/COD, refund |
| `ai-agent-service` | `ai_db` + Qdrant + Neo4j | `chat_sessions`, `chat_messages`, `ai_curated_bundles`, `ai_curated_bundle_items`, `ai_analytics_logs`, `ai_index_jobs` | Chat log, AI bundles, analytics, index jobs. **KHÔNG sở hữu dữ liệu gốc sản phẩm/khách hàng.** |
| `notification-service` | `notification_db` | `notification_logs` | Email, SMS, push notifications |

---

## Luồng người dùng chính

```text
Khách hàng truy cập hệ thống
→ Đăng ký / đăng nhập
→ Tạo Style Profile (body_morphology, preferred_fit, style_personas...)
→ Duyệt sản phẩm hoặc chat với AI Stylist
→ Nhận gợi ý outfit từ sản phẩm còn hàng (inventory-aware)
→ Thêm sản phẩm vào giỏ hàng (variant_id, is_ai_recommended)
→ Checkout (shipping_address snapshot)
→ Thanh toán giả lập
→ Tạo đơn hàng (order_status: PENDING → PROCESSING → FULFILLED)
→ Theo dõi trạng thái đơn hàng
```

---

## Luồng Admin chính

```text
Admin đăng nhập (role=ADMIN)
→ Vào Dashboard
→ Quản lý sản phẩm (categories tree, products, variants, images)
→ Quản lý đơn hàng (status: PENDING/PROCESSING/COMPENSATING_ROLLBACK/FULFILLED/CANCELLED)
→ Theo dõi AI Pipeline (ai_index_jobs: PENDING/PROCESSING/COMPLETED/FAILED)
→ Quản lý Knowledge Graph (Neo4j: Product, Category, Color, Material, Style, Occasion, Season, BodyType, Outfit)
→ Xem Recommendation Analytics (ai_analytics_logs funnel: IMPRESSION → CLICK → ADD_TO_CART)
```

---

## AI Stylist (Chống ảo giác - Anti-Hallucination)

AI Stylist là chức năng nổi bật của hệ thống. Người dùng có thể nhập yêu cầu tự nhiên:

```text
Tôi cần một outfit lịch sự nhưng thoáng mát để đi phỏng vấn mùa hè.
```

Hệ thống xử lý dựa trên **Runtime API Fetching (Function Calling)**:

```text
Style Profile (user_db)
+ Product Metadata (product_db)
+ Vector Search (Qdrant)
+ Knowledge Graph Rules (Neo4j)
+ Recommendation Signal
```

**Nguyên tắc cốt lõi:** AI **KHÔNG** được tự tạo ra giá/tồn kho. Mọi thông tin động đều được lấy realtime qua Internal API:
- `GET /internal/products/:id` → giá chính xác
- `GET /internal/orders/:id` → trạng thái đơn (kèm ownership check)

Kết quả trả về:
* Lời tư vấn phối đồ (`chat_messages.message_text`)
* Danh sách sản phẩm phù hợp (`recommended_products` với match_score)
* Product cards trong chat (`has_product_block: true`)
* Lý do vì sao outfit phù hợp (`ai_curated_bundles.justification_summary`)
* Nút thêm từng sản phẩm hoặc toàn bộ outfit vào giỏ hàng (`cart_items.is_ai_recommended`, `source_bundle_id`)

---

## Saga / Checkout Flow

Hệ thống có thiết kế luồng checkout theo hướng xử lý giao dịch phân tán (Saga Orchestrator-based):

Ví dụ luồng thành công:
```text
Tạo đơn hàng (order_status: PENDING)
→ Xử lý thanh toán (transactions: COMPLETED)
→ Xác nhận đơn hàng (order_status: FULFILLED)
```

Nếu thanh toán thất bại:
```text
Tạo đơn hàng (order_status: PENDING)
→ Thanh toán thất bại (transactions: FAILED)
→ Rollback: Hủy đơn hàng (order_status: CANCELLED / COMPENSATING_ROLLBACK)
```

---

## Cài đặt và chạy Frontend

Di chuyển vào thư mục FE:

```bash
cd FE
```

Cài đặt dependencies:

```bash
npm install
```

Chạy project:

```bash
npm run dev
```

Build project:

```bash
npm run build
```

---

## Cấu hình môi trường Frontend

Tạo file `.env` trong thư mục `FE/`:

```env
VITE_API_BASE_URL=http://localhost:3001/api
VITE_APP_NAME=StyleMind
```

Lưu ý:
```text
Không commit file .env lên GitHub.
Chỉ commit file .env.example nếu cần.
```

---

## Chạy Backend

Backend sẽ được phát triển bằng Spring Boot Microservices (8 services).

Dự kiến chạy local bằng Docker Compose:

```bash
cd BE
docker compose up
```

Hoặc chạy từng service Spring Boot riêng tùy giai đoạn phát triển.

Xem chi tiết tại [DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md) và [MICROSERVICE_ARCHITECTURE.md](docs/MICROSERVICE_ARCHITECTURE.md).

---

## Test Swagger / OpenAPI 3

Sau khi backend đã chạy bằng Docker Compose, kiểm tra nhanh gateway và các service:

```bash
curl http://localhost:3001/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8088/actuator/health
```

Swagger UI của từng service có thể mở trực tiếp trên trình duyệt:

| Service | Swagger UI | OpenAPI JSON |
| :--- | :--- | :--- |
| API Gateway | http://localhost:3001/swagger-ui.html | http://localhost:3001/v3/api-docs |
| Auth Service | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| User Service | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Product Service | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| AI Agent Service | http://localhost:8085/swagger-ui.html | http://localhost:8085/v3/api-docs |
| Cart Service | http://localhost:8086/swagger-ui.html | http://localhost:8086/v3/api-docs |
| Order Service | http://localhost:8087/swagger-ui.html | http://localhost:8087/v3/api-docs |
| Payment Service | http://localhost:8088/swagger-ui.html | http://localhost:8088/v3/api-docs |
| Notification Service | http://localhost:8089/swagger-ui.html | http://localhost:8089/v3/api-docs |

Một số API public có thể test ngay không cần token:

```bash
curl http://localhost:3001/api/products
curl http://localhost:3001/api/categories
curl http://localhost:3001/api/cart
```

Với API cần đăng nhập, tạo một user test rồi đăng nhập qua gateway để lấy JWT:

```bash
curl -X POST http://localhost:3001/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Swagger Tester","email":"swagger-tester@example.com","password":"swagger123"}'

curl -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"swagger-tester@example.com","password":"swagger123"}'
```

Nếu email test đã tồn tại trong database local, đổi sang email khác rồi chạy lại lệnh `register`.

Sau đó vào Swagger UI của service cần test, bấm **Authorize**, nhập:

```text
Bearer <accessToken>
```

Lưu ý khi test:

* Frontend và client nên gọi qua API Gateway `http://localhost:3001/api/...`.
* Swagger UI của từng service dùng để inspect/test nhanh API nội bộ trong môi trường local.
* Các endpoint admin cần JWT của user có role `ADMIN`.
* Nếu Docker Dashboard hiển thị log cũ, kiểm tra trạng thái thật bằng `docker compose -f BE/docker-compose.yml ps -a`.
* Nếu endpoint cần quyền trả `401` hoặc `403`, kiểm tra lại token JWT và role của user.

---

## Branch workflow

Dự án sử dụng workflow cơ bản:

```text
main      → bản ổn định để deploy
develop   → bản đang phát triển và test
feature/* → nhánh phát triển chức năng
fix/*     → nhánh sửa lỗi
```

Ví dụ:

```bash
git checkout develop
git checkout -b feature/frontend-ai-stylist
```

Commit message nên viết rõ ràng:

```text
feat: add AI stylist chat page
feat: implement product catalog filters
fix: update cart quantity logic
docs: update project README
chore: initialize FE and BE structure
```

---

## Deploy dự kiến

### Frontend

Frontend có thể deploy lên:

* Netlify
* Vercel

Cấu hình deploy:

```text
Root directory: FE
Build command: npm run build
Publish directory: dist
```

### Backend

Backend có thể deploy lên:

* Render
* Railway
* VPS với Docker Compose

Frontend sẽ gọi backend thông qua API Gateway:

```env
VITE_API_BASE_URL=https://your-api-gateway-url/api
```

---

## Trạng thái hiện tại

* [x] Khởi tạo repository
* [x] Tạo cấu trúc FE và BE
* [x] Thêm ReactJS frontend
* [x] **DATA_MODEL_DOCUMENTATION** - Complete (Source of Truth)
* [x] **MICROSERVICE_ARCHITECTURE.md** - Aligned with Data Model
* [x] **API_CONTRACT.md** - Aligned with Data Model
* [x] **DEPLOYMENT_GUIDE.md** - Aligned with Data Model
* [x] **MIGRATION_ROADMAP.md** - Aligned with Data Model
* [ ] Xây dựng Spring Boot backend services (8 services)
* [ ] Kết nối Frontend với Backend API
* [ ] Tích hợp AI Stylist Service (Qdrant + Neo4j + Function Calling)
* [ ] Hoàn thiện deploy

---

## Thành viên nhóm

> Cập nhật tên thành viên nhóm tại đây.

```text
1. ...
2. ...
3. ...
4. ...
```

---

## Ghi chú

Dự án được phát triển phục vụ học tập và nghiên cứu kiến trúc **Microservices**, kết hợp với ứng dụng **AI trong thương mại điện tử thời trang**.

Mục tiêu không chỉ là xây dựng một website bán hàng, mà còn là mô phỏng một hệ thống có khả năng tư vấn thời trang cá nhân hóa, quản lý thông minh và phân tích hiệu quả gợi ý của AI.

**Tài liệu tham khảo chính:**
* [DATA_MODEL_DOCUMENTATION](docs/DATA_MODEL_DOCUMENTATION) - Single Source of Truth cho data model
* [MICROSERVICE_ARCHITECTURE.md](docs/MICROSERVICE_ARCHITECTURE.md) - Kiến trúc, DB schema, State machines, API Gateway, AI design
* [API_CONTRACT.md](docs/API_CONTRACT.md) - Chi tiết request/response cho tất cả API
* [DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md) - Hướng dẫn chạy local & Docker
* [MIGRATION_ROADMAP.md](docs/MIGRATION_ROADMAP.md) - Lộ trình 7 bước từ Mock → Production
