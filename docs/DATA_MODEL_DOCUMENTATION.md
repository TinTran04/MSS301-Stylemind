# DATA MODEL DOCUMENTATION: E-COMMERCE INTEGRATED WITH AI STYLIST CORE

[cite_start]Tài liệu này đặc tả danh sách các Thực thể (Entities) và Thuộc tính (Attributes) của hệ thống E-Commerce kết hợp Advanced AI Stylist Core, phục vụ cấu trúc dữ liệu chuẩn (Single Source of Truth) sau khi đã loại bỏ phân vùng quản lý kho (Inventory)[cite: 2].

---

## 1. Phân vùng Core E-Commerce & Product
[cite_start]Phân vùng này quản lý toàn bộ thông tin gốc về sản phẩm, danh mục và các biến thể thời trang làm cơ sở dữ liệu gốc để đồng bộ lên các tầng nhận thức của AI (Vector Index và Knowledge Graph)[cite: 4].

### `categories` (Danh mục sản phẩm)
[cite_start]Quản lý cấu trúc cây danh mục sản phẩm phục vụ việc điều hướng và phân loại phân cấp[cite: 6].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID duy nhất[cite: 6]. | Khóa chính (Primary Key) |
| `name` | [cite_start]Tên danh mục (Ví dụ: Áo khoác, Quần tây)[cite: 6]. | Chuỗi ký tự (String), Not Null |
| `parent_id` | [cite_start]ID của danh mục cha (Phục vụ cấu trúc cây danh mục đệ quy)[cite: 6]. | Khóa ngoại (Foreign Key) |
| `slug` | [cite_start]Đường dẫn tinh gọn phục vụ tìm kiếm/SEO[cite: 6]. | Chuỗi ký tự (String), Unique |

### `products` (Sản phẩm gốc)
[cite_start]Lưu trữ thông tin định danh và các siêu dữ liệu thời trang cốt lõi cấu thành thuộc tính sản phẩm[cite: 8].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID sản phẩm[cite: 8]. | Khóa chính (Primary Key) |
| `category_id` | [cite_start]ID danh mục[cite: 8]. | Khóa ngoại (Foreign Key) -> `categories.id` |
| `name` | [cite_start]Tên sản phẩm[cite: 8]. | Chuỗi ký tự (String), Not Null |
| `description` | [cite_start]Mô tả chi tiết sản phẩm (Dùng để AI trích xuất và tạo Vector Embedding)[cite: 8]. | Văn bản dài (Text) |
| `base_price` | [cite_start]Giá bán cơ sở ban đầu[cite: 8]. | Số thập phân (Decimal) |
| `aesthetic_style` | [cite_start]Phong cách thiết kế thời trang (Ví dụ: Casual, Streetwear, Vintage)[cite: 8]. | Chuỗi ký tự (String) |
| `target_demographic` | [cite_start]Đối tượng khách hàng mục tiêu (Ví dụ: Men, Women, Unisex)[cite: 8]. | Chuỗi ký tự (String) |
| `seasonal_property` | [cite_start]Thuộc tính mùa phù hợp (Ví dụ: Spring, Summer, Winter)[cite: 8]. | Chuỗi ký tự (String) |
| `status` | [cite_start]Trạng thái sản phẩm trên hệ thống (ACTIVE, INACTIVE, DELETED)[cite: 8]. | Chuỗi ký tự (String) |
| `created_at` | [cite_start]Thời gian tạo bản ghi ban đầu[cite: 8]. | Thời gian (Timestamp) |
| `updated_at` | [cite_start]Thời gian cập nhật dữ liệu gần nhất[cite: 8]. | Thời gian (Timestamp) |

### `product_variants` (Biến thể sản phẩm)
[cite_start]Quản lý các phối hợp thực tế (Size, Color, Material) của sản phẩm để phục vụ việc chọn kích cỡ phù hợp với cơ thể người dùng[cite: 10].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID biến thể sản phẩm[cite: 10]. | Khóa chính (Primary Key) |
| `product_id` | [cite_start]ID sản phẩm gốc liên kết[cite: 10]. | Khóa ngoại (Foreign Key) -> `products.id` |
| `sku` | [cite_start]Mã định danh quản lý hàng hóa độc nhất của biến thể[cite: 10]. | Chuỗi ký tự (String), Unique |
| `size` | [cite_start]Kích cỡ chi tiết (S, M, L, XL, 30, 31, v.v.)[cite: 10]. | Chuỗi ký tự (String) |
| `color` | [cite_start]Màu sắc hiển thị bên ngoài[cite: 10]. | Chuỗi ký tự (String) |
| `material` | [cite_start]Chất liệu vải cấu thành (Ví dụ: Cotton, Linen, Silk, Jeans)[cite: 10]. | Chuỗi ký tự (String) |
| `price_override` | [cite_start]Giá bán riêng của biến thể (Chỉ áp dụng nếu có thay đổi so với giá gốc)[cite: 10]. | Số thập phân (Decimal), Nullable |

### `product_images` (Hình ảnh sản phẩm)
[cite_start]Tài nguyên trực quan của sản phẩm phục vụ hiển thị UI và làm dữ liệu đầu vào cho mô hình Multimodal Vector Search[cite: 12].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID hình ảnh[cite: 12]. | Khóa chính (Primary Key) |
| `product_id` | [cite_start]ID sản phẩm gốc liên kết[cite: 12]. | Khóa ngoại (Foreign Key) -> `products.id` |
| `image_url` | [cite_start]Đường dẫn lưu trữ hình ảnh trên CDN/S3 (Dùng hiển thị UI và trích xuất hình ảnh đầu vào cho AI)[cite: 12]. | Chuỗi ký tự (String) |
| `is_primary` | [cite_start]Đánh dấu ảnh đại diện chính hiển thị đầu tiên (True/False)[cite: 12]. | Kiểu logic (Boolean) |

---

## 2. Phân vùng Customer Identity & Style Profile
[cite_start]Lưu trữ thông tin định danh khách hàng và hồ sơ sinh trắc học cố định nhằm làm mỏ neo ngữ cảnh giúp AI đối chiếu kích thước hình thể và cá nhân hóa phong cách (Lọc Tier 3 trong pipeline RAG)[cite: 14].

### `users` (Tài khoản người dùng)
[cite_start]Thông tin định danh và tài khoản hệ thống của người dùng[cite: 16].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID người dùng hệ thống[cite: 16]. | Khóa chính (Primary Key) |
| `email` | [cite_start]Địa chỉ email đăng nhập[cite: 16]. | Chuỗi ký tự (String), Unique |
| `password_hash` | [cite_start]Mật khẩu bảo mật đã mã hóa (Có thể trống nếu dùng SSO)[cite: 16]. | Chuỗi ký tự (String), Nullable |
| `full_name` | [cite_start]Họ và tên đầy đủ của người dùng[cite: 16]. | Chuỗi ký tự (String) |
| `provider` | [cite_start]Hình thức xác thực đăng nhập (LOCAL hoặc bên thứ ba như GOOGLE, FACEBOOK)[cite: 16]. | Chuỗi ký tự (String) |
| `provider_id` | [cite_start]ID định danh duy nhất nhận từ nhà cung cấp SSO bên thứ ba[cite: 16]. | Chuỗi ký tự (String), Nullable |
| `role` | [cite_start]Vai trò phân quyền trong hệ thống (CUSTOMER, ADMIN)[cite: 16]. | Chuỗi ký tự (String) |
| `created_at` | [cite_start]Thời gian đăng ký tài khoản thành công[cite: 16]. | Thời gian (Timestamp) |

### `customer_style_profiles` (Hồ sơ phong cách khách hàng)
[cite_start]Mỏ neo sinh trắc học và gu thẩm mỹ cố định của khách hàng để AI Agent thực hiện so khớp ràng buộc về phom dáng[cite: 18].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `user_id` | [cite_start]ID người dùng liên kết (Khóa chính, Khóa ngoại quan hệ 1:1)[cite: 18]. | Khóa ngoại (Foreign Key) -> `users.id` |
| `gender` | [cite_start]Giới tính sinh học của khách hàng[cite: 18]. | Chuỗi ký tự (String) |
| `age` | [cite_start]Tuổi hiện tại của khách hàng[cite: 18]. | Số nguyên (Integer) |
| `height_cm` | [cite_start]Chiều cao đo bằng đơn vị cm[cite: 18]. | Số thập phân (Decimal) |
| `weight_kg` | [cite_start]Cân nặng đo bằng đơn vị kg[cite: 18]. | Số thập phân (Decimal) |
| `body_morphology` | [cite_start]Dáng người/Hình thể sinh học (Ví dụ: Thước kẻ, Đồng hồ cát, Quả lê)[cite: 18]. | Chuỗi ký tự (String) |
| `preferred_fit` | [cite_start]Form dáng quần áo ưa thích (Ví dụ: Slim-fit, Oversized, Regular)[cite: 18]. | Chuỗi ký tự (String) |
| `style_personas` | [cite_start]Chuỗi/mảng văn bản lưu danh sách phong cách yêu thích cá nhân định dạng JSON[cite: 18]. | Định dạng văn bản JSON |

### `delivery_addresses` (Địa chỉ nhận hàng)
[cite_start]Quản lý sổ địa chỉ giao hàng và cung cấp thông tin vị trí phục vụ cho AI trích xuất thời tiết thời gian thực[cite: 20].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID địa chỉ[cite: 20]. | Khóa chính (Primary Key) |
| `user_id` | [cite_start]ID người dùng sở hữu địa chỉ này (Khóa ngoại)[cite: 20]. | Khóa ngoại (Foreign Key) -> `users.id` |
| `recipient_name` | [cite_start]Tên người nhận hàng tại địa điểm[cite: 20]. | Chuỗi ký tự (String) |
| `phone_number` | [cite_start]Số điện thoại liên hệ nhận hàng[cite: 20]. | Chuỗi ký tự (String) |
| `address_line` | [cite_start]Địa chỉ chi tiết nơi ở (Số nhà, số ngõ, tên đường)[cite: 20]. | Chuỗi ký tự (String) |
| `city` | [cite_start]Tỉnh hoặc Thành phố (Dùng để hệ thống AI lấy dữ liệu API thời tiết vùng thực tế)[cite: 20]. | Chuỗi ký tự (String) |
| `is_default` | [cite_start]Đánh dấu đây là địa chỉ mặc định nhận hàng (True/False)[cite: 20]. | Kiểu logic (Boolean) |

---

## 3. Phân vùng Interactive Conversational AI
[cite_start]Lưu trữ toàn bộ lịch sử trò chuyện của Chatbot, ngữ cảnh môi trường (Weather) và các bộ trang phục phối sẵn tương tác (Bundles) do AI tạo ra[cite: 22].

### `chat_sessions` (Phiên tư vấn với AI)
[cite_start]Quản lý vòng đời và ngữ cảnh của một phiên tương tác hội thoại tự nhiên[cite: 24].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID phiên hội thoại (Khóa chính - Sử dụng định dạng dữ liệu UUID)[cite: 24]. | Khóa chính kiểu UUID |
| `user_id` | ID người dùng thực hiện cuộc trò chuyện (Khóa ngoại)[cite: 24]. | Khóa ngoại (Foreign Key) -> `users.id` |
| `context_weather_temp` | [cite_start]Nhiệt độ môi trường thực tế tại vị trí khách hàng lúc chat (Lấy từ API thời tiết bên ngoài)[cite: 24]. | Số thập phân (Decimal), Nullable |
| `context_weather_condition` | [cite_start]Tình trạng thời tiết thực tế tại vị trí khách hàng lúc chat (Ví dụ: Rainy, Sunny, Cloudy)[cite: 24]. | Chuỗi ký tự (String), Nullable |
| `created_at` | [cite_start]Thời điểm bắt đầu phiên trò chuyện tư vấn thời trang[cite: 24]. | Thời gian (Timestamp) |

### `chat_messages` (Chi tiết hội thoại)
[cite_start]Nhật ký chi tiết các lượt thoại giữa người dùng và AI trợ lý[cite: 26].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID tin nhắn hội thoại cá nhân[cite: 26]. | Khóa chính (Primary Key) |
| `session_id` | [cite_start]ID phiên chat chứa tin nhắn này (Khóa ngoại)[cite: 26]. | Khóa ngoại (Foreign Key) -> `chat_sessions.id` |
| `sender_type` | [cite_start]Bản chất vai trò người gửi tin nhắn (USER hoặc AI)[cite: 26]. | Chuỗi ký tự (String / Enum) |
| `message_text` | [cite_start]Nội dung văn bản tin nhắn chat bằng ngôn ngữ tự nhiên thông thường[cite: 26]. | Văn bản dài (Text) |
| `has_product_block` | [cite_start]Đánh dấu xem tin nhắn này có chứa thẻ khối hiển thị sản phẩm tương tác hay không (True/False)[cite: 26]. | Kiểu logic (Boolean) |
| `created_at` | [cite_start]Thời gian gửi tin nhắn lên hệ thống[cite: 26]. | Thời gian (Timestamp) |

### `ai_curated_bundles` (Bộ trang phục do AI gợi ý)
[cite_start]Khối dữ liệu trang phục phối hợp (Outfit set) hoàn chỉnh do AI đề xuất dựa trên luật thời trang và ngữ cảnh người dùng[cite: 28].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID bộ trang phục phối tích hợp[cite: 28]. | Khóa chính (Primary Key) |
| `message_id` | ID tin nhắn tương ứng của AI đã sinh ra bộ sản phẩm này (Khóa ngoại)[cite: 28]. | Khóa ngoại (Foreign Key) -> `chat_messages.id` |
| `justification_summary` | [cite_start]Đoạn văn bản tóm tắt lý do phối đồ thông minh của AI (Ví dụ: 'Set đồ thoáng mát phù hợp với thời tiết 35 độ C hôm nay')[cite: 28]. | Văn bản dài (Text) |
| `created_at` | [cite_start]Thời điểm tạo gợi ý phối đồ hoàn chỉnh[cite: 28]. | Thời gian (Timestamp) |

### `ai_curated_bundle_items` (Chi tiết sản phẩm trong bộ phối)
[cite_start]Bảng trung gian kết nối các sản phẩm cấu thành bộ trang phục phối của AI (Quan hệ Many-to-Many giữa `bundles` và `products`)[cite: 30].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `bundle_id` | ID bộ trang phục phối hoàn chỉnh (Khóa chính, Khóa ngoại)[cite: 30]. | Khóa ngoại -> `ai_curated_bundles.id` |
| `product_id` | [cite_start]ID sản phẩm được AI lựa chọn đưa vào bộ phối (Khóa chính, Khóa ngoại)[cite: 30]. | Khóa ngoại -> `products.id` |

---

## 4. Phân vùng Transactional Orders & Shopping Cart
[cite_start]Quản lý giỏ hàng hiện hành và dữ liệu giao dịch đơn hàng, đồng thời lưu vết nguồn gốc chuyển đổi (Conversion Linkage) từ các gợi ý AI[cite: 32].

### `shopping_carts` (Giỏ hàng người dùng)
[cite_start]Thành phần lưu trữ giỏ hàng hiện hành của khách hàng[cite: 34].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID giỏ hàng cá nhân[cite: 34]. | Khóa chính (Primary Key) |
| `user_id` | ID người dùng sở hữu giỏ hàng (Khóa ngoại, Ràng buộc Unique)[cite: 34]. | Khóa ngoại (Foreign Key) -> `users.id`, Unique |
| `updated_at` | [cite_start]Thời gian cập nhật chỉnh sửa giỏ hàng gần nhất[cite: 34]. | Thời gian (Timestamp) |

### `cart_items` (Chi tiết sản phẩm trong giỏ)
[cite_start]Chi tiết các biến thể sản phẩm nằm trong giỏ hàng, hỗ trợ gắn cờ nếu phần tử đó được thêm từ khối tương tác bundle của AI[cite: 36].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID dòng sản phẩm trong giỏ hiện tại[cite: 36]. | Khóa chính (Primary Key) |
| `cart_id` | [cite_start]ID giỏ hàng cha (Khóa ngoại)[cite: 36]. | Khóa ngoại (Foreign Key) -> `shopping_carts.id` |
| `variant_id` | [cite_start]ID biến thể sản phẩm cụ thể khách chọn mua (Khóa ngoại)[cite: 36]. | Khóa ngoại (Foreign Key) -> `product_variants.id` |
| `quantity` | [cite_start]Số lượng sản phẩm đặt mua tạm thời[cite: 36]. | Số nguyên (Integer) |
| `is_ai_recommended` | [cite_start]Đánh dấu sản phẩm được cho vào giỏ từ nút 'Mua trọn bộ của AI' (True/False)[cite: 36]. | Kiểu logic (Boolean), Default False |
| `source_bundle_id` | [cite_start]ID bộ phối của AI nguồn (Khóa ngoại - Chỉ có dữ liệu khi is_ai_recommended = True)[cite: 36]. | Khóa ngoại -> `ai_curated_bundles.id`, Nullable |

### `orders` (Đơn đặt hàng)
[cite_start]Quản lý trạng thái vòng đời của đơn hàng, phục vụ kiến trúc phân tán (Saga Workflow) với khả năng bù trừ giao dịch[cite: 38].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID hóa đơn đơn hàng đặt mua[cite: 38]. | Khóa chính (Primary Key) |
| `user_id` | ID khách hàng thực hiện mua sắm đơn này (Khóa ngoại)[cite: 38]. | Khóa ngoại (Foreign Key) -> `users.id` |
| `total_amount` | [cite_start]Tổng giá trị đơn hàng sau cùng phải thanh toán[cite: 38]. | Số thập phân (Decimal) |
| `order_status` | [cite_start]Trạng thái xử lý đơn hàng thực tế (PENDING, PROCESSING, COMPENSATING_ROLLBACK, FULFILLED, CANCELLED)[cite: 38]. | Chuỗi ký tự (String) |
| `shipping_address` | [cite_start]Ảnh chụp thông tin văn bản địa chỉ nhận hàng cố định lưu tại thời điểm bấm đặt đơn[cite: 38]. | Văn bản dài (Text) |
| `created_at` | [cite_start]Thời gian khởi tạo đơn hàng thành công trên hệ thống[cite: 38]. | Thời gian (Timestamp) |
| `updated_at` | [cite_start]Thời gian cập nhật trạng thái đơn hàng gần đây nhất[cite: 38]. | Thời gian (Timestamp) |

### `order_items` (Chi tiết hóa đơn sản phẩm)
[cite_start]Chi tiết từng mặt hàng và Snapshot giá thực tế khi thanh toán đơn hàng[cite: 40].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID dòng hóa đơn chi tiết[cite: 40]. | Khóa chính (Primary Key) |
| `order_id` | [cite_start]ID đơn đặt hàng cha liên kết (Khóa ngoại)[cite: 40]. | Khóa ngoại (Foreign Key) -> `orders.id` |
| `variant_id` | ID biến thể sản phẩm cụ thể được mua (Khóa ngoại)[cite: 40]. | Khóa ngoại (Foreign Key) -> `product_variants.id` |
| `quantity` | [cite_start]Số lượng sản phẩm mua thực tế[cite: 40]. | Số nguyên (Integer) |
| `price_at_purchase` | [cite_start]Giá sản phẩm chốt tại thời điểm mua (Dùng để Snapshot chống đổi giá hệ thống sau này)[cite: 40]. | Số thập phân (Decimal) |
| `is_ai_conversion` | [cite_start]Đánh dấu dòng sản phẩm này chuyển đổi thành công từ gợi ý của AI (Phục vụ thống kê Conversion Rate của Admin)[cite: 40]. | Kiểu logic (Boolean), Default False |
| `source_bundle_id` | [cite_start]ID bộ phối gốc của AI tạo ra đơn mua này nếu có[cite: 40]. | Số nguyên / UUID (Tương ứng với `ai_curated_bundles.id`), Nullable |

---

## 5. Phân vùng AI Performance Analytics
[cite_start]Nhật ký ghi nhận trực tiếp các sự kiện tương tác của khách hàng (Impression, Click, Add to Cart) nhằm phục vụ công cụ tính toán số học chỉ số Click-Through Rate (CTR) trên Analytics Dashboard của Admin[cite: 42].

### `ai_analytics_logs` (Nhật ký tương tác số hóa AI)
[cite_start]Lưu vết telemetry hành vi của người dùng trên các khối Product Block do AI khởi tạo[cite: 44].

| Thuộc tính (Attribute) | Mô tả (Description) | Ràng buộc gợi ý |
| :--- | :--- | :--- |
| `id` | [cite_start]ID bản ghi nhật ký hoạt động[cite: 44]. | Khóa chính (Primary Key) |
| `user_id` | [cite_start]ID khách hàng phát sinh hành vi tương tác (Khóa ngoại)[cite: 44]. | Khóa ngoại (Foreign Key) -> `users.id` |
| `bundle_id` | ID bộ phối trang phục của AI nhận tương tác tương ứng (Khóa ngoại)[cite: 44]. | Khóa ngoại (Foreign Key) -> `ai_curated_bundles.id` |
| `interaction_type` | [cite_start]Loại tương tác cụ thể của khách hàng (IMPRESSION - Hiển thị, CLICK - Bấm xem chi tiết, ADD_TO_CART - Thêm trọn bộ vào giỏ)[cite: 44]. | Chuỗi ký tự (String / Enum) |
| `created_at` | [cite_start]Thời điểm chính xác hành vi tương tác của người dùng xảy ra[cite: 44]. | Thời gian (Timestamp) |