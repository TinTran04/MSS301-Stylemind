# StyleMind - AI-Powered Fashion Platform (Frontend)

Thư mục này chứa mã nguồn Frontend cho dự án **StyleMind**, một nền tảng thương mại điện tử thời trang được tích hợp trí tuệ nhân tạo (AI). Nền tảng được phát triển bằng **ReactJS** và **Vite**, sử dụng **Tailwind CSS v4** để thiết kế giao diện và quản lý trạng thái thông qua **Zustand**.

---

## 🛠️ Công nghệ sử dụng

Frontend của dự án sử dụng các thư viện và công nghệ hiện đại sau:

*   **Framework chính:** [React 18](https://react.dev/) & [Vite 5](https://vite.dev/) (Tối ưu tốc độ build và Hot Module Replacement).
*   **Styling:** [Tailwind CSS v4](https://tailwindcss.com/) (Tận dụng `@tailwindcss/vite` để tối ưu hóa CSS).
*   **Routing:** [React Router DOM v6](https://reactrouter.com/) (Hỗ trợ phân chia Layout cho Khách hàng, Admin và Auth).
*   **Quản lý trạng thái (State Management):** [Zustand](https://zustand-demo.pmnd.rs/) (Nhẹ, hiệu quả cao, thay thế cho Redux Toolkit).
*   **Kết nối API:** [Axios](https://axios-http.com/) (Được cấu hình interceptor tự động đính kèm JWT token).
*   **Biểu đồ & Thống kê:** [Recharts](https://recharts.org/) (Dùng cho trang Dashboard và Recommendation Analytics của Admin).
*   **Hiệu ứng chuyển động:** [Framer Motion](https://www.framer.com/motion/) (Tạo các hiệu ứng micro-animations mượt mà).
*   **Icons:** [Lucide React](https://lucide.dev/) & [Material Symbols](https://fonts.google.com/icons).

---

## 📁 Cấu trúc Thư mục

Mã nguồn được tổ chức theo hướng module hóa dựa trên các tính năng (feature-based structure) giúp dự án dễ mở rộng:

```text
FE/
├── public/                 # Các tài nguyên tĩnh (favicon, logo, v.v.)
├── src/
│   ├── app/                # Cấu hình cốt lõi của ứng dụng
│   │   ├── App.jsx         # Component gốc nhập Router
│   │   └── router.jsx      # Định tuyến (Routing) toàn bộ ứng dụng
│   ├── assets/             # Hình ảnh, font chữ tĩnh
│   ├── components/         # Các UI component có thể tái sử dụng
│   │   ├── admin/          # Component dành riêng cho trang Admin (Sidebar, Table, Topbar...)
│   │   ├── ai/             # Component liên quan đến AI (ChatBubble, PromptSuggestion, Reasoning...)
│   │   ├── common/         # Các UI Core tái sử dụng chung (Button, Card, Modal, Input...)
│   │   └── customer/       # Component dành riêng cho khách hàng (CartItem, ProductCard, Filter...)
│   ├── data/               # Dữ liệu Mock phục vụ demo & phát triển
│   │   ├── mockAnalytics.js
│   │   ├── mockInventory.js
│   │   ├── mockProducts.js
│   │   └── mockUsers.js
│   ├── features/           # Các Module nghiệp vụ (mỗi module chứa API, Store và các tiện ích riêng)
│   │   ├── ai-stylist/     # Quản lý chatbot tư vấn thời trang AI
│   │   ├── analytics/      # Phân tích dữ liệu & thống kê cho Admin
│   │   ├── auth/           # Xác thực người dùng (Đăng ký, Đăng nhập, lưu session)
│   │   ├── cart/           # Quản lý giỏ hàng (Zustand store)
│   │   ├── inventory/      # Quản lý kho hàng
│   │   ├── orders/         # Xử lý đơn hàng
│   │   ├── payment/        # Giả lập thanh toán
│   │   ├── products/       # Danh mục & chi tiết sản phẩm
│   │   └── profile/        # Quản lý Style Profile của khách hàng (số đo, sở thích thời trang)
│   ├── hooks/              # Các Custom Hooks dùng chung (useAuth, useCart, useDebounce)
│   ├── layouts/            # Các khung bố cục chính (AdminLayout, AuthLayout, CustomerLayout)
│   ├── pages/              # Các trang giao diện hoàn chỉnh
│   │   ├── admin/          # Dashboard, QL sản phẩm, QL kho, Pipeline AI, Knowledge Graph, v.v.
│   │   ├── auth/           # Login, Register, Thiết lập Style Profile ban đầu
│   │   └── customer/       # Trang chủ, Cửa hàng, Chi tiết sản phẩm, AI Chatbot Stylist, Giỏ hàng, Đơn hàng
│   ├── services/           # Xử lý kết nối mạng và API bên ngoài
│   │   ├── apiClient.js    # Cấu hình Axios Client (Interceptor để tự động xử lý token & lỗi 401)
│   │   └── endpoints.js    # Centralized endpoints kết nối đến Spring Boot Backend
│   ├── styles/             # Cấu hình giao diện và màu sắc mở rộng
│   │   └── index.css       # File css chính chứa @theme và các keyframes tùy biến cao cấp
│   ├── utils/              # Các hàm bổ trợ dùng chung
│   ├── main.jsx            # Điểm khởi chạy chính ứng dụng (Mounting root)
│   └── index.css           # Cấu hình Tailwind directives & custom classes
├── index.html              # Template HTML chính của ứng dụng
├── package.json            # Định nghĩa các dependencies & scripts chạy dự án
└── vite.config.js          # Cấu hình Vite & Tailwind CSS plugins
```

---

## ⚡ Các Tính năng Chính trên Giao diện

### 1. Phân hệ Khách hàng (Customer Portal)
*   **Trang chủ (Home Page):** Giao diện thiết kế theo phong cách tối giản cao cấp (Editorial Style) giới thiệu các bộ sưu tập mới nhất.
*   **Bộ lọc sản phẩm thông minh (Product Catalog & Filter):** Lọc theo danh mục, khoảng giá, chất liệu, màu sắc và sắp xếp theo độ tương thích của AI.
*   **Tư vấn thời trang AI (AI Stylist Chatbot):** Giao diện chat trực quan với trợ lý AI, hỗ trợ gợi ý Outfit phù hợp với chỉ số cơ thể và sở thích, hiển thị trực quan các thẻ sản phẩm kèm bảng suy luận lý do gợi ý (*AI Reasoning Panel*).
*   **Hồ sơ thời trang cá nhân (Style Profile):** Nơi khách hàng cấu hình thông tin hình dáng cơ thể, phong cách ưa thích, màu sắc yêu thích, ngân sách để trợ lý AI đưa ra gợi ý chuẩn xác nhất.
*   **Giỏ hàng & Thanh toán (Cart & Checkout):** Xem nhanh giỏ hàng thông qua Sliding Drawer, tính toán tổng tiền, VAT, giao hàng và giả lập thanh toán an toàn.
*   **Theo dõi đơn hàng (Order Tracking Page):** Theo dõi trạng thái vận chuyển qua dòng thời gian (Timeline) động trực quan.

### 2. Phân hệ Quản trị viên (Admin Dashboard)
*   **Thống kê tổng quan (Dashboard Analytics):** Biểu đồ doanh thu, số đơn hàng, khách hàng mới và tỷ lệ mua hàng được đề xuất bởi AI sử dụng Recharts.
*   **Quản lý danh mục & Kho hàng (Product & Inventory Management):** Xem danh sách, cập nhật hàng tồn kho, mã SKU, trạng thái sẵn hàng của từng phân loại sản phẩm.
*   **Quản lý quy trình AI (AI Pipeline Page):** Giao diện kiểm tra quy trình xử lý AI bao gồm Vectorizing, Data Sync, và Model Training.
*   **Trực quan hóa đồ thị tri thức (Knowledge Graph):** Quản trị các thực thể thời trang như Trends, Occasions, Attributes để huấn luyện AI.
*   **Phân tích đề xuất (Recommendation Analytics):** Phân tích hiệu suất đề xuất của chatbot AI, tỷ lệ chuyển đổi đơn hàng từ gợi ý của AI.

---

## 🚀 Hướng dẫn Cài đặt & Khởi chạy

### 1. Yêu cầu hệ thống
*   Đã cài đặt [Node.js](https://nodejs.org/) (Khuyên dùng phiên bản LTS v18 trở lên).
*   Đã cài đặt package manager (`npm` hoặc `yarn`).

### 2. Cài đặt các thư viện phụ thuộc
Di chuyển vào thư mục `FE` và chạy lệnh sau để cài đặt tất cả các dependencies trong `package.json`:
```bash
npm install
```

### 3. Cấu hình biến môi trường (Environment Variables)
Tạo một file `.env` ở thư mục gốc của dự án `FE` và định nghĩa địa chỉ API Gateway kết nối tới Spring Boot Backend:
```env
VITE_API_GATEWAY=http://localhost:3000/api
```
*(Nếu không khai báo biến này, ứng dụng mặc định kết nối tới `http://localhost:3000/api`)*.

### 4. Khởi chạy dự án ở chế độ Phát triển (Development)
Chạy lệnh sau để khởi chạy Vite dev server:
```bash
npm run dev
```
Sau khi chạy thành công, giao diện sẽ hiển thị tại địa chỉ mặc định: [http://localhost:5173](http://localhost:5173).

### 5. Build dự án cho môi trường Production
Để đóng gói ứng dụng tối ưu hóa hiệu năng phục vụ cho việc deploy:
```bash
npm run build
```
Thư mục đầu ra `dist/` sẽ được tạo ra chứa toàn bộ mã nguồn tĩnh đã được compile, minify và tối ưu hóa dung lượng.

---

## 🛡️ Xác thực & Bảo mật trên Frontend
*   **Quản lý Token:** JWT token sau khi đăng nhập được lưu trữ trong `localStorage`.
*   **Request Interceptor:** Mỗi khi gọi API qua `apiClient`, interceptor sẽ tự động lấy token và đính kèm vào header `Authorization: Bearer <token>`.
*   **Response Interceptor:** Nếu nhận được phản hồi lỗi `401 Unauthorized` từ Spring Boot Backend, hệ thống sẽ tự động xóa token và chuyển hướng người dùng về trang `/login`.
