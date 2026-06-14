# TÀI LIỆU PHÂN TÍCH DỰ ÁN STYLEMIND (HIỆN TRẠNG)

Tài liệu này cung cấp cái nhìn chi tiết về hiện trạng của dự án **Stylemind** – Hệ thống bán quần áo kết hợp AI Agent tư vấn phong cách. Dựa trên việc phân tích mã nguồn thực tế tại thư mục `FE/`, tài liệu giúp các lập trình viên hoặc sinh viên mới tiếp cận dự án nắm được cấu trúc, công nghệ và các điểm tích hợp API.

---

## 1. Tổng quan dự án hiện tại

Hiện tại, dự án **Stylemind** đang ở trạng thái **Frontend-Only (Thiếu Backend)**. 
* Thư mục `BE/` ở thư mục gốc hoàn toàn trống (chỉ chứa tệp `.gitkeep` để giữ thư mục trong Git).
* Toàn bộ dữ liệu của hệ thống (sản phẩm, kho hàng, đơn hàng, người dùng, hội thoại của AI) đều được giả lập (**Mock Data**) và xử lý hoàn toàn trên trình duyệt của Client.
* Giao diện người dùng đã được xây dựng hoàn thiện và chuyên nghiệp, có sẵn các file gọi API nhưng đang cấu hình để trả về dữ liệu giả lập từ các file `*.api.js` trong thư mục `src/features`.

---

## 2. Công nghệ đang sử dụng (Frontend Stack)

Dựa trên tệp [package.json](file:///e:/Ki_8/MSS/MSS301-Stylemind/FE/package.json), dự án sử dụng các công nghệ hiện đại bao gồm:

| Thư viện | Phiên bản | Vai trò & Trách nhiệm |
| :--- | :--- | :--- |
| **Vite** | `^5.3.1` | Công cụ build và hot-reload nhanh cho môi trường phát triển. |
| **React** | `^18.3.1` | Thư viện xây dựng giao diện người dùng dựa trên Component. |
| **React Router DOM** | `^6.23.1` | Quản lý định tuyến (routing) giữa các trang của ứng dụng. |
| **Tailwind CSS** | `^4.0.0` | Framework CSS tiện ích (Utility-first) để xây dựng giao diện hiện đại, responsive. |
| **Zustand** | `^4.5.2` | Thư viện quản lý trạng thái (State Management) nhẹ và hiệu năng cao (được dùng để lưu trữ giỏ hàng, thông tin thanh toán). |
| **Axios** | `^1.7.2` | HTTP Client dùng để gửi yêu cầu đến backend (đã cấu hình interceptor để đính kèm Token xác thực). |
| **Lucide React** | `^0.395.0` | Bộ icon SVG hiện đại, đồng bộ về mặt thẩm mỹ. |
| **Framer Motion** | `^11.2.10` | Thư viện tạo các hiệu ứng chuyển động (animations) mượt mà cho UI. |
| **Recharts** | `^2.12.7` | Thư viện vẽ biểu đồ tương tác, dùng cho giao diện quản trị viên (Admin Dashboard). |

---

## 3. Cấu trúc thư mục nguồn (Source Code Structure)

Giao diện được tổ chức theo cấu trúc **Feature-Driven** (chia theo tính năng nghiệp vụ), giúp dự án dễ dàng mở rộng và tách thành các microservice tương ứng sau này. Dưới đây là sơ đồ cấu trúc thư mục trong thư mục `FE/src`:

```text
src/
├── app/                  # Chứa cấu hình cốt lõi của ứng dụng
│   ├── App.jsx           # Entry Component chính
│   └── router.jsx        # Định nghĩa các tuyến đường (routes)
├── assets/               # Chứa ảnh tĩnh, logo, v.v.
├── components/           # Các component tái sử dụng chung
│   ├── admin/            # Component dùng riêng cho trang Admin (Sidebar, Table, Topbar)
│   ├── ai/               # Component dùng riêng cho AI Chat (ChatBubble, ReasoningPanel, ProductBlock)
│   ├── common/           # Component dùng chung (Button, Card, Input)
│   └── customer/         # Component dùng riêng cho khách hàng (ProductCard, OutfitCard, CartItem)
├── data/                 # Chứa dữ liệu Mock thô (JSON/JS files)
│   ├── mockAnalytics.js  # Dữ liệu phân tích, biểu đồ admin
│   ├── mockInventory.js  # Dữ liệu kho hàng tồn
│   ├── mockOrders.js     # Lịch sử đơn hàng khách hàng
│   ├── mockProducts.js   # Danh sách sản phẩm quần áo và outfit gợi ý
│   └── mockUsers.js      # Danh sách khách hàng và hồ sơ sở thích
├── features/             # Chứa logic nghiệp vụ được chia theo tính năng độc lập
│   ├── ai-stylist/       # Xử lý chat tư vấn và mock dữ liệu AI
│   ├── analytics/        # Logic lấy dữ liệu phân tích hệ thống
│   ├── auth/             # Xử lý đăng nhập, đăng ký, lưu token
│   ├── cart/             # Quản lý giỏ hàng (Zustand store + mock API)
│   ├── inventory/        # Quản lý tồn kho cho admin
│   ├── orders/           # Quản lý đơn hàng và timeline vận chuyển
│   ├── payment/          # Giả lập quy trình thanh toán (Zustand store)
│   ├── products/         # Logic lấy danh sách và chi tiết sản phẩm
│   └── profile/          # Quản lý hồ sơ phong cách cá nhân
├── hooks/                # Custom React Hooks
├── layouts/              # Giao diện khung (Layouts: Admin, Customer, Auth)
├── pages/                # Các trang hiển thị chính tương ứng với Router
│   ├── admin/            # Các trang chức năng quản trị viên
│   ├── auth/             # Các trang đăng ký, đăng nhập, khảo sát style
│   └── customer/         # Các trang mua sắm, giỏ hàng, chat AI của khách
├── services/             # Cấu hình API Client dùng chung
│   ├── apiClient.js      # Cấu hình Axios, Interceptors đính kèm JWT
│   └── endpoints.js      # Định nghĩa các đường dẫn URL của API Gateway
└── utils/                # Các hàm tiện ích dùng chung
```

---

## 4. Các chức năng giao diện (UI) đã có

Ứng dụng hiện chia thành 3 phân hệ chính tương ứng với các Layouts:

### A. Phân hệ Khách hàng (Customer Pages - `src/pages/customer`)
1. **Trang chủ (`HomePage.jsx`):** Hiển thị banner phong cách, danh mục sản phẩm nổi bật, sản phẩm mới cập nhật và các outfit gợi ý theo xu hướng.
2. **Trang mua sắm (`ProductCatalogPage.jsx`):** Danh sách toàn bộ sản phẩm. Hỗ trợ tìm kiếm, lọc theo danh mục, mức giá và sắp xếp theo giá cả, đánh giá, hoặc theo **Điểm khớp AI (AI Match Score)**.
3. **Trang chi tiết sản phẩm (`ProductDetailPage.jsx`):** Xem thông tin sản phẩm, chọn màu sắc, chọn size, xem điểm AI Match và các sản phẩm gợi ý phối kèm.
4. **Giỏ hàng (`CartPage.jsx`):** Xem danh sách sản phẩm đã chọn, tăng/giảm số lượng, xóa sản phẩm và tính tổng giá trị giỏ hàng.
5. **Thanh toán (`CheckoutPage.jsx`):** Chọn địa chỉ giao hàng, phương thức thanh toán (COD hoặc giả lập thanh toán online) và hiển thị quá trình xử lý đơn hàng gồm 4 bước: *Tạo đơn -> Giữ kho -> Xử lý thanh toán -> Xác nhận*.
6. **Theo dõi đơn hàng (`OrderTrackingPage.jsx`):** Xem danh sách đơn hàng đã mua và chi tiết timeline vận chuyển của từng đơn.
7. **Tư vấn viên AI (`AIStylistChatPage.jsx`):** Giao diện Chatbot thông minh. Khách hàng nhắn yêu cầu thời trang, AI Agent phân tích và gợi ý sản phẩm/outfit, hiển thị điểm số match score kèm panel giải thích lý do gợi ý (AI Reasoning). Người dùng có thể click thêm nhanh cả outfit vào giỏ hàng hoặc lưu outfit.

### B. Phân hệ Xác thực (Auth Pages - `src/pages/auth`)
1. **Đăng nhập (`LoginPage.jsx`):** Nhập email/password để đăng nhập.
2. **Đăng ký (`RegisterPage.jsx`):** Đăng ký tài khoản mới.
3. **Hồ sơ phong cách (`StyleProfilePage.jsx`):** Khảo sát sở thích của người dùng bao gồm dáng người (body type), kiểu dáng ưa thích (fit preference), màu sắc yêu thích, size đồ và phong cách định hình (Style DNA) nhằm làm đầu vào cá nhân hóa cho AI.

### C. Phân hệ Quản trị (Admin Pages - `src/pages/admin`)
1. **Bảng điều khiển (`AdminDashboardPage.jsx`):** Biểu đồ doanh thu, tỷ lệ chuyển đổi và các chỉ số tương tác AI (CTR).
2. **Quản lý sản phẩm (`ProductManagementPage.jsx`):** Danh sách sản phẩm, biểu mẫu thêm mới và chỉnh sửa thông tin sản phẩm.
3. **Quản lý kho (`InventoryManagementPage.jsx`):** Theo dõi số lượng tồn kho thực tế, số lượng hàng đang bị giữ (reserved stock) do khách đang đặt và cảnh báo hết hàng.
4. **Quản lý đơn hàng (`OrderManagementPage.jsx`):** Xem danh sách đơn hàng của toàn hệ thống và cập nhật trạng thái đơn hàng.
5. **Quản lý khách hàng (`CustomerManagementPage.jsx`):** Danh sách khách hàng kèm phân hạng thành viên (Silver, Gold, Platinum) và Style DNA của họ.
6. **AI Pipeline (`AIPipelinePage.jsx`):** Xem trạng thái các công việc chạy nền của AI (như embed sản phẩm, sync graph, cập nhật index) kèm logs sự kiện.
7. **Đồ thị tri thức (`KnowledgeGraphPage.jsx`):** Trực quan hóa các mối liên kết (Nodes & Relationships) giữa sản phẩm, màu sắc, phong cách và khách hàng dạng mạng nhện.
8. **Phân tích hiệu quả gợi ý (`RecommendationAnalyticsPage.jsx`):** Biểu đồ phễu chuyển đổi từ đề xuất của AI đến lượt click và đặt hàng thực tế.
9. **Cài đặt hệ thống (`AdminSettingsPage.jsx`):** Cấu hình các tham số hệ thống, phân quyền admin và giám sát hàng đợi xử lý công việc.

---

## 5. Các API frontend đang gọi và Phân tích ánh xạ

Mặc dù frontend đang chạy dữ liệu giả lập, cấu trúc gọi API đã được chuẩn bị sẵn thông qua tệp [endpoints.js](file:///e:/Ki_8/MSS/MSS301-Stylemind/FE/src/services/endpoints.js). Tệp này khai báo đường dẫn API Gateway mặc định là `http://localhost:3000/api` và chia thành các tài nguyên độc lập.

Dưới đây là bảng phân tích chi tiết các API mà frontend đang gọi từ các tệp logic (`src/features/**/*.api.js` hoặc các Zustand stores) và định hướng ánh xạ sang các Microservices tương ứng ở Backend:

### 5.1. Phân hệ Xác thực & Người dùng
* **Tệp gọi:** `src/features/auth/auth.api.js` và `src/features/profile/profile.api.js`

| Chức năng UI | Method | URL Path | Request Body | Response dự kiến | Service xử lý |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Đăng nhập | `POST` | `/auth/login` | `{ email, password }` | `{ success: true, data: { token, user: { id, name, email, role } } }` | `auth-service` |
| Đăng ký | `POST` | `/auth/register` | `{ name, email, password }` | `{ success: true, data: { token, user: { id, name, email, role } } }` | `auth-service` |
| Đăng xuất | `POST` | `/auth/logout` | Không có | `{ success: true, message: "Logged out" }` | `auth-service` |
| Lấy user hiện tại | `GET` | `/auth/me` | Header JWT | `{ success: true, data: { id, name, email, role } }` | `auth-service` |
| Lấy Style Profile | `GET` | `/users/profile` | Header JWT | `{ success: true, data: { id, stylePreferences, bodyType, favoriteColors } }` | `user-service` |
| Cập nhật Profile | `PUT` | `/users/profile` | `{ bodyType, fitPreference, favoriteColors, sizeProfile }` | `{ success: true, data: { id, bodyType, fitPreference, ... } }` | `user-service` |

### 5.2. Phân hệ Sản phẩm & Tồn kho
* **Tệp gọi:** `src/features/products/product.api.js` và `src/features/inventory/inventory.api.js`

| Chức năng UI | Method | URL Path | Request Query / Body | Response dự kiến | Service xử lý |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Lấy danh sách SP | `GET` | `/products` | `?category=...&search=...&minPrice=...&maxPrice=...&sort=...` | `{ success: true, data: [ { id, name, price, originalPrice, category, SKU, ... } ] }` | `product-service` |
| Chi tiết sản phẩm | `GET` | `/products/:id` | Đường dẫn `id` | `{ success: true, data: { id, name, price, description, images, colors, sizes, material } }` | `product-service` |
| Tìm kiếm sản phẩm | `GET` | `/products/search` | `?q=...` | `{ success: true, data: [ { id, name, price, ... } ] }` | `product-service` |
| Lấy toàn bộ tồn kho | `GET` | `/inventory` | Chỉ dành cho Admin | `{ success: true, data: [ { productId, sku, currentStock, reservedStock, status } ] }` | `inventory-service` |
| Lấy tồn kho của 1 SP | `GET` | `/inventory/:productId` | Đường dẫn `productId` | `{ success: true, data: { productId, sku, currentStock, reservedStock } }` | `inventory-service` |
| Cập nhật số lượng kho | `PUT` | `/inventory/:productId` | `{ quantity }` | `{ success: true, data: { productId, currentStock, lastUpdated } }` | `inventory-service` |

### 5.3. Phân hệ Giỏ hàng & Đặt hàng & Thanh toán
* **Tệp gọi:** `src/features/cart/cart.api.js`, `src/features/orders/order.api.js`, và `src/features/payment/payment.store.js`

| Chức năng UI | Method | URL Path | Request Query / Body | Response dự kiến | Service xử lý |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Lấy giỏ hàng | `GET` | `/cart` | Header JWT hoặc `?guestSessionId=...` | `{ success: true, data: { items: [ { cartItemId, id, name, quantity, size, color } ] } }` | `cart-service` |
| Thêm vào giỏ hàng | `POST` | `/cart` | `{ productId, quantity, size, color }` + `guestSessionId` | `{ success: true, data: { cartItemId, productId, quantity, size, color } }` | `cart-service` |
| Cập nhật số lượng | `PUT` | `/cart/:cartItemId` | `{ quantity }` | `{ success: true, data: { cartItemId, quantity } }` | `cart-service` |
| Xóa khỏi giỏ hàng | `DELETE` | `/cart/:cartItemId` | Đường dẫn `cartItemId` | `{ success: true, message: "Item removed" }` | `cart-service` |
| Khởi tạo thanh toán | `POST` | `/payment/checkout` | `{ items, total, method, address }` | `{ success: true, data: { transactionId, status, amount } }` | `payment-service` |
| Tạo đơn hàng mới | `POST` | `/orders` | `{ items, total, paymentMethod, address, transactionId }` | `{ success: true, data: { id, date, status, items, total, timeline: [...] } }` | `order-service` |
| Danh sách đơn hàng | `GET` | `/orders` | Header JWT | `{ success: true, data: [ { id, date, status, total, items: [...] } ] }` | `order-service` |
| Chi tiết đơn hàng | `GET` | `/orders/:id` | Đường dẫn `id` | `{ success: true, data: { id, date, status, total, items, timeline } }` | `order-service` |
| Theo dõi hành trình | `GET` | `/orders/:id/tracking` | Đường dẫn `id` | `{ success: true, data: [ { status, date, completed } ] }` | `order-service` |

### 5.4. Phân hệ Tư vấn AI Agent & Phân tích
* **Tệp gọi:** `src/features/ai-stylist/aiStylist.api.js` và `src/features/analytics/analytics.api.js`

| Chức năng UI | Method | URL Path | Request Query / Body | Response dự kiến | Service xử lý |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Chat với AI Stylist | `POST` | `/ai-stylist/chat` | `{ prompt, conversationId }` | `{ success: true, data: { conversationId, message, intent, recommendedProducts: [...], styleTips: [...] } }` | `ai-agent-service` |
| Giải thích đề xuất AI | `POST` | `/ai-stylist/explain` | `{ recommendedProducts, context }` | `{ success: true, data: { matchScore, factors: [ "Color", "Style DNA" ], explanation: "..." } }` | `ai-agent-service` |
| Gợi ý outfit theo dịp | `POST` | `/ai-stylist/recommend-outfits` | `{ occasion, style, gender, budget, preferredColors }` | `{ success: true, data: { outfits: [ { outfitId, title, totalPrice, items: [...] } ] } }` | `ai-agent-service` |
| Lịch sử tư vấn AI | `GET` | `/ai-stylist/history` | Header JWT | `{ success: true, data: [ { id, date, prompt, response } ] }` | `ai-agent-service` |
| Đồng bộ index SP | `POST` | `/internal/ai/index/products/:id` | Header bảo mật nội bộ | `{ success: true, message: "Product indexed successfully" }` | `ai-agent-service` (API nội bộ) |
| Chỉ số Dashboard | `GET` | `/analytics/dashboard` | Chỉ dành cho Admin | `{ success: true, data: { sales: ..., conversionRate: ..., ctr: ... } }` | `analytics-service` |
| Sự kiện AI Pipeline | `GET` | `/analytics/ai-pipeline` | Chỉ dành cho Admin | `{ success: true, data: { recentEvents: [ { id, eventType, status, timestamp } ] } }` | `ai-agent-service` |
| Phễu đề xuất AI | `GET` | `/analytics/funnel` | Chỉ dành cho Admin | `{ success: true, data: [ { stage: "recommend", count: 1200 }, ... ] }` | `analytics-service` |
| Top sản phẩm đề xuất | `GET` | `/analytics/top-products` | Chỉ dành cho Admin | `{ success: true, data: [ { id, name, clicks, conversion } ] }` | `analytics-service` |
| Đồ thị tri thức (Graph) | `GET` | `/analytics/knowledge-graph` | Chỉ dành cho Admin | `{ success: true, data: { nodes: [...], links: [...] } }` | `ai-agent-service` |

---

## 6. Kết luận phân tích hiện trạng

Cấu trúc frontend hiện tại đã được tách biệt logic nghiệp vụ rất tốt vào các thư mục `src/features`. Điều này mở ra cơ hội tuyệt vời để triển khai **Kiến trúc Microservices** bằng Spring Boot:
1. Mỗi thư mục trong `src/features` (như `products`, `orders`, `cart`, `ai-stylist`) sẽ được ánh xạ trực tiếp thành một microservice nghiệp vụ tương ứng ở backend.
2. Các API Client (`apiClient.js`) và `endpoints.js` hiện tại đã sẵn sàng để chuyển hướng toàn bộ yêu cầu từ giả lập sang một điểm tập trung duy nhất là **API Gateway** để định tuyến vào các microservices.
3. Thử thách chính nằm ở việc hiện thực hóa dịch vụ `ai-agent-service` để xử lý RAG nâng cao dựa trên đồ thị tri thức thời trang (Neo4j) và tìm kiếm lai (Qdrant) mà vẫn đảm bảo tính chính xác thông tin thời gian thực của sản phẩm, tồn kho và trạng thái đơn hàng.
