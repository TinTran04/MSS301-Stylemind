# LỘ TRÌNH CHUYỂN ĐỔI HỆ THỐNG (MIGRATION ROADMAP)

Tài liệu này vạch ra lộ trình 7 bước chuyển đổi từ hệ thống UI/Frontend tĩnh hiện tại sang hệ thống Microservices chạy thực tế sử dụng Spring Boot và AI Agent.

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
|  - Xây dựng database auth_db & user_db. Đăng ký, đăng nhập bcrypt, phát hành JWT. |
|  - Gateway giải mã JWT, truyền định danh qua header. Phân quyền ROLE_CUSTOMER/ADMIN|
|  - Quản lý Style Profile phục vụ cá nhân hóa cho AI.                              |
+---------------------------------------+-------------------------------------------+
                                        |
                                        v
+-----------------------------------------------------------------------------------+
|  BƯỚC 4: Tạo product-service và inventory-service                                 |
|  - Quản lý CRUD sản phẩm. Thiết lập MinIO để lưu trữ ảnh, lưu URL vào product_db.  |
|  - Quản lý tồn kho thực tế và cơ chế giữ kho (Inventory Reservation).              |
+---------------------------------------+-------------------------------------------+
                                        |
                                        v
+-----------------------------------------------------------------------------------+
|  BƯỚC 5: Tạo cart-service, order-service và payment-service                       |
|  - Thiết lập Guest Cart (guestSessionId) và cơ chế Merge Cart sau đăng nhập.       |
|  - Xây dựng luồng tạo đơn hàng, thanh toán giả lập, quản lý các máy trạng thái.   |
+---------------------------------------+-------------------------------------------+
                                        |
                                        v
+-----------------------------------------------------------------------------------+
|  BƯỚC 6: Tích hợp ai-agent-service (Graph + Hybrid Search)                        |
|  - Cài đặt Neo4j và Qdrant. Xây dựng bảng ai_index_jobs và retry policy.          |
|  - Thiết lập Function Calling gọi ngược về Product/Inventory/Order để chống ảo giác|
|  - Triển khai thuật toán Hybrid Search & Re-ranking và API giải thích khuyến nghị |
+---------------------------------------+-------------------------------------------+
                                        |
                                        v
+-----------------------------------------------------------------------------------+
|  BƯỚC 7: Docker Compose toàn hệ thống                                             |
|  - Viết Dockerfile cho từng service, đóng gói toàn bộ hệ thống bằng docker-compose|
|  - Chạy seed data, thực thi index dữ liệu AI và tiến hành kiểm thử tích hợp.      |
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
  1. Tạo `auth-service`, kết nối `auth_db`. Cài đặt Spring Security và thư viện `jjwt`. Viết API đăng ký, đăng nhập (mã hóa mật khẩu bằng BCryptPasswordEncoder).
  2. Tạo `user-service`, kết nối `user_db`. Cung cấp API cập nhật thông tin dáng người, sở thích và size đồ cá nhân.
  3. Cấu hình phân quyền phương thức (Method Security) tại các controller nghiệp vụ:
     * `ROLE_CUSTOMER` cho các tác vụ mua sắm cá nhân.
     * `ROLE_ADMIN` cho các tác vụ quản trị hệ thống.

### Bước 4: Triển khai Nghiệp vụ Sản phẩm & Kho hàng (Product & Inventory Services)
* **Mục tiêu:** Quản lý danh mục, thông tin sản phẩm, ảnh S3 và số lượng tồn kho.
* **Hành động:**
  1. Tạo `product-service`, kết nối `product_db`. Cấu hình AWS SDK S3 kết nối đến MinIO Container.
  2. Thiết lập API Upload ảnh cho Admin:
     * Ảnh tải lên được lưu vào MinIO bucket `stylemind-products`.
     * URL truy cập ảnh công khai được lưu vào bảng `product_images`.
  3. Tạo `inventory-service`, kết nối `inventory_db`.
  4. Cung cấp API nội bộ `/internal/inventory/reserve` hỗ trợ khóa/giữ kho tạm thời khi có đơn hàng mới và `/internal/inventory/commit` để giảm trừ kho vĩnh viễn khi thanh toán thành công.

### Bước 5: Triển khai Mua sắm, Đặt hàng & Thanh toán (Cart, Order & Payment Services)
* **Mục tiêu:** Vận hành giỏ hàng (Guest/User), tạo đơn hàng và giả lập thanh toán.
* **Hành động:**
  1. Tạo `cart-service`, kết nối `cart_db`. Hỗ trợ nhận diện giỏ hàng qua `guestSessionId`.
  2. Viết logic `/cart/merge` thực hiện gộp sản phẩm từ giỏ khách vãng lai sang tài khoản chính sau khi người dùng đăng nhập.
  3. Tạo `order-service` và `payment-service`.
  4. Triển khai quy trình tạo đơn hàng sử dụng **Saga Pattern đơn giản (Orchestrator-based)** qua REST gọi đồng bộ:
     * `order-service` nhận yêu cầu -> Tạo đơn trạng thái `PENDING`.
     * Gọi `inventory-service` để giữ kho (`reserve`).
     * Gọi `payment-service` để xử lý giao dịch.
     * Nếu mọi bước thành công, đổi trạng thái đơn sang `CONFIRMED`, đổi trạng thái kho sang `COMMITTED`.
     * Nếu giữ kho thất bại hoặc thanh toán lỗi, thực hiện rollback (đổi đơn sang `CANCELLED` và giải phóng kho `RELEASED`).

### Bước 6: Tích hợp Dịch vụ Trí tuệ nhân tạo (AI Agent Service)
* **Mục tiêu:** Kết nối cơ sở dữ liệu vector/graph và thực hiện tư vấn thời trang thông minh.
* **Hành động:**
  1. Tạo `ai-agent-service`, kết nối `ai_db`, Qdrant và Neo4j.
  2. Tạo bảng `ai_index_jobs` quản lý đồng bộ. Viết Scheduler quét các job lỗi để tự động Retry với cơ chế lũy thừa thời gian chờ (Exponential Backoff).
  3. Cấu hình Spring AI kết hợp mô hình LLM (như Gemini/GPT).
  4. Triển khai **Function Calling**: Định nghĩa các Spring AI `@Tool` gọi Feign Client sang `product-service` (lấy giá gốc), `inventory-service` (lấy tồn kho thực tế) và `order-service` (lấy trạng thái đơn hàng hiện tại của chính user đó) để loại bỏ hoàn toàn việc AI tự bịa thông tin.
  5. Xây dựng logic **Hybrid Search**: thực hiện truy vấn song song Vector Search (Qdrant), Keyword Search (Full-text search trên Postgres), Graph Traversal (Neo4j) để sinh context gửi LLM.
  6. Áp dụng các tham số khống chế: rate limit (5 request/phút), `top-k` (lấy tối đa 10 sản phẩm từ DB), `rerank` threshold (bỏ kết quả có điểm < 0.65), và giới hạn context token gửi LLM dưới 4096 tokens.

### Bước 7: Đóng gói và Vận hành Hệ thống với Docker Compose
* **Mục tiêu:** Container hóa toàn bộ hệ thống và chạy thử nghiệm tích hợp.
* **Hành động:** Viết `Dockerfile` cho từng microservice Java và build thành các Docker images, thiết lập tệp `docker-compose.yml` liên kết toàn bộ CSDL và mã nguồn.

---

## 3. Kế hoạch nạp dữ liệu mẫu (Seed Data) và Indexing Timeline

Để hệ thống AI hoạt động chính xác ngay khi khởi chạy, việc chuẩn bị dữ liệu mẫu và lập chỉ mục phải tuân thủ nghiêm ngặt theo thứ tự sau:

```text
+-------------------------------------------------------------+
| BƯỚC A: Nạp dữ liệu nghiệp vụ vào PostgreSQL                |
| - Nạp Categories & Products vào [product_db].               |
| - Nạp số lượng tồn kho cho các SKUs tương ứng vào [inv_db]. |
| - Nạp tài khoản mẫu và sở thích khách hàng vào [user_db].   |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
| BƯỚC B: Thực hiện Indexing sản phẩm sang AI                 |
| - Gọi API nội bộ của ai-agent-service để quét sản phẩm.     |
| - Trích xuất văn bản -> Sinh Vector Embedding -> Qdrant.    |
| - Tạo các Nodes (Product, Category, Color) -> Neo4j.        |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
| BƯỚC C: Nạp luật thời trang và Outfit mẫu trực tiếp vào Graph|
| - Nạp các nút Style, Occasion, Season, BodyType.            |
| - Thiết lập các mối quan hệ thời trang giữa chúng.          |
| - Nạp các Outfit mẫu liên kết đến Product Nodes sẵn có.     |
+-------------------------------------------------------------+
```

### Chi tiết các bước nạp dữ liệu:
1. **Nạp dữ liệu nghiệp vụ chính (PostgreSQL):**
   * Nạp 12 sản phẩm quần áo mẫu từ tệp [mockProducts.js](file:///e:/Ki_8/MSS/MSS301-Stylemind/FE/src/data/mockProducts.js) vào bảng `products` của `product_db`.
   * Khởi tạo số lượng tồn kho thực tế cho các sản phẩm này trong bảng `inventories` của `inventory_db` dựa trên tệp [mockInventory.js](file:///e:/Ki_8/MSS/MSS301-Stylemind/FE/src/data/mockInventory.js).
   * Tạo sẵn tài khoản người dùng mẫu trong `auth_db` và hồ sơ sở thích tương ứng trong `user_db` từ tệp [mockUsers.js](file:///e:/Ki_8/MSS/MSS301-Stylemind/FE/src/data/mockUsers.js).
2. **Kích hoạt Indexing sang Vector DB & Graph DB:**
   * Chỉ chạy sau khi Bước 1 hoàn tất. Gọi API quản trị `/api/admin/ai/index/products/reindex` để hệ thống tự động quét toàn bộ sản phẩm từ `product-service` qua REST API nội bộ.
   * `ai-agent-service` tiến hành phân tích thông tin mô tả sản phẩm, chuyển đổi thành vector embedding qua mô hình text-embedding và lưu vào Collection `products` trên Qdrant.
   * Đồng thời, tạo các nút `Product` tương ứng trên Neo4j và liên kết chúng với các nút `Category`, `Color` và `Material` tương tự.
3. **Thiết lập Đồ thị tri thức (Neo4j):**
   * Nạp các thực thể phong cách tĩnh (Style: Minimalist, Streetwear), dịp sử dụng (Occasion: Dinner, Work), dáng người (BodyType: Slim, Athletic).
   * Thiết lập các liên kết suy luận thời trang và quy tắc (ví dụ: `FashionRule -[:RECOMMENDS]-> Product`).
   * Nạp 2 outfit mẫu từ tệp [mockProducts.js](file:///e:/Ki_8/MSS/MSS301-Stylemind/FE/src/data/mockProducts.js) (Minimalist Evening và Casual Luxe) bằng cách tạo nút `Outfit` và liên kết `[:CONTAINS]` đến các sản phẩm đã có.
4. **Kiểm tra hoạt động:**
   * Sau khi hoàn tất nạp và index, truy cập `/api/admin/ai/graph/status` để xác nhận số lượng nút và quan hệ được khởi tạo đầy đủ. AI Agent lúc này đã sẵn sàng phục vụ tư vấn mà không bị ảo giác.
