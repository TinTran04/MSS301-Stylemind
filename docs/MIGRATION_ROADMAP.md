# LỘ TRÌNH CHUYỂN ĐỔI HỆ THỐNG (MIGRATION ROADMAP)

Tài liệu này vạch ra lộ trình 7 bước chuyển đổi từ hệ thống UI/Frontend tĩnh hiện tại sang hệ thống Microservices chạy thực tế sử dụng Spring Boot và AI Agent. **Tất cả các bước tuân thủ DATA_MODEL_DOCUMENTATION.**

---

## 1. Lộ trình chuyển đổi 7 bước chi tiết

```text
+-----------------------------------------------------------------------------------+
|  BƯỚC 1: Chạy UI Mock API (Hiện trạng)                                            |
|  - Chạy frontend React/Vite, toàn bộ logic dữ liệu nằm local ở các file *.api.js   |
+---------------------------------------+-------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|  BƯỚC 2: Xây dựng API Gateway                                                     |
|  - Tạo Spring Cloud Gateway. Định cấu hình định tuyến cho các tài nguyên.          |
|  - Thiết lập rule xóa (strip) header X-User-Id và X-User-Roles để bảo mật.         |
+---------------------------------------+-------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|  BƯỚC 3: Tạo auth-service và user-service                                         |
|  - Xây dựng database auth_db & user_db theo DATA_MODEL_DOCUMENTATION.              |
|  - Đăng ký, đăng nhập bcrypt, phát hành JWT (users: id, email, password_hash,     |
|    full_name, provider, provider_id, role).                                      |
|  - Gateway giải mã JWT, truyền định danh qua header. Phân quyền ROLE_CUSTOMER/ADMIN|
|  - Quản lý Style Profile (customer_style_profiles: gender, age, height_cm,        |
|    weight_kg, body_morphology, preferred_fit, style_personas JSONB) và           |
|    delivery_addresses phục vụ cá nhân hóa cho AI.                                 |
+---------------------------------------+-------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
||  BƯỚC 4: Tạo product-service                                                                      |
||  - Quản lý CRUD sản phẩm (products, product_variants, product_images, categories)|
||    theo DATA_MODEL_DOCUMENTATION: base_price, aesthetic_style, target_demographic,|
||    seasonal_property, status; variants: sku, size, color, material, price_override|
||  - Thiết lập MinIO để lưu trữ ảnh, lưu URL vào product_db.                        |
+---------------------------------------+-------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|  BƯỚC 5: Tạo cart-service, order-service và payment-service                       |
|  - Thiết lập Guest Cart (guestSessionId) và cơ chế Merge Cart sau đăng nhập.      |
|    cart_items: variant_id, quantity, is_ai_recommended, source_bundle_id          |
|  - Xây dựng luồng tạo đơn hàng (orders: order_status PENDING/PROCESSING/          |
|    COMPENSATING_ROLLBACK/FULFILLED/CANCELLED), thanh toán giả lập, quản lý        |
|    trường price_at_purchase, is_ai_conversion, source_bundle_id.                  |
+---------------------------------------+-------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|  BƯỚC 6: Tích hợp ai-agent-service (Graph + Hybrid Search)                        |
|  - Cài đặt Neo4j và Qdrant. Xây dựng bảng ai_index_jobs và retry policy.          |
|  - Thiết lập Function Calling gọi ngược về Product/Inventory/Order để chống ảo giác|
|  - Triển khai thuật toán Hybrid Search & Re-ranking và API giải thích khuyến nghị |
|  - Data model: chat_sessions (UUID, context_weather), chat_messages,              |
|    ai_curated_bundles, ai_curated_bundle_items, ai_analytics_logs                |
+---------------------------------------+-------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|  BƯỚC 7: Docker Compose toàn hệ thống                                             |
|  - Viết Dockerfile cho từng service, đóng gói toàn bộ hệ thống bằng docker-compose|
|  - Chạy seed data theo DATA_MODEL_DOCUMENTATION, thực thi index dữ liệu AI và     |
|    tiến hành kiểm thử tích hợp.                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 2. Chi tiết thực hiện từng bước

### Bước 1: Vận hành Frontend với Mock API (Hiện trạng)
* **Mục tiêu:** Đảm bảo giao diện người dùng chạy ổn định cục bộ.
* **Hành động:** Sử dụng `npm run dev` để chạy FE tại cổng mặc định `http://localhost:5173`. Xác nhận các luồng đi trên màn hình hoạt động chính xác dựa trên mock dữ liệu từ thư mục `src/data`.

### Bước 2: Thiết lập API Gateway
* **Mục tiêu:** Tạo đầu mối định tuyến tập trung và thiết lập cấu trúc bảo mật.
* **Hành động:**
  1. Tạo dự án Spring Boot mới với dependency `spring-cloud-starter-gateway`.
  2. Định cấu hình `application.yml` định tuyến các prefix `/api/auth/**` -> `auth-service`, `/api/products/**` -> `product-service`, v.v.
  3. Viết một `GlobalFilter` thực hiện:
     * Xóa sạch các tiêu đề `X-User-Id` và `X-User-Roles` nếu có trong request gửi từ bên ngoài.
     * Kiểm tra tiêu đề `Authorization`. Nếu có, giải mã chữ ký JWT, lấy ra `userId` và `roles` gán lại vào header `X-User-Id` và `X-User-Roles`.
  4. Chặn toàn bộ các request từ ngoài internet gọi vào các endpoint `/internal/**`.

### Bước 3: Triển khai Nền tảng định danh (Auth & User Services)
* **Mục tiêu:** Đăng ký, đăng nhập và quản lý hồ sơ phong cách.
* **Hành động:**
  1. Tạo `auth-service`, kết nối `auth_db`. Cài đặt Spring Security và thư viện `jjwt`. Viết API đăng ký, đăng nhập (mã hóa mật khẩu bằng BCryptPasswordEncoder). Schema `users` theo DATA_MODEL_DOCUMENTATION: `id, email, password_hash, full_name, provider, provider_id, role, created_at`.
  2. Tạo `user-service`, kết nối `user_db`. Cung cấp API cập nhật thông tin dáng người (`customer_style_profiles`: gender, age, height_cm, weight_kg, body_morphology, preferred_fit, style_personas JSONB) và sổ địa chỉ (`delivery_addresses`).
  3. Cấu hình phân quyền phương thức (Method Security) tại các controller nghiệp vụ:
     * `ROLE_CUSTOMER` cho các tác vụ mua sắm cá nhân.
     * `ROLE_ADMIN` cho các tác vụ quản trị hệ thống.

### Bước 4: Triển khai Nghiệp vụ Sản phẩm (Product Service)
* **Mục tiêu:** Quản lý danh mục, thông tin sản phẩm, ảnh S3.
* **Hành động:**
  1. Tạo `product-service`, kết nối `product_db`. Cấu hình AWS SDK S3 kết nối đến MinIO Container.
  2. Thiết lập API Upload ảnh cho Admin:
     * Ảnh tải lên được lưu vào MinIO bucket `stylemind-products`.
     * URL truy cập ảnh công khai được lưu vào bảng `product_images`.
  3. Schema sản phẩm theo DATA_MODEL_DOCUMENTATION:
     * `categories` (id, name, parent_id, slug) - hỗ trợ cây danh mục đệ quy
     * `products` (id, category_id, name, description, base_price, aesthetic_style, target_demographic, seasonal_property, status, created_at, updated_at)
     * `product_variants` (id, product_id, sku UNIQUE, size, color, material, price_override)
     * `product_images` (id, product_id, image_url, is_primary)

### Bước 5: Triển khai Mua sắm, Đặt hàng & Thanh toán (Cart, Order & Payment Services)
* **Mục tiêu:** Vận hành giỏ hàng (Guest/User), tạo đơn hàng và giả lập thanh toán.
* **Hành động:**
  1. Tạo `cart-service`, kết nối `cart_db`. Hỗ trợ nhận diện giỏ hàng qua `guestSessionId`. Schema: `shopping_carts` (id PK, user_id UNIQUE nullable), `cart_items` (id, cart_id, variant_id FK, quantity, is_ai_recommended, source_bundle_id).
  2. Viết logic `/cart/merge` thực hiện gộp sản phẩm từ giỏ khách vãng lai sang tài khoản chính sau khi người dùng đăng nhập.
  3. Tạo `order-service` và `payment-service`.
  4. Triển khai quy trình tạo đơn hàng sử dụng **Saga Pattern đơn giản (Orchestrator-based)** qua REST gọi đồng bộ:
     * `order-service` nhận yêu cầu -> Tạo đơn trạng thái `PENDING`.
     * Gọi `payment-service` để xử lý giao dịch.
     * Nếu mọi bước thành công, đổi trạng thái đơn sang `FULFILLED`.
     * Nếu thanh toán lỗi, thực hiện rollback (đổi đơn sang `CANCELLED` hoặc `COMPENSATING_ROLLBACK`).
  5. Schema `orders` theo DATA_MODEL_DOCUMENTATION: `order_status` (PENDING, PROCESSING, COMPENSATING_ROLLBACK, FULFILLED, CANCELLED), `shipping_address` (snapshot text).
  6. Schema `order_items`: `variant_id`, `quantity`, `price_at_purchase` (snapshot), `is_ai_conversion`, `source_bundle_id`.
  7. Schema `transactions` (payment_db): `order_id`, `user_id`, `amount`, `method`, `status`, `transaction_ref`.

### Bước 6: Tích hợp Dịch vụ Trí tuệ nhân tạo (AI Agent Service)
* **Mục tiêu:** Kết nối cơ sở dữ liệu vector/graph và thực hiện tư vấn thời trang thông minh.
* **Hành động:**
  1. Tạo `ai-agent-service`, kết nối `ai_db`, Qdrant và Neo4j.
  2. Tạo bảng `ai_index_jobs` quản lý đồng bộ. Viết Scheduler quét các job lỗi để tự động Retry với cơ chế lũy thừa thời gian chờ (Exponential Backoff).
  3. Cấu hình Spring AI kết hợp mô hình LLM (như Gemini/GPT).
  4. Triển khai **Function Calling**: Định nghĩa các Spring AI `@Tool` gọi Feign Client sang `product-service` (lấy giá gốc qua `/internal/products/:id`) và `order-service` (lấy trạng thái đơn hàng hiện tại của chính user đó qua `/internal/orders/:id`) để loại bỏ hoàn toàn việc AI tự bịa thông tin.
  5. Xây dựng logic **Hybrid Search**: thực hiện truy vấn song song Vector Search (Qdrant), Keyword Search (Full-text search trên Postgres), Graph Traversal (Neo4j) để sinh context gửi LLM.
  6. Schema AI theo DATA_MODEL_DOCUMENTATION:
     * `chat_sessions` (id UUID, user_id, context_weather_temp, context_weather_condition, created_at)
     * `chat_messages` (id, session_id, sender_type USER/AI, message_text, has_product_block, created_at)
     * `ai_curated_bundles` (id, message_id FK, justification_summary, created_at)
     * `ai_curated_bundle_items` (bundle_id, product_id) - Many-to-Many
     * `ai_analytics_logs` (id, user_id, bundle_id, interaction_type IMPRESSION/CLICK/ADD_TO_CART, created_at)
     * `ai_index_jobs` (id, target_type, target_id, operation_type, status, retry_count, last_error_message)
  7. Áp dụng các tham số khống chế: rate limit (5 request/phút), `top-k` (lấy tối đa 10 sản phẩm từ DB), `rerank` threshold (bỏ kết quả có điểm < 0.65), và giới hạn context token gửi LLM dưới 4096 tokens.

### Bước 7: Đóng gói và Vận hành Hệ thống với Docker Compose
* **Mục tiêu:** Container hóa toàn bộ hệ thống và chạy thử nghiệm tích hợp.
* **Hành động:** Viết `Dockerfile` cho từng microservice Java và build thành các Docker images, thiết lập tệp `docker-compose.yml` liên kết toàn bộ CSDL và mã nguồn (8 databases: auth_db, user_db, product_db, cart_db, order_db, payment_db, ai_db, notification_db).

---

## 3. Kế hoạch nạp dữ liệu mẫu (Seed Data) và Indexing Timeline

Để hệ thống AI hoạt động chính xác ngay khi khởi chạy, việc chuẩn bị dữ liệu mẫu và lập chỉ mục phải tuân thủ nghiêm ngặt theo thứ tự sau (theo DATA_MODEL_DOCUMENTATION):

```text
+-------------------------------------------------------------+
| BƯỚC A: Nạp dữ liệu nghiệp vụ vào PostgreSQL                |
| - Nạp Categories (cây đệ quy) & Products vào [product_db].  |
| - Nạp Product Variants (sku, size, color, material,        |
|   price_override) vào [product_db].                         |
| - Nạp Product Images vào [product_db].                      |
| - Nạp tài khoản mẫu (users) vào [auth_db].                 |
| - Nạp Style Profile (customer_style_profiles) & Addresses  |
|   vào [user_db].                                            |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
| BƯỚC B: Thực hiện Indexing sản phẩm sang AI                 |
| - Gọi API nội bộ của ai-agent-service để quét sản phẩm.     |
| - Trích xuất văn bản (description, aesthetic_style,         |
|   target_demographic, seasonal_property) -> Sinh Vector     |
|   Embedding -> Qdrant.                                      |
| - Tạo các Nodes (Product, Category, Color, Material) ->     |
|   Neo4j. Thiết lập quan hệ: BELONGS_TO, HAS_COLOR,         |
|   MADE_OF, MATCHES_STYLE, SUITABLE_FOR, etc.               |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
| BƯỚC C: Nạp luật thời trang và Outfit mẫu trực tiếp vào Graph|
| - Nạp các nút Style, Occasion, Season, BodyType.            |
| - Thiết lập các mối quan hệ thời trang giữa chúng.          |
| - Nạp các Outfit mẫu (ai_curated_bundles) liên kết đến      |
|   Product Nodes sẵn có.                                     |
+-------------------------------------------------------------+
```

### Chi tiết các bước nạp dữ liệu:

1. **Nạp dữ liệu nghiệp vụ chính (PostgreSQL):**
   * Nạp 12+ sản phẩm quần áo mẫu từ tệp `mockProducts.js` vào bảng `products` của `product_db` (có base_price, aesthetic_style, target_demographic, seasonal_property, status).
   * Nạp biến thể sản phẩm (variants: sku, size, color, material, price_override) vào `product_variants`.
   * Nạp hình ảnh sản phẩm vào `product_images`.
   * Tạo sẵn tài khoản người dùng mẫu trong `auth_db` (`users`: email, password_hash, full_name, provider, role).
   * Tạo hồ sơ phong cách mẫu (`customer_style_profiles`: gender, age, height_cm, weight_kg, body_morphology, preferred_fit, style_personas JSONB) và địa chỉ (`delivery_addresses`) trong `user_db` từ tệp `mockUsers.js`.
   * Tạo giỏ hàng mẫu (`shopping_carts`, `cart_items`) và đơn hàng mẫu (`orders`, `order_items`) với các trường tracking AI (`is_ai_conversion`, `source_bundle_id`).

2. **Kích hoạt Indexing sang Vector DB & Graph DB:**
   * Chỉ chạy sau khi Bước 1 hoàn tất. Gọi API quản trị `/api/admin/ai/index/products/reindex` để hệ thống tự động quét toàn bộ sản phẩm từ `product-service` qua REST API nội bộ.
   * `ai-agent-service` tiến hành phân tích thông tin mô tả sản phẩm, chuyển đổi thành vector embedding qua mô hình text-embedding và lưu vào Collection `products` trên Qdrant.
   * Đồng thời, tạo các nút `Product` tương ứng trên Neo4j và liên kết chúng với các nút `Category`, `Color` và `Material` tương tự.

3. **Thiết lập Đồ thị tri thức (Neo4j):**
   * Nạp các thực thể phong cách tĩnh (Style: Minimalist, Streetwear), dịp sử dụng (Occasion: Dinner, Work), dáng người (BodyType: Slim, Athletic, Hourglass, Pear, Rectangle).
   * Thiết lập các liên kết suy luận thời trang và quy tắc (ví dụ: `FashionRule -[:RECOMMENDS]-> Product`).
   * Nạp outfit mẫu từ dữ liệu AI tạo ra bằng cách tạo nuớc `ai_curated_bundles` và `ai_curated_bundle_items` liên kết đến các sản phẩm đã có.

4. **Kiểm tra hoạt động:**
   * Sau khi hoàn tất nạp và index, truy cập `/api/admin/ai/graph/status` để xác nhận số lượng nút và quan hệ được khởi tạo đầy đủ.
   * Kiểm tra `/api/admin/ai/analytics-logs` để track interaction events.
   * AI Agent lúc này đã sẵn sàng phục vụ tư vấn mà không bị ảo giác.
