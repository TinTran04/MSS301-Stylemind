# HỢP ĐỒNG API HỆ THỐNG STYLEMIND (API CONTRACT)

Tài liệu này định nghĩa chi tiết các đặc tả giao tiếp API giữa Frontend và các dịch vụ Backend thông qua API Gateway, cũng như các API nội bộ phục vụ giao tiếp liên dịch vụ (Service-to-Service). **Tất cả các response/request structure tuân thủ DATA_MODEL_DOCUMENTATION.**

---

## 1. Chuẩn hóa định dạng phản hồi (Response Format)

Toàn bộ hệ thống Microservices thống nhất cấu trúc dữ liệu trả về theo chuẩn JSON như sau:

### 1.1. Phản hồi thành công (Success Response - HTTP Status 2xx)
```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": {
    "key": "value"
  },
  "timestamp": "2026-06-14T20:21:40.000Z"
}
```

### 1.2. Phản hồi lỗi (Error Response - HTTP Status 4xx, 5xx)
```json
{
  "success": false,
  "errorCode": "ERROR_CODE_STRING",
  "message": "Mô tả chi tiết lỗi dành cho lập trình viên/người dùng đọc",
  "timestamp": "2026-06-14T20:21:40.000Z"
}
```

#### Các mã lỗi hệ thống chuẩn hóa (ErrorCode):
* `AUTH_INVALID_CREDENTIALS`: Email hoặc mật khẩu đăng nhập không đúng.
* `AUTH_TOKEN_EXPIRED`: JWT token hết hạn, yêu cầu refresh hoặc đăng nhập lại.
* `AUTH_ACCESS_DENIED`: Thiếu quyền truy cập vào tài nguyên (ví dụ: Customer truy cập admin API).
* `PRODUCT_NOT_FOUND`: Không tìm thấy sản phẩm.
* `VARIANT_NOT_FOUND`: Không tìm thấy biến thể sản phẩm.
* `CART_ITEM_NOT_FOUND`: Sản phẩm trong giỏ hàng không tồn tại.
* `ORDER_OUT_OF_STOCK`: Số lượng tồn kho thực tế không đủ cho đơn hàng.
* `ORDER_NOT_FOUND`: Không tìm thấy đơn hàng.
* `PAYMENT_DECLINED`: Giao dịch bị từ chối bởi ngân hàng/cổng thanh toán.
* `AI_RATE_LIMIT_EXCEEDED`: User gửi vượt quá 5 câu hỏi/phút cho AI Agent.
* `INTERNAL_ERROR`: Lỗi không xác định ở hệ thống backend.

---

## 2. Bảo mật API nội bộ (`/internal/**`)

Các API có đường dẫn bắt đầu bằng `/internal/**` được dành riêng cho các microservice giao tiếp với nhau (ví dụ: `ai-agent-service` gọi `product-service` để lấy giá và tồn kho thời gian thực).
* **Không public ra ngoài:** API Gateway được cấu hình để chặn tất cả các cuộc gọi từ bên ngoài (Internet) có tiền tố `/internal/**`.
* **Xác thực Service-to-Service:** Các dịch vụ khi gọi qua OpenFeign bắt buộc phải gửi kèm Header xác thực:
  `X-Internal-Token: sm-secret-internal-service-token-key-2026`
  Nếu thiếu hoặc token không khớp, trả về lỗi `401 Unauthorized`.

---

## 3. Danh sách API dành cho Frontend (Public & Customer APIs)

Tất cả các API dưới đây đều được gọi thông qua API Gateway tại `http://localhost:3000/api`.

### 3.1. Phân quyền và Xác thực (`auth-service`)

#### A. Đăng nhập
* **Method:** `POST`
* **Path:** `/auth/login`
* **Auth Required:** No
* **Request Body:**
```json
{
  "email": "julianne@example.com",
  "password": "securepassword123"
}
```
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "token": "eyJhbG...VCJ9...",
    "user": {
      "id": "usr_001",
      "email": "julianne@example.com",
      "full_name": "Julianne Deauville",
      "role": "customer",
      "provider": "LOCAL"
    }
  },
  "timestamp": "2026-06-14T20:22:00.000Z"
}
```

#### B. Đăng ký tài khoản
* **Method:** `POST`
* **Path:** `/auth/register`
* **Auth Required:** No
* **Request Body:**
```json
{
  "name": "Julianne Deauville",
  "email": "julianne@example.com",
  "password": "securepassword123"
}
```

#### C. Lấy thông tin user hiện tại
* **Method:** `GET`
* **Path:** `/auth/me`
* **Auth Required:** Yes (JWT Bearer Token)
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Lấy thông tin người dùng thành công",
  "data": {
    "id": "usr_001",
    "email": "julianne@example.com",
    "full_name": "Julianne Deauville",
    "role": "customer",
    "provider": "LOCAL",
    "created_at": "2026-01-15T10:00:00.000Z"
  },
  "timestamp": "2026-06-14T20:22:10.000Z"
}
```

---

### 3.2. Quản lý Style Profile & Địa chỉ (`user-service`)

#### A. Lấy Style Profile của User
* **Method:** `GET`
* **Path:** `/users/profile`
* **Auth Required:** Yes (JWT Bearer Token)
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Lấy thông tin hồ sơ thành công",
  "data": {
    "user_id": "usr_001",
    "gender": "Female",
    "age": 28,
    "height_cm": 165.5,
    "weight_kg": 52.0,
    "body_morphology": "Hourglass",
    "preferred_fit": "Slim-fit",
    "style_personas": ["Minimalist", "Classic"]
  },
  "timestamp": "2026-06-14T20:22:10.000Z"
}
```

#### B. Cập nhật Style Profile
* **Method:** `PUT`
* **Path:** `/users/profile`
* **Auth Required:** Yes (JWT Bearer Token)
* **Request Body:**
```json
{
  "gender": "Female",
  "age": 28,
  "height_cm": 165.5,
  "weight_kg": 52.0,
  "body_morphology": "Hourglass",
  "preferred_fit": "Slim-fit",
  "style_personas": ["Minimalist", "Classic"]
}
```

#### C. Lấy danh sách địa chỉ giao hàng
* **Method:** `GET`
* **Path:** `/users/addresses`
* **Auth Required:** Yes (JWT Bearer Token)
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Lấy danh sách địa chỉ thành công",
  "data": [
    {
      "id": "addr_001",
      "user_id": "usr_001",
      "recipient_name": "Julianne Deauville",
      "phone_number": "0901234567",
      "address_line": "123 Đường ABC, Phường XYZ",
      "city": "Hồ Chí Minh",
      "is_default": true
    }
  ],
  "timestamp": "2026-06-14T20:22:15.000Z"
}
```

#### D. Thêm/Cập nhật/Xóa địa chỉ giao hàng
* **Method:** `POST` / `PUT` / `DELETE`
* **Path:** `/users/addresses` / `/users/addresses/{id}`
* **Auth Required:** Yes (JWT Bearer Token)

---

### 3.3. Duyệt sản phẩm (`product-service`)

#### A. Lấy danh sách sản phẩm (Có tìm kiếm & bộ lọc)
* **Method:** `GET`
* **Path:** `/products`
* **Auth Required:** No
* **Query Parameters:** `category` (string), `search` (string), `minPrice` (decimal), `maxPrice` (decimal), `sort` (price_asc, price_desc, rating, ai_match), `status` (ACTIVE, INACTIVE - default ACTIVE)
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Lấy danh sách sản phẩm thành công",
  "data": [
    {
      "id": "P001",
      "category_id": 1,
      "name": "Silk Midi Dress",
      "description": "Flowing silk midi dress with draped neckline...",
      "base_price": 285000.00,
      "aesthetic_style": "Romantic",
      "target_demographic": "Women",
      "seasonal_property": "Spring",
      "status": "ACTIVE",
      "images": [
        {
          "id": 1,
          "image_url": "https://s3.stylemind.ai/images/p001.jpg",
          "is_primary": true
        }
      ],
      "variants": [
        {
          "id": "P001_S_Ivory",
          "sku": "SM-DRESS-001-S-IVORY",
          "size": "S",
          "color": "Ivory",
          "material": "Silk",
          "price_override": null
        }
      ],
      "created_at": "2026-01-15T10:00:00.000Z",
      "updated_at": "2026-01-15T10:00:00.000Z"
    }
  ],
  "timestamp": "2026-06-14T20:22:20.000Z"
}
```

#### B. Chi tiết sản phẩm
* **Method:** `GET`
* **Path:** `/products/:id`
* **Auth Required:** No
* **Response:** Tương tự item trong list nhưng đầy đủ variants và images

#### C. Lấy danh mục sản phẩm
* **Method:** `GET`
* **Path:** `/categories`
* **Auth Required:** No

---

### 3.4. Quản lý Giỏ hàng (`cart-service`)

#### A. Lấy giỏ hàng hiện tại (Hỗ trợ Guest Session)
* **Method:** `GET`
* **Path:** `/cart`
* **Auth Required:** No (Nhận dạng bằng JWT Header nếu đã đăng nhập, hoặc gửi kèm Header `X-Guest-Session-Id` nếu là khách vãng lai)
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Lấy giỏ hàng thành công",
  "data": {
    "cart_id": "usr_001",
    "items": [
      {
        "id": "cart_item_001",
        "cart_id": "usr_001",
        "variant_id": "P001_S_Ivory",
        "quantity": 1,
        "is_ai_recommended": true,
        "source_bundle_id": "bundle_001",
        "variant": {
          "id": "P001_S_Ivory",
          "sku": "SM-DRESS-001-S-IVORY",
          "size": "S",
          "color": "Ivory",
          "material": "Silk",
          "price_override": null,
          "product": {
            "id": "P001",
            "name": "Silk Midi Dress",
            "base_price": 285000.00,
            "images": [{"image_url": "https://s3.stylemind.ai/images/p001.jpg", "is_primary": true}]
          }
        }
      }
    ]
  },
  "timestamp": "2026-06-14T20:22:30.000Z"
}
```

#### B. Thêm sản phẩm vào giỏ
* **Method:** `POST`
* **Path:** `/cart`
* **Auth Required:** No (Cần Header `Authorization: Bearer ***` hoặc `X-Guest-Session-Id`)
* **Request Body:**
```json
{
  "variant_id": "P001_S_Ivory",
  "quantity": 1
}
```

#### C. Cập nhật số lượng
* **Method:** `PUT`
* **Path:** `/cart/{itemId}`
* **Request Body:** `{ "quantity": 2 }`

#### D. Xóa khỏi giỏ hàng
* **Method:** `DELETE`
* **Path:** `/cart/{itemId}`

#### E. Gộp giỏ hàng (Merge Cart)
* **Method:** `POST`
* **Path:** `/cart/merge`
* **Auth Required:** Yes (JWT Bearer Token)
* **Request Body:**
```json
{
  "guest_session_id": "gst_99c591ac-3fbd-4e9e-95e2-5b8436a3cb63"
}
```

---

### 3.5. Xử lý Đơn hàng (`order-service`)

#### A. Tạo đơn hàng mới
* **Method:** `POST`
* **Path:** `/orders`
* **Auth Required:** Yes (JWT Bearer Token)
* **Request Body:**
```json
{
  "shipping_address": "123 Đường ABC, Quận 1, TP. HCM",
  "payment_method": "online_simulated",
  "transaction_id": "tx_20260614_88899"
}
```
* **Response Thành công (201 Created):**
```json
{
  "success": true,
  "message": "Đơn hàng đã được tạo thành công",
  "data": {
    "order_id": "ORD-2026-003",
    "user_id": "usr_001",
    "total_amount": 685000.00,
    "order_status": "PENDING",
    "shipping_address": "123 Đường ABC, Quận 1, TP. HCM",
    "created_at": "2026-06-14T20:22:40.000Z",
    "updated_at": "2026-06-14T20:22:40.000Z",
    "items": [
      {
        "id": "oi_001",
        "variant_id": "P001_S_Ivory",
        "quantity": 1,
        "price_at_purchase": 285000.00,
        "is_ai_conversion": true,
        "source_bundle_id": "bundle_001",
        "variant": {
          "id": "P001_S_Ivory",
          "sku": "SM-DRESS-001-S-IVORY",
          "size": "S",
          "color": "Ivory",
          "product": {
            "id": "P001",
            "name": "Silk Midi Dress"
          }
        }
      }
    ]
  },
  "timestamp": "2026-06-14T20:22:45.000Z"
}
```

#### B. Lấy danh sách đơn hàng của user
* **Method:** `GET`
* **Path:** `/orders`
* **Auth Required:** Yes (JWT Bearer Token)

#### C. Chi tiết đơn hàng
* **Method:** `GET`
* **Path:** `/orders/:id`
* **Auth Required:** Yes (JWT Bearer Token)

---

### 3.6. Hội thoại và tư vấn thời trang AI (`ai-agent-service`)

#### A. Gửi tin nhắn tư vấn phong cách
* **Method:** `POST`
* **Path:** `/ai-stylist/chat`
* **Auth Required:** No (Nếu có JWT gửi kèm sẽ cá nhân hóa theo Style DNA của user)
* **Request Body:**
```json
{
  "message": "Tôi cần tìm áo sơ mi đi làm màu trắng dưới 500k",
  "conversation_id": "conv_001"
}
```
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "AI tư vấn thành công",
  "data": {
    "conversation_id": "conv_001",
    "message_id": "msg_001",
    "sender_type": "AI",
    "message_text": "Dựa trên yêu cầu và dáng người Hourglass của bạn, tôi gợi ý mẫu áo Oxford Cotton cao cấp. Nó lịch sự và thoáng mát.",
    "has_product_block": true,
    "intent": "product_recommendation",
    "recommended_products": [
      {
        "product_id": "P101",
        "name": "Áo sơ mi trắng Oxford",
        "base_price": 459000.00,
        "image_url": "https://s3.stylemind.ai/images/p101.jpg",
        "reason": "Phù hợp phong cách công sở của bạn, chất vải Oxford thoáng khí.",
        "match_score": 0.92
      }
    ],
    "style_tips": [
      "Kết hợp cùng quần tây đen hoặc beige.",
      "Sơ vin gọn gàng để tăng vẻ chuyên nghiệp."
    ],
    "curated_bundle": {
      "id": "bundle_001",
      "justification_summary": "Set đồ lịch sự phù hợp với dáng Hourglass và môi trường văn phòng",
      "items": [
        {"product_id": "P101"},
        {"product_id": "P102"}
      ]
    }
  },
  "timestamp": "2026-06-14T20:23:00.000Z"
}
```

#### B. Giải thích đề xuất thời trang (Explain Recommendation)
* **Method:** `POST`
* **Path:** `/ai-stylist/explain`
* **Auth Required:** No
* **Request Body:**
```json
{
  "product_id": "P101",
  "user_context": {
    "body_morphology": "Hourglass",
    "style_personas": ["Minimalist"],
    "preferred_colors": ["Black", "White"]
  }
}
```

#### C. Gợi ý Outfit theo sự kiện
* **Method:** `POST`
* **Path:** `/ai-stylist/recommend-outfits`
* **Auth Required:** No
* **Request Body:**
```json
{
  "occasion": "wedding",
  "style": "elegant",
  "gender": "female",
  "budget": 1500000.00,
  "preferred_colors": ["Black", "Beige"]
}
```

#### D. Lịch sử hội thoại
* **Method:** `GET`
* **Path:** `/ai-stylist/history`
* **Auth Required:** Yes (JWT Bearer Token)

---

### 3.7. Thanh toán (`payment-service`)

#### A. Khởi tạo thanh toán
* **Method:** `POST`
* **Path:** `/payment/checkout`
* **Auth Required:** Yes (JWT Bearer Token)
* **Request Body:**
```json
{
  "order_id": "ORD-2026-003",
  "method": "online_simulated",
  "amount": 685000.00
}
```

---

## 4. Các API quản trị dành cho Quản trị viên (Admin APIs)

Tất cả các API này yêu cầu Header JWT chứa `role=ADMIN`.

### 4.1. Quản lý sản phẩm (`product-service`)
* `POST /api/admin/products`: Tạo sản phẩm mới (Tải kèm ảnh lên S3/MinIO).
* `PUT /api/admin/products/:id`: Cập nhật thông tin chi tiết (bao gồm base_price, aesthetic_style, target_demographic, seasonal_property, status).
* `DELETE /api/admin/products/:id`: Xóa sản phẩm (Tự động kích hoạt xóa đối tượng trên S3/MinIO và gửi tín hiệu xóa index ở AI qua ai_index_jobs).
* `POST /api/admin/categories`: Tạo danh mục mới (có parent_id để hỗ trợ cây).
* `PUT /api/admin/categories/:id`: Cập nhật danh mục.
* `POST /api/admin/products/:id/variants`: Thêm biến thể (sku, size, color, material, price_override).
* `POST /api/admin/products/:id/images`: Thêm hình ảnh sản phẩm.

### 4.2. Quản lý đơn hàng (`order-service`)
* `GET /api/admin/orders`: Lấy toàn bộ danh sách đơn hàng (filter theo status: PENDING, PROCESSING, COMPENSATING_ROLLBACK, FULFILLED, CANCELLED).
* `PUT /api/admin/orders/:id/status`: Cập nhật trạng thái đơn hàng (ví dụ: chuyển sang PROCESSING, FULFILLED, CANCELLED).
  - Payload: `{ "order_status": "FULFILLED" }`

### 4.3. Quản lý khách hàng (`user-service`)
* `GET /api/admin/users`: Danh sách user kèm role, created_at.
* `GET /api/admin/users/:id/profile`: Xem Style Profile và Addresses của user.

### 4.4. Giám sát & Quản lý AI (`ai-agent-service`)

#### A. Tra cứu sự kiện chạy nền (AI Pipeline Events)
* **Method:** `GET`
* **Path:** `/api/admin/ai/pipeline/events`
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Lấy logs sự kiện AI Pipeline thành công",
  "data": [
    {
      "event_id": "evt_998",
      "event_type": "VECTOR_EMBEDDING_GENERATION",
      "target_id": "P101",
      "status": "SUCCESS",
      "duration_ms": 450,
      "timestamp": "2026-06-14T19:00:00.000Z"
    }
  ],
  "timestamp": "2026-06-14T20:24:00.000Z"
}
```

#### B. Kiểm tra Đồ thị tri thức (Knowledge Graph Management)
* **Method:** `GET`
* **Path:** `/api/admin/ai/graph/status`

#### C. Tra cứu nhật ký gợi ý (Recommendation Logs / Analytics)
* **Method:** `GET`
* **Path:** `/api/admin/ai/analytics-logs`
* **Query Parameters:** `user_id` (string), `bundle_id` (string), `interaction_type` (IMPRESSION, CLICK, ADD_TO_CART), `limit` (int)
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Lấy logs tương tác AI thành công",
  "data": [
    {
      "id": "log_001",
      "user_id": "usr_001",
      "bundle_id": "bundle_001",
      "interaction_type": "CLICK",
      "created_at": "2026-06-14T18:30:00.000Z"
    }
  ],
  "timestamp": "2026-06-14T20:24:20.000Z"
}
```

#### D. Quản lý Index Job
* `GET /api/admin/ai/index-jobs`: Lấy danh sách job index (PENDING, PROCESSING, COMPLETED, FAILED).
* `POST /api/admin/ai/index-jobs/retry`: Retry các job FAILED.
* `POST /api/admin/ai/index/products/reindex`: Trigger reindex toàn bộ sản phẩm.

---

## 5. API Nội bộ (Internal APIs - Service to Service)

Các API này chỉ được gọi qua mạng nội bộ với header `X-Internal-Token`.

### 5.1. Product Service (Internal)
* `GET /internal/products/:id` → Trả về product chi tiết (id, name, base_price, variants[], images[])
* `GET /internal/products/:id/variants` → Trả về danh sách variants
* `GET /internal/products/by-skus?skus=sku1,sku2` → Lấy nhiều sản phẩm theo SKU cho checkout

### 5.2. Inventory Service (Internal)
* `GET /internal/inventory/:sku` → Trả về { sku, current_stock, reserved_stock }
* `POST /internal/inventory/reserve` → Body: { order_id, items: [{sku, quantity}] } → Trả về reservation_id
* `POST /internal/inventory/commit` → Body: { reservation_id } → Xác nhận trừ kho
* `POST /internal/inventory/release` → Body: { reservation_id } → Giải phóng kho

### 5.3. Order Service (Internal)
* `GET /internal/orders/:id` → Trả về order detail (kèm ownership check)
* `GET /internal/orders/:id/items` → Trả về order_items
* `PUT /internal/orders/:id/status` → Cập nhật status từ payment-service

### 5.4. AI Agent Service (Internal) - Called by other services for indexing
* `POST /internal/ai/index/products/:id` → Trigger index single product
* `POST /internal/ai/index/products/reindex` → Trigger full reindex
