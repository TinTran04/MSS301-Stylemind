# KIẾN TRÚC MICROSERVICES STYLEMIND (ĐẶC TẢ CHI TIẾT)

Tài liệu này đặc tả chi tiết kiến trúc hệ thống backend dạng Microservices kết hợp AI Agent thông minh cho dự án **Stylemind**. Tài liệu được biên soạn nhằm hướng dẫn lập trình viên phát triển hệ thống Spring Boot, thiết lập cơ sở dữ liệu và vận hành hệ thống.

---

## 1. Sơ đồ kiến trúc tổng thể hệ thống

Hệ thống được thiết kế theo kiến trúc Microservices phi tập trung, kết nối qua một điểm đầu mối duy nhất là API Gateway.

```text
                               +-----------------------------+
                               |     Frontend Client (FE)    |
                               +--------------+--------------+
                                              |
                                              | HTTPS (Cổng công khai: 3000/80)
                                              v
+---------------------------------------------+----------------------------------------------+
|                                  API GATEWAY (Spring Cloud Gateway)                       |
|                                                                                            |
| - Xác thực JWT tập trung                - Phân quyền theo vai trò (RBAC)                   |
| - Rate Limiting (AI Chat / API)         - Strip Headers (X-User-Id, X-User-Roles)          |
+----------------------+----------------------+----------------------+-----------------------+
                       |                      |                      |
      (Giao tiếp REST) |                      | (Giao tiếp REST)     | (Giao tiếp REST)
                       v                      v                      v
+----------------------+-+      +-------------+--------+      +------+-----------------+
|      auth-service      |      |     user-service     |      |     product-service     |
| (Port: 8081)           |      | (Port: 8082)         |      | (Port: 8083)            |
+----------+-------------+      +----------+-----------+      +----------+--------------+
           |                               |                             |
           v [PostgreSQL]                  v [PostgreSQL]                v [PostgreSQL]
       [auth_db]                       [user_db]                     [product_db]
                                                                         |
                                                                         v (Chỉ lưu URLs)
                                                              +----------+--------------+
                                                              |  Object Storage (MinIO)  |
                                                              +-------------------------+
                       |                      |                      |
                       v                      v                      v
+----------------------+-+      +-------------+--------+      +------+-----------------+
|    inventory-service   |      |     cart-service     |      |      order-service      |
| (Port: 8084)           |      | (Port: 8086)         |      | (Port: 8087)            |
+----------+-------------+      +----------+-----------+      +----------+--------------+
           |                               |                             |
           v [PostgreSQL]                  v [PostgreSQL]                v [PostgreSQL]
       [inventory_db]                  [cart_db]                     [order_db]
                       |                      |                      |
                       v                      v                      v
+----------------------+-+      +-------------+--------+      +------+-----------------+
|     payment-service    |      |  notification-service|      |     ai-agent-service    |
| (Port: 8088)           |      | (Port: 8089)         |      | (Port: 8085)            |
+----------+-------------+      +----------+-----------+      +----+--------+--------+-+
           |                               |                       |        |        |
           v [PostgreSQL]                  v [PostgreSQL]          |        |        |
       [payment_db]                    [notification_db]           |        |        |
                                                                   v        v        v
                                                            [PostgreSQL] [Qdrant] [Neo4j]
                                                              [ai_db]   [Vectors] [Graph]
```

### A. Quy tắc giao tiếp liên dịch vụ (Service-to-Service Communication)
* **Không truy vấn chéo DB:** Các dịch vụ hoàn toàn độc lập về cơ sở dữ liệu. Không một microservice nào được phép kết nối trực tiếp hoặc query vào database của service khác.
* **Giao tiếp đồng bộ:** Sử dụng **REST API (HTTP) thông qua Spring Cloud OpenFeign** cho các cuộc gọi cần phản hồi ngay (như kiểm tra kho khi tạo đơn).
* **Bảo mật nội bộ:** Các API phục vụ giao tiếp nội bộ (`/internal/**`) bị chặn ở API Gateway và bắt buộc phải gửi kèm tiêu đề xác thực `X-Internal-Token` (Pre-shared key).

---

## 2. Phân chia Scope: MVP vs Future

Để tối ưu hóa tài nguyên và đảm bảo khả năng triển khai thực tế, dự án phân chia lộ trình làm 2 giai đoạn:

### MVP Scope (Phạm vi chạy thử ban đầu)
1. **Kiến trúc AI (Graph + Hybrid Search):** Đưa **Neo4j** (Đồ thị tri thức) và **Qdrant** (Vector Database) vào hoạt động trực tiếp trong MVP để chạy tìm kiếm lai.
2. **Giao tiếp liên dịch vụ:** Giao tiếp trực tiếp qua REST API (HTTP/JSON) sử dụng Spring Cloud OpenFeign nhằm giảm độ phức tạp vận hành.
3. **Cơ sở dữ liệu:** PostgreSQL cho nghiệp vụ, Qdrant cho Vector, Neo4j cho Graph, Redis cho lưu caching/session.
4. **Quy tắc Index AI:** Qdrant và Neo4j chỉ được index dữ liệu **sau khi** dữ liệu sản phẩm, tồn kho và seed data nghiệp vụ đã được khởi tạo thành công ở các database Postgres chính.
5. **Lưu trữ ảnh:** Sử dụng **MinIO** chạy local giả lập S3.

### Future Scope (Phạm vi mở rộng tương lai)
1. **Kiến trúc hướng sự kiện (Event-Driven):** Tích hợp **Apache Kafka** hoặc **RabbitMQ** để xử lý bất đồng bộ (ví dụ: phát event `ProductCreated` để đồng bộ sang Qdrant/Neo4j, event `OrderCreated` để gửi thông báo).
2. **Hệ thống giám sát nâng cao:** Triển khai ELK/EFK Stack (Elasticsearch, Logstash, Kibana) để gom log tập trung và Zipkin/Jaeger cho Distributed Tracing chuyên sâu.
3. **Quản trị hạ tầng:** Triển khai Kubernetes (K8s) và API Gateway nâng cấp (Kong, Apisix).

---

## 3. Phân định quyền sở hữu dữ liệu (Data Ownership)

Quyền sở hữu dữ liệu được phân định rạch ròi theo bảng dưới đây:

| Service | Cơ sở dữ liệu | Quyền sở hữu bảng (Tables Owned) | Trách nhiệm chính |
| :--- | :--- | :--- | :--- |
| `auth-service` | `auth_db` (Postgres) | `users`, `roles`, `user_roles` | Lưu thông tin đăng nhập cá nhân (email, hashed password) và vai trò phân quyền. |
| `user-service` | `user_db` (Postgres) | `user_profiles`, `style_preferences`, `size_profiles` | Lưu thông tin hồ sơ khách hàng, phân hạng thành viên, các chỉ số đo dáng người và sở thích ăn mặc. |
| `product-service` | `product_db` (Postgres) | `categories`, `products`, `product_variants`, `product_images` | Lưu trữ thông tin danh mục, thông tin chi tiết sản phẩm, chất liệu, màu sắc, size, và các URLs ảnh liên kết tới MinIO. |
| `inventory-service`| `inventory_db` (Postgres)| `inventories`, `inventory_reservations` | Quản lý số lượng tồn kho thực tế của từng SKU và lượng hàng đang bị giữ chỗ tạm thời (reserved). |
| `cart-service` | `cart_db` (Postgres) | `carts`, `cart_items` | Lưu trữ giỏ hàng hiện tại của người dùng đã đăng nhập hoặc khách vãng lai (`guestSessionId`). |
| `order-service` | `order_db` (Postgres) | `orders`, `order_items`, `order_timeline` | Quản lý đơn đặt hàng, lịch sử mua và cập nhật hành trình vận đơn. |
| `payment-service` | `payment_db` (Postgres) | `transactions` | Lưu trữ lịch sử giao dịch thanh toán trực tiếp hoặc COD. |
| `ai-agent-service` | `ai_db` (Postgres) <br> **Qdrant** (Vector) <br> **Neo4j** (Graph) | `ai_conversations`, `ai_messages`, `ai_feedback`, `ai_index_jobs` | **Không sở hữu dữ liệu gốc của sản phẩm hay khách hàng.** <br> Chỉ sở hữu database chat log, feedback và bảng quản lý index. Qdrant và Neo4j chỉ lưu các snapshot metadata, vector embeddings và liên kết đồ thị phục vụ mục đích tìm kiếm. Khi cần giá/tồn kho thực tế, phải gọi API runtime sang các service sở hữu gốc. |
| `notification-service`| `notification_db` (Postgres)| `notification_logs` | Lưu trữ lịch sử gửi email, SMS hoặc thông báo hệ thống. |

---

## 4. Đặc tả cơ sở dữ liệu (Database Schemas)

### 4.1. PostgreSQL (Các bảng cốt lõi)

#### A. `auth_db`
```sql
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY, -- Sinh dạng UUID hoặc ID đồng bộ
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id INT PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL -- ROLE_CUSTOMER, ROLE_ADMIN
);

CREATE TABLE user_roles (
    user_id VARCHAR(50) REFERENCES users(id),
    role_id INT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);
```

#### B. `user_db`
```sql
CREATE TABLE user_profiles (
    user_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    tier VARCHAR(20) DEFAULT 'Silver', -- Silver, Gold, Platinum
    join_date DATE DEFAULT CURRENT_DATE,
    total_spent DECIMAL(12, 2) DEFAULT 0.00
);

CREATE TABLE style_preferences (
    user_id VARCHAR(50) REFERENCES user_profiles(user_id),
    body_type VARCHAR(50), -- Slim, Athletic, Rectangular, Pear, Hourglass
    fit_preference VARCHAR(50), -- Slim, Regular, Oversized
    favorite_colors VARCHAR(255), -- Black, Cream, Navy (Lưu chuỗi phân tách dấu phẩy hoặc bảng phụ)
    style_dna VARCHAR(255), -- Minimalist, Classic, Streetwear
    PRIMARY KEY (user_id)
);

CREATE TABLE size_profiles (
    user_id VARCHAR(50) REFERENCES user_profiles(user_id),
    top_size VARCHAR(10), -- XS, S, M, L, XL
    bottom_size VARCHAR(10),
    shoe_size VARCHAR(10),
    PRIMARY KEY (user_id)
);
```

#### C. `product_db`
```sql
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE products (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    original_price DECIMAL(12, 2),
    category_id INT REFERENCES categories(id),
    description TEXT,
    material VARCHAR(100),
    sku VARCHAR(50) UNIQUE NOT NULL,
    rating DECIMAL(3, 2) DEFAULT 0.00,
    is_new BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_variants (
    id SERIAL PRIMARY KEY,
    product_id VARCHAR(50) REFERENCES products(id) ON DELETE CASCADE,
    color VARCHAR(50) NOT NULL,
    size VARCHAR(20) NOT NULL,
    sku_variant VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE product_images (
    id SERIAL PRIMARY KEY,
    product_id VARCHAR(50) REFERENCES products(id) ON DELETE CASCADE,
    image_url VARCHAR(255) NOT NULL, -- Chỉ lưu URL trỏ đến MinIO/S3
    is_primary BOOLEAN DEFAULT FALSE
);
```

#### D. `inventory_db`
```sql
CREATE TABLE inventories (
    sku VARCHAR(100) PRIMARY KEY, -- Trùng với sku_variant của sản phẩm
    current_stock INT NOT NULL DEFAULT 0,
    reserved_stock INT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_reservations (
    id VARCHAR(50) PRIMARY KEY, -- UUID cho mỗi phiên giữ kho
    sku VARCHAR(100) REFERENCES inventories(sku),
    order_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING_RESERVE, RESERVED, COMMITTED, RELEASED
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### E. `cart_db`
```sql
CREATE TABLE carts (
    id VARCHAR(50) PRIMARY KEY, -- Có thể là userId hoặc guestSessionId
    is_guest BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cart_items (
    id SERIAL PRIMARY KEY,
    cart_id VARCHAR(50) REFERENCES carts(id) ON DELETE CASCADE,
    product_id VARCHAR(50) NOT NULL,
    sku_variant VARCHAR(100) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    size VARCHAR(20) NOT NULL,
    color VARCHAR(50) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### F. `order_db`
```sql
CREATE TABLE orders (
    id VARCHAR(50) PRIMARY KEY, -- ORD-yyyyMMdd-XXXXX
    user_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    total_amount DECIMAL(12, 2) NOT NULL,
    shipping_address TEXT NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id VARCHAR(50) REFERENCES orders(id) ON DELETE CASCADE,
    product_id VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    sku_variant VARCHAR(100) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    quantity INT NOT NULL,
    size VARCHAR(20) NOT NULL,
    color VARCHAR(50) NOT NULL,
    image_url VARCHAR(255)
);

CREATE TABLE order_timeline (
    id SERIAL PRIMARY KEY,
    order_id VARCHAR(50) REFERENCES orders(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed BOOLEAN DEFAULT FALSE
);
```

#### G. `ai_db` (`ai-agent-service`)
```sql
CREATE TABLE ai_conversations (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50), -- NULL đối với khách vãng lai
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_messages (
    id VARCHAR(50) PRIMARY KEY,
    conversation_id VARCHAR(50) REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(10) NOT NULL, -- 'user' hoặc 'ai'
    content TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_feedback (
    id SERIAL PRIMARY KEY,
    message_id VARCHAR(50) REFERENCES ai_messages(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating IN (-1, 1)), -- -1: dislike, 1: like
    comment VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Định nghĩa chi tiết bảng quản lý index job phục vụ đồng bộ dữ liệu AI
CREATE TABLE ai_index_jobs (
    id VARCHAR(50) PRIMARY KEY,                  -- UUID định danh công việc index
    target_type VARCHAR(30) NOT NULL,            -- Loại đối tượng: 'PRODUCT', 'INVENTORY', 'RULE'
    target_id VARCHAR(50) NOT NULL,              -- ID của sản phẩm hoặc đối tượng đích
    operation_type VARCHAR(10) NOT NULL,         -- 'CREATE', 'UPDATE', 'DELETE'
    status VARCHAR(20) NOT NULL,                 -- 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'
    retry_count INT DEFAULT 0,                   -- Số lần đã thử lại (tối đa 3 lần)
    last_error_message TEXT,                     -- Lưu vết lỗi khi kết nối Qdrant/Neo4j thất bại
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

### 4.2. Neo4j (Đồ thị tri thức thời trang)

Neo4j liên kết các thực thể phi cấu trúc để giúp LLM suy luận ra outfit và phong cách phù hợp.

#### Các Node chính
* `Product` {id, name, sku, category}
* `Category` {name}
* `Color` {name}
* `Material` {name}
* `Style` {name} (Minimalist, Classic, Streetwear, Bohemian, Romantic)
* `Occasion` {name} (Dinner, Casual, Work, Summer, Wedding)
* `Season` {name} (Spring, Summer, Autumn, Winter)
* `BodyType` {name} (Slim, Athletic, Rectangular, Pear, Hourglass)
* `Outfit` {id, name, description}

#### Các Quan hệ chính (Relationships)
```cypher
(:Product) -[:BELONGS_TO]-> (:Category)
(:Product) -[:HAS_COLOR]-> (:Color)
(:Product) -[:MADE_OF]-> (:Material)
(:Product) -[:MATCHES_STYLE]-> (:Style)
(:Product) -[:SUITABLE_FOR]-> (:Occasion)
(:Product) -[:SUITABLE_FOR_SEASON]-> (:Season)
(:Product) -[:FITS_BODY_TYPE]-> (:BodyType)
(:Product) -[:CAN_PAIR_WITH]-> (:Product)
(:Outfit) -[:CONTAINS]-> (:Product)
```

---

## 5. Định nghĩa máy trạng thái nghiệp vụ (State Machines)

Vòng đời của các đối tượng nghiệp vụ được quản lý chặt chẽ qua các trạng thái:

```text
A. Giỏ hàng (Cart State Machine)
+------------+     Đăng nhập / Merge     +------------+
|   ACTIVE   |-------------------------->|   MERGED   | (Gộp vào Cart của User)
+-----+------+                           +------------+
      |
      | Không hoạt động > 30 ngày
      v
+------------+
| ABANDONED  |
+------------+

B. Giữ kho (Inventory Reservation State Machine)
+-------------------+     Hết hạn (Expires)     +------------+
|  PENDING_RESERVE  |-------------------------->|  RELEASED  | (Trả lại kho)
+---------+---------+                           +------------+
          |                                           ^
          | Giữ thành công                            | Hủy đơn hàng
          v                                           |
+-------------------+      Xác nhận mua hàng          |
|     RESERVED      |---------------------------------+
+---------+---------+
          |
          | Thanh toán thành công (Commit)
          v
+-------------------+
|     COMMITTED     | (Giảm trừ kho vĩnh viễn)
+-------------------+

C. Đơn hàng (Order State Machine)
+-----------+    Thanh toán lỗi/Hủy    +-----------+
|  PENDING  |------------------------->| CANCELLED |
+-----+-----+                          +-----------+
      |
      | Xác nhận & Giữ kho xong
      v
+-----------+    Xử lý đóng gói     +------------+    Giao cho ĐVVC     +-----------+    Khách nhận hàng    +-----------+
| CONFIRMED |---------------------->| PROCESSING |--------------------->|  SHIPPED  |---------------------->| DELIVERED |
+-----------+                       +------------+                      +-----------+                       +-----------+

D. Thanh toán (Payment State Machine)
+-----------+    Lỗi kết nối/Từ chối   +-----------+
|  PENDING  |------------------------->|  FAILED   |
+-----+-----+                          +-----------+
      |
      | Giao dịch thành công
      v
+-----------+          Yêu cầu trả hàng          +-----------+
| COMPLETED |----------------------------------->| REFUNDED  |
+-----------+                                    +-----------+
```

---

## 6. API Gateway: Xác thực, Phân quyền & Bảo mật Headers

API Gateway đóng vai trò là chốt chặn bảo mật đầu tiên của hệ thống.

```text
[Request từ bên ngoài: chứa JWT token và có thể chứa header X-User-Id giả mạo]
                               |
                               v
                     +-------------------+
                     |    API Gateway    |
                     |                   |
                     | 1. STRIP HEADERS  | <- Xóa sạch mọi header X-User-Id / X-User-Roles nhận được từ ngoài.
                     | 2. VALIDATE JWT   | <- Kiểm tra chữ ký, hạn dùng của token.
                     | 3. DECODE JWT     | -> Lấy userId = "usr_123", roles = ["CUSTOMER"].
                     | 4. APPEND HEADERS | -> Đính kèm X-User-Id = "usr_123", X-User-Roles = "ROLE_CUSTOMER".
                     +---------+---------+
                               |
                               | Downstream (Trong mạng nội bộ bảo mật)
                               v
                     +-------------------+
                     |  Microservice BE  | <- Đọc thông tin user tin cậy trực tiếp từ Header.
                     +-------------------+
```

### A. Quy tắc bảo mật tại Gateway
1. **Strip Headers chống giả mạo:** Cấu hình Gateway loại bỏ hoàn toàn các tiêu đề `X-User-Id` và `X-User-Roles` ra khỏi request đầu vào của người dùng trước khi tiến hành xác thực. Điều này ngăn chặn kẻ xấu chèn giá trị giả mạo.
2. **Giải mã JWT:** Gateway kiểm tra tính hợp lệ của token trong Header `Authorization: Bearer <token>`. Nếu hợp lệ, lấy `userId` và `roles` từ payload.
3. **Chuyển tiếp định danh:** Gateway đính kèm `X-User-Id` và `X-User-Roles` vào header của request downstream trước khi chuyển tiếp cho các service nghiệp vụ.
4. **Phân quyền Role-Based Access Control (RBAC):**
   * Các endpoint `/admin/**` bắt buộc phải có `ROLE_ADMIN` trong header `X-User-Roles`.
   * Các endpoint mua sắm thông thường yêu cầu `ROLE_CUSTOMER` hoặc cho phép truy cập public (đối với danh mục sản phẩm).

---

## 7. Cơ chế Giỏ hàng vãng lai (Guest Cart) và Merge Cart

Hệ thống cho phép khách hàng chưa đăng nhập thêm sản phẩm vào giỏ hàng nhằm tối ưu hóa trải nghiệm mua sắm:

```text
1. Khách vãng lai truy cập shop -> Frontend sinh ngẫu nhiên guestSessionId = "gst_999".
2. Khách thêm áo thun vào giỏ hàng -> Gọi POST /cart?guestSessionId=gst_999 -> cart-service tạo giỏ hàng cho guestSessionId.
3. Khách tiến hành Đăng nhập -> Xác thực thành công nhận JWT.
4. Frontend gửi yêu cầu gộp giỏ hàng:
   POST /cart/merge
   Headers: Authorization: Bearer <JWT>
   Payload: { "guestSessionId": "gst_999" }
5. API Gateway xử lý: giải mã JWT -> đính kèm X-User-Id = "usr_777" gửi xuống cart-service.
6. cart-service xử lý:
   - Tìm giỏ hàng của gst_999 và usr_777.
   - Nếu usr_777 chưa có giỏ hàng, cập nhật cột id của giỏ gst_999 thành usr_777, set is_guest = false.
   - Nếu usr_777 đã có giỏ hàng, chuyển toàn bộ items từ giỏ gst_999 sang giỏ usr_777 (nếu trùng SKU thì cộng dồn số lượng), sau đó xóa giỏ gst_999.
7. Trả về giỏ hàng mới đã gộp thành công cho người dùng.
```

---

## 8. Thiết kế AI Agent chống ảo giác (Anti-Hallucination)

Để đảm bảo AI Agent không cung cấp thông tin sai lệch về giá bán, tồn kho hoặc đơn hàng (ảo giác của LLM), kiến trúc RAG nâng cao áp dụng quy tắc **Runtime API Fetching** thông qua **Function Calling (Spring AI / LangChain4j Tools)**.

```text
             +------------------+
             | Khách hàng hỏi   | -> "Áo sơ mi trắng Oxford giá bao nhiêu, còn size M không?"
             +--------+---------+
                      |
                      v
             +------------------+
             | ai-agent-service |
             +--------+---------+
                      |
                      |-- 1. Gọi Hybrid Search tìm sản phẩm -> Nhận sản phẩm P001 (Áo sơ mi trắng Oxford)
                      |-- 2. Phát hiện LLM cần thông tin động (giá, size M còn hàng không)
                      |
                      v [Function Calling / OpenFeign REST API]
             +-------------------------------------------------------------+
             | Giao tiếp API nội bộ (Runtime):                             |
             | - Gọi product-service: GET /internal/products/P001         | -> Trả về: Giá thực tế = 459,000 VND
             | - Gọi inventory-service: GET /internal/inventory/P001-M     | -> Trả về: Tồn kho thực tế = 5 cái
             +-------------------------------------------------------------+
                      |
                      v
             +------------------+
             | ai-agent-service | -> Ghép thông tin chính xác vào context prompt gửi LLM.
             +--------+---------+
                      |
                      v
             +------------------+
             | LLM (Gemini/GPT) | -> Sinh câu trả lời dựa trên context chính xác 100%.
             +--------+---------+
                      |
                      v
             +-------------------------------------------------------------+
             | Trả về câu trả lời: "Áo sơ mi Oxford đang có giá 459k       |
             | và size M hiện tại còn 5 chiếc trong kho, bạn có muốn mua?" |
             +-------------------------------------------------------------+
```

### Nguyên tắc bảo mật & phân quyền đối với AI
* **Không lưu trữ dữ liệu gốc:** Dịch vụ `ai-agent-service` tuyệt đối không lưu trữ bản sao cơ sở dữ liệu sản phẩm, tồn kho hay người dùng. Nó chỉ lưu các chỉ mục (vector embeddings trên Qdrant, đồ thị quan hệ thời trang trên Neo4j).
* **Kiểm tra quyền sở hữu đơn hàng (Order Ownership Verification):** Khi người dùng hỏi: *"Đơn hàng ORD-123 của tôi đi đến đâu rồi?"*, AI Agent trích xuất ID đơn hàng, sau đó gọi runtime sang `order-service` để lấy trạng thái. `order-service` bắt buộc phải đối chiếu `userId` lấy từ header `X-User-Id` (do Gateway truyền xuống) với `userId` ghi trên đơn hàng. Nếu không khớp, lập tức trả về lỗi từ chối truy cập (403), ngăn chặn AI đọc trộm thông tin đơn hàng của người khác.

---

## 9. Thiết kế Hybrid Search & Giới hạn hoạt động AI

Hệ thống kết hợp tìm kiếm ngữ nghĩa, từ khóa và đồ thị để đưa ra kết quả gợi ý thời trang hoàn hảo nhất.

### 9.1. Các thành phần của Hybrid Search
1. **Vector Search (Semantic Search):** Dùng để tìm sản phẩm có nghĩa gần nhất với truy vấn tự nhiên của người dùng (ví dụ: *"đồ đi dạo phố mát mẻ"*). Sử dụng **Qdrant** để so sánh khoảng cách cosine giữa vector truy vấn và các vector sản phẩm.
2. **Keyword Search (Từ khóa chính xác):** 
   * Đối với MVP: Sử dụng **PostgreSQL Full-text Search** tích hợp trong `product-service` để bắt chính xác các từ khóa thương hiệu, danh mục (như *"áo sơ mi"*, *"wool"*).
   * Hoặc có thể kích hoạt tính năng **Qdrant Sparse Vector** (BM25) để thực hiện hybrid search trực tiếp trên một cơ sở dữ liệu duy nhất.
3. **Metadata Filter (Lọc thuộc tính):** Lọc cứng dựa trên thông tin kích cỡ, màu sắc, khoảng giá và trạng thái tồn kho thực tế lấy từ database nghiệp vụ.
4. **Graph Traversal (Duyệt đồ thị):** Sử dụng Neo4j để lấy các sản phẩm có mối quan hệ thời trang. Ví dụ, nếu khách tìm đồ phối với *"Áo blazer"*, Neo4j sẽ duyệt quan hệ `[:CAN_PAIR_WITH]` để tìm ra *"Quần âu"* hoặc *"Áo sơ mi"*.
5. **Re-ranking (Tái xếp hạng):** Tổng hợp điểm số từ các nguồn và tính điểm cuối cùng:
   $$\text{Final Score} = (\text{Vector Score} \times 0.35) + (\text{Keyword Score} \times 0.25) + (\text{Graph Score} \times 0.25) + (\text{Personalization Score} \times 0.15)$$

### 9.2. Giới hạn hoạt động AI (AI Control Parameters)
* **AI Rate Limiting:** Gateway giới hạn tối đa **5 tin nhắn/phút** cho mỗi `userId` hoặc IP đối với API `/api/ai/chat` để tránh spam và quá tải chi phí LLM.
* **Top-K Retrieval:** Giới hạn chỉ lấy tối đa **Top 10** sản phẩm có điểm tương đồng cao nhất từ Vector DB/Graph để đưa vào Re-ranking.
* **Rerank Threshold:** Loại bỏ toàn bộ sản phẩm có `Final Score` dưới **0.65** trước khi đưa vào gợi ý.
* **Context Window Limit:** Giới hạn lịch sử hội thoại tối đa **6 lượt chat gần nhất** và khống chế tổng độ dài ngữ cảnh gửi vào LLM không quá **4,096 tokens** cho mỗi request để tối ưu tốc độ phản hồi.

---

## 10. Quản lý Index dữ liệu AI qua bảng `ai_index_jobs`

Đồng bộ dữ liệu sang Qdrant và Neo4j được quản lý bởi `ai-agent-service` qua bảng `ai_index_jobs` để tránh mất mát dữ liệu khi hệ thống AI gặp sự cố.

### Các thuộc tính chi tiết của bảng `ai_index_jobs`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | VARCHAR(50) | PRIMARY KEY | UUID định danh duy nhất cho công việc. |
| `target_type` | VARCHAR(30) | NOT NULL | Loại thực thể cần index (`PRODUCT`, `INVENTORY`, `RULE`). |
| `target_id` | VARCHAR(50) | NOT NULL | ID của đối tượng ở database nghiệp vụ (ví dụ: `P001`). |
| `operation_type`| VARCHAR(10) | NOT NULL | Hành động nghiệp vụ (`CREATE`, `UPDATE`, `DELETE`). |
| `status` | VARCHAR(20) | NOT NULL | Trạng thái công việc: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`. |
| `retry_count` | INT | DEFAULT 0 | Số lần đã thử lại khi gặp lỗi (tối đa 3 lần). |
| `last_error_message`| TEXT | NULL | Ghi lại dấu vết lỗi ngoại lệ (exception) để admin debug. |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Thời điểm tạo yêu cầu index. |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Thời điểm cập nhật trạng thái gần nhất. |

### Quy trình xử lý lỗi và Retry Policy (Exponential Backoff)
1. **Ghi nhận lỗi:** Khi việc đẩy dữ liệu lên Qdrant/Neo4j gặp lỗi (timeout, mất kết nối), job được đổi trạng thái thành `FAILED` và ghi lỗi vào cột `last_error_message`.
2. **Exponential Backoff Retry:** Một scheduler chạy ngầm sẽ quét các job trạng thái `FAILED` có `retry_count` < 3. Khoảng thời gian chờ giữa các lần thử lại tăng dần theo công thức: 
   $$\text{Delay} = 2^{\text{retry\_count}} \times 5 \text{ giây} \quad (\text{Lần 1: 10s}, \text{Lần 2: 20s}, \text{Lần 3: 40s})$$
3. **Dead Letter Alert:** Nếu sau 3 lần thử lại vẫn thất bại, status giữ nguyên là `FAILED` và gửi cảnh báo đến quản trị viên qua `notification-service`.

---

## 11. Chiến lược lưu trữ ảnh sản phẩm (Image Storage Lifecycle)

Hình ảnh sản phẩm được lưu trữ tập trung tại Object Storage tương thích S3 (sử dụng **MinIO** chạy Docker ở môi trường local/MVP, sử dụng **AWS S3** hoặc **Cloudflare R2** khi chạy Production).

```text
A. Tải ảnh lên (Upload Lifecycle)
[Admin UI] -> Tải file ảnh lên -> [API Gateway] -> [product-service]
                                                      |
                                                      v (Upload File)
                                             [S3 Bucket: 'stylemind-products']
                                                      |
                                                      v (Trả về CDN/Access URL)
                                             [https://s3.stylemind.ai/images/p001.jpg]
                                                      |
                                                      v (Lưu URL vào DB)
                                             [product_db.product_images]

B. Cập nhật và Xóa ảnh (Update/Delete Lifecycle)
- Khi Admin xóa sản phẩm/ảnh -> product-service xóa bản ghi trong product_images.
- Phát lệnh xóa đối tượng tương ứng trên S3 Bucket qua S3 SDK (sử dụng API client s3.deleteObject).
```

### Quy trình tạo URL tạm thời (Presigned URL)
* Các hình ảnh công khai của sản phẩm sẽ được cấu hình bucket ở chế độ **Public Read** để truy cập trực tiếp qua CDN/URL tĩnh nhằm tối ưu hiệu năng.
* Đối với ảnh nhạy cảm (như hóa đơn thanh toán, ảnh cá nhân của khách hàng trong style profile), bucket được đặt ở chế độ **Private**. `product-service` hoặc `user-service` sẽ tạo **S3 Presigned URL** có thời gian hết hạn ngắn (**15 phút**) để gửi về cho frontend hiển thị, đảm bảo tính bảo mật.

---

## 12. Hạ tầng Logging, Tracing & Giám sát hệ thống

### A. Distributed Tracing với `X-Request-Id`
* **Sinh mã định danh:** API Gateway tự động tạo ra một chuỗi UUID duy nhất và gán vào header `X-Request-Id` cho mỗi request đầu vào từ client.
* **Lan truyền mã tracing:**
  - Gateway đính kèm `X-Request-Id` vào request gửi đến microservice đầu tiên.
  - Khi microservice này gọi microservice khác qua OpenFeign Client, cấu hình **Feign RequestInterceptor** sẽ tự động sao chép tiêu đề `X-Request-Id` từ Context hiện tại của Spring (ThreadLocal) và chèn vào request tiếp theo.
* **Ghi nhật ký (Logging):** Mọi dòng log ghi ra hệ thống console/file ở tất cả các dịch vụ đều phải có định dạng chứa `X-Request-Id` trong cấu hình Pattern của Logback:
  `%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{X-Request-Id}] [%thread] %-5level %logger{36} - %msg%n`
  Điều này giúp quản trị viên chỉ cần tìm kiếm theo `X-Request-Id` là có thể truy vết toàn bộ hành trình xử lý đi qua nhiều service.

### B. Giám sát với Spring Boot Actuator
Tất cả các dịch vụ nghiệp vụ Spring Boot đều phải cấu hình thư viện `spring-boot-starter-actuator` để hiển thị các endpoint giám sát:
* `/actuator/health`: Kiểm tra sức khỏe của dịch vụ, kết nối đến database, kết nối đến Qdrant/Neo4j. Cấu hình hiển thị chi tiết: `management.endpoint.health.show-details=always`.
* `/actuator/metrics`: Cung cấp các thông số hệ thống như số lượng request, thời gian phản hồi trung bình, tỷ lệ lỗi 5xx, mức tiêu thụ tài nguyên RAM/CPU của JVM.

---

## 13. Sơ đồ các luồng nghiệp vụ chính (Text Diagrams)

### Luồng 1: Đăng nhập hệ thống (User Login)
```text
[Frontend] --(POST /auth/login {email, pwd})--> [API Gateway]
                                                      |
                                                      | (Chặn & Strip X-User-Id từ ngoài)
                                                      v
                                                [auth-service]
                                                      | (Kiểm tra email & bcrypt hash)
                                                      |-- Sinh JWT chứa userId, roles
                                                      v
[Frontend] <--(Trả về token & user info)--------------+
```

### Luồng 2: Xem chi tiết sản phẩm (View Product Detail)
```text
[Frontend] --(GET /products/:id)--> [API Gateway] --(Chuyển tiếp)--> [product-service]
                                                                          |
                                                                          |-- 1. Query sản phẩm & URLs ảnh trong product_db
                                                                          v
[Frontend] <--(Trả về thông tin chi tiết sản phẩm)------------------------+
```

### Luồng 3: Thêm sản phẩm vào giỏ (Add to Cart - Guest & User)
```text
[Frontend] --(POST /cart {productId, quantity, ...})--> [API Gateway]
                                                             |
                                                             |-- Giải mã JWT (nếu có)
                                                             v
                                                       [cart-service]
                                                             |
                                                             |-- 1. Nếu có X-User-Id -> Lưu vào cart của User
                                                             |-- 2. Nếu không có (chỉ có guestSessionId) -> Lưu vào cart của Guest
                                                             v
[Frontend] <--(Trả về trạng thái giỏ hàng mới)---------------+
```

### Luồng 4: Đặt hàng và Giữ kho (Place Order & Inventory Reservation)
```text
[Frontend] --(POST /orders {items, total, ...})--> [API Gateway] --(Chuyển tiếp)--> [order-service]
                                                                                         |
                                                                                         |-- 1. Tạo đơn hàng với status 'PENDING'
                                                                                         v
                                                                             [inventory-service]
                                                                                         |
                                                                                         |-- 2. Gọi API giữ kho (Reservation)
                                                                                         |   - Kiểm tra tồn kho thực tế.
                                                                                         |   - Tạo bản ghi 'PENDING_RESERVE' trong DB.
                                                                                         |   - Cộng dồn 'reserved_stock'.
                                                                                         v
                                                                                   [order-service]
                                                                                         |
                                                                                         |-- 3. Đổi trạng thái đơn hàng sang 'CONFIRMED'
                                                                                         |   - Đổi trạng thái giữ kho sang 'RESERVED'.
                                                                                         v
[Frontend] <--(Trả về đơn hàng đã xác nhận thành công)-----------------------------------+
```

### Luồng 5: Xử lý thanh toán (Payment Process)
```text
[Frontend] --(POST /payment/checkout)--> [API Gateway] --(Chuyển tiếp)--> [payment-service]
                                                                               |
                                                                               |-- 1. Gọi cổng thanh toán hoặc giả lập
                                                                               |-- 2. Tạo bản ghi transaction 'COMPLETED'
                                                                               v
                                                                        [order-service]
                                                                               |
                                                                               |-- 3. Gọi REST API cập nhật trạng thái đơn
                                                                               |   - Cập nhật order sang 'PROCESSING'.
                                                                               v
                                                                       [inventory-service]
                                                                               |
                                                                               |-- 4. Gọi REST API xác nhận kho (Commit)
                                                                               |   - Chuyển reservation sang 'COMMITTED'.
                                                                               |   - Giảm trừ 'current_stock' và 'reserved_stock'.
                                                                               v
[Frontend] <--(Trả về kết quả thanh toán thành công)---------------------------+
```

### Luồng 6: AI Agent tư vấn sản phẩm (AI Product Consultation - Chống ảo giác)
```text
[Frontend] --(POST /api/ai/chat {prompt})--> [API Gateway] --(Xác thực & Forward)--> [ai-agent-service]
                                                                                          |
                                                                                          |-- 1. Gọi Qdrant Vector Search
                                                                                          |-- 2. Gọi Neo4j Graph Traversal
                                                                                          |   - Tìm các sản phẩm phù hợp.
                                                                                          v
                                                                                   [product-service]
                                                                                          |
                                                                                          |-- 3. Gọi Runtime REST lấy thông tin động:
                                                                                          |   - Lấy giá bán chính xác của sản phẩm.
                                                                                          v
                                                                                 [inventory-service]
                                                                                          |
                                                                                          |-- 4. Gọi Runtime REST kiểm tra tồn kho:
                                                                                          |   - Kiểm tra còn hàng / còn size không.
                                                                                          v
                                                                                   [ai-agent-service]
                                                                                          |
                                                                                          |-- 5. Tổng hợp context chính xác.
                                                                                          |-- 6. Gửi tới LLM để sinh câu trả lời.
                                                                                          v
[Frontend] <--(Câu trả lời tự nhiên kèm đề xuất sản phẩm thực tế)---------------+
```

### Luồng 7: AI Agent kiểm tra hành trình đơn hàng (AI Order Tracking)
```text
[Frontend] --(POST /api/ai/chat {prompt: "Đơn ORD-999 của tôi thế nào?"})--> [API Gateway]
                                                                                 |
                                                                                 |-- Giải mã JWT lấy X-User-Id = "usr_123"
                                                                                 v
                                                                         [ai-agent-service]
                                                                                 |
                                                                                 v [REST API call]
                                                                          [order-service]
                                                                                 |
                                                                                 |-- 1. Kiểm tra đơn hàng ORD-999.
                                                                                 |-- 2. Đối chiếu: ORD-999.userId == "usr_123"?
                                                                                 |   - Nếu khớp: Trả về trạng thái 'SHIPPED'.
                                                                                 |   - Nếu lệch: Từ chối truy cập (403).
                                                                                 v
                                                                         [ai-agent-service]
                                                                                 |
                                                                                 |-- 3. LLM nhận context trạng thái thực tế.
                                                                                 |-- 4. Sinh câu trả lời bảo mật.
                                                                                 v
[Frontend] <--(Trả về thông tin trạng thái đơn hàng của chính user đó)-----------+
```
