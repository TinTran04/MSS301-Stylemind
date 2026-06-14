# HỢP ĐỒNG API HỆ THỐNG STYLEMIND (API CONTRACT)

Tài liệu này định nghĩa chi tiết các đặc tả giao tiếp API giữa Frontend và các dịch vụ Backend thông qua API Gateway, cũng như các API nội bộ phục vụ giao tiếp liên dịch vụ (Service-to-Service).

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
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "usr_001",
      "name": "Julianne Deauville",
      "email": "julianne@example.com",
      "role": "customer"
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

---

### 3.2. Quản lý Style Profile (`user-service`)

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
    "userId": "usr_001",
    "tier": "Platinum",
    "stylePreferences": ["Minimalist", "Classic"],
    "bodyType": "Slim",
    "fitPreference": "Slim",
    "favoriteColors": ["Black", "Cream", "Navy"],
    "sizeProfile": {
      "top": "S",
      "bottom": "S",
      "shoe": "38"
    }
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
  "stylePreferences": ["Minimalist", "Classic"],
  "bodyType": "Slim",
  "fitPreference": "Slim",
  "favoriteColors": ["Black", "Cream", "Navy"],
  "sizeProfile": {
    "top": "S",
    "bottom": "S",
    "shoe": "38"
  }
}
```

---

### 3.3. Duyệt sản phẩm (`product-service`)

#### A. Lấy danh sách sản phẩm (Có tìm kiếm & bộ lọc)
* **Method:** `GET`
* **Path:** `/products`
* **Auth Required:** No
* **Query Parameters:** `category` (string), `search` (string), `minPrice` (decimal), `maxPrice` (decimal), `sort` (price_asc, price_desc, rating, ai_match)
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Lấy danh sách sản phẩm thành công",
  "data": [
    {
      "id": "P001",
      "name": "Silk Midi Dress",
      "price": 285000.00,
      "originalPrice": 380000.00,
      "category": "Dresses",
      "description": "Flowing silk midi dress with draped neckline...",
      "images": ["https://s3.stylemind.ai/images/p001.jpg"],
      "colors": ["Ivory", "Black", "Champagne"],
      "sizes": ["XS", "S", "M", "L"],
      "sku": "SM-DRESS-001",
      "rating": 4.80,
      "isNew": true
    }
  ],
  "timestamp": "2026-06-14T20:22:20.000Z"
}
```

#### B. Chi tiết sản phẩm
* **Method:** `GET`
* **Path:** `/products/:id`
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
    "cartId": "usr_001", -- hoặc "gst_99c591ac..."
    "items": [
      {
        "cartItemId": 12,
        "productId": "P001",
        "name": "Silk Midi Dress",
        "price": 285000.00,
        "quantity": 1,
        "size": "M",
        "color": "Ivory",
        "imageUrl": "https://s3.stylemind.ai/images/p001.jpg"
      }
    ]
  },
  "timestamp": "2026-06-14T20:22:30.000Z"
}
```

#### B. Thêm sản phẩm vào giỏ
* **Method:** `POST`
* **Path:** `/cart`
* **Auth Required:** No (Cần Header `Authorization: Bearer <token>` hoặc `X-Guest-Session-Id`)
* **Request Body:**
```json
{
  "productId": "P001",
  "quantity": 1,
  "size": "M",
  "color": "Ivory"
}
```

#### C. Gộp giỏ hàng (Merge Cart)
* **Method:** `POST`
* **Path:** `/cart/merge`
* **Auth Required:** Yes (JWT Bearer Token)
* **Request Body:**
```json
{
  "guestSessionId": "gst_99c591ac-3fbd-4e9e-95e2-5b8436a3cb63"
}
```
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Gộp giỏ hàng vãng lai thành công",
  "data": {
    "cartId": "usr_001",
    "totalMergedItems": 3
  },
  "timestamp": "2026-06-14T20:22:35.000Z"
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
  "shippingAddress": "123 Đường ABC, Quận 1, TP. HCM",
  "paymentMethod": "online_simulated", -- cod, online_simulated
  "transactionId": "tx_20260614_88899" -- Bắt buộc nếu là online payment
}
```
* **Response Thành công (201 Created):**
```json
{
  "success": true,
  "message": "Đơn hàng đã được tạo thành công",
  "data": {
    "orderId": "ORD-2026-003",
    "status": "CONFIRMED",
    "totalAmount": 685000.00,
    "createdAt": "2026-06-14T20:22:40.000Z",
    "timeline": [
      { "status": "PENDING", "date": "2026-06-14T20:22:40.000Z", "completed": true },
      { "status": "CONFIRMED", "date": "2026-06-14T20:22:45.000Z", "completed": true }
    ]
  },
  "timestamp": "2026-06-14T20:22:45.000Z"
}
```

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
  "conversationId": "conv_001" -- Gửi rỗng ở tin nhắn đầu tiên
}
```
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "AI tư vấn thành công",
  "data": {
    "conversationId": "conv_001",
    "message": "Dựa trên yêu cầu và dáng người Slim của bạn, tôi gợi ý mẫu áo Oxford Cotton cao cấp. Nó lịch sự và thoáng mát.",
    "intent": "product_recommendation",
    "recommendedProducts": [
      {
        "productId": "P101",
        "name": "Áo sơ mi trắng Oxford",
        "price": 459000.00,
        "imageUrl": "https://s3.stylemind.ai/images/p101.jpg",
        "reason": "Phù hợp phong cách công sở của bạn, chất vải Oxford thoáng khí.",
        "matchScore": 0.92
      }
    ],
    "styleTips": [
      "Kết hợp cùng quần tây đen hoặc beige.",
      "Sơ vin gọn gàng để tăng vẻ chuyên nghiệp."
    ]
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
  "productId": "P101",
  "userContext": {
    "bodyType": "Slim",
    "stylePreferences": ["Minimalist"],
    "favoriteColors": ["Black", "White"]
  }
}
```
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Phân tích mức độ phù hợp thành công",
  "data": {
    "productId": "P101",
    "matchScore": 0.92,
    "breakdown": {
      "styleFit": 0.95,
      "colorHarmony": 0.90,
      "silhouetteCompatibility": 0.92
    },
    "reasoningFactors": [
      "Kiểu dáng Slim Fit ôm nhẹ, rất tôn dáng người Slim của bạn.",
      "Tông màu trắng thuộc gam màu cơ bản mà bạn yêu thích.",
      "Chất liệu Cotton phù hợp với phong cách Minimalist không cầu kỳ."
    ]
  },
  "timestamp": "2026-06-14T20:23:05.000Z"
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
  "preferredColors": ["Black", "Beige"]
}
```

---

## 4. Các API quản trị dành cho Quản trị viên (Admin APIs)

Tất cả các API này yêu cầu Header JWT chứa `ROLE_ADMIN`.

### 4.1. Quản lý sản phẩm (`product-service`)
* `POST /api/admin/products`: Tạo sản phẩm mới (Tải kèm ảnh lên S3/MinIO).
* `PUT /api/admin/products/:id`: Cập nhật thông tin chi tiết.
* `DELETE /api/admin/products/:id`: Xóa sản phẩm (Tự động kích hoạt xóa đối tượng trên S3/MinIO và gửi tín hiệu xóa index ở AI).

### 4.2. Quản lý tồn kho (`inventory-service`)
* `GET /api/admin/inventory`: Lấy thông tin tồn kho toàn hệ thống.
* `PUT /api/admin/inventory/:productId`: Cập nhật số lượng kho hàng thực tế (`current_stock`).

### 4.3. Quản lý đơn hàng (`order-service`)
* `GET /api/admin/orders`: Lấy toàn bộ danh sách đơn hàng.
* `PUT /api/admin/orders/:id/status`: Cập nhật trạng thái đơn hàng (ví dụ: Chuyển từ `PROCESSING` sang `SHIPPED`).

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
      "eventId": "evt_998",
      "eventType": "VECTOR_EMBEDDING_GENERATION",
      "targetId": "P101",
      "status": "SUCCESS",
      "durationMs": 450,
      "timestamp": "2026-06-14T19:00:00.000Z"
    }
  ],
  "timestamp": "2026-06-14T20:24:00.000Z"
}
```

#### B. Kiểm tra Đồ thị tri thức (Knowledge Graph Management)
* **Method:** `GET`
* **Path:** `/api/admin/ai/graph/status`
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Trạng thái đồ thị tri thức ổn định",
  "data": {
    "totalNodes": 154,
    "totalRelationships": 482,
    "neo4jStatus": "UP"
  },
  "timestamp": "2026-06-14T20:24:10.000Z"
}
```

#### C. Tra cứu nhật ký gợi ý (Recommendation Logs)
* **Method:** `GET`
* **Path:** `/api/admin/ai/recommendation-logs`
* **Query Parameters:** `userId` (string), `limit` (int)
* **Response Thành công (200 OK):**
```json
{
  "success": true,
  "message": "Lấy logs gợi ý thành công",
  "data": [
    {
      "logId": "rec_log_009",
      "userId": "usr_001",
      "prompt": "áo đi làm thoải mái",
      "recommendedProducts": ["P101", "P102"],
      "conversionStatus": "CLICKED", -- SHOWN, CLICKED, CARTED, ORDERED
      "timestamp": "2026-06-14T18:30:00.000Z"
    }
  ],
  "timestamp": "2026-06-14T20:24:20.000Z"
}
```
