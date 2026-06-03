# MSS301 - StyleMind

**StyleMind** là nền tảng thương mại điện tử thời trang tích hợp **AI Stylist**, được xây dựng với **ReactJS** cho Frontend và **Spring Boot Microservices** cho Backend.

Hệ thống hỗ trợ khách hàng mua sắm sản phẩm thời trang, tạo hồ sơ phong cách cá nhân, trò chuyện với AI để nhận gợi ý phối đồ, thêm sản phẩm vào giỏ hàng, đặt hàng và theo dõi đơn hàng. Đồng thời, Admin có thể quản lý sản phẩm, tồn kho, đơn hàng, khách hàng, pipeline AI, Knowledge Graph và các chỉ số phân tích hiệu quả gợi ý của AI.

Mục tiêu chính của hệ thống là cung cấp trải nghiệm **tư vấn thời trang cá nhân hóa**, đồng thời đảm bảo AI chỉ gợi ý các sản phẩm **thật sự tồn tại và còn hàng trong hệ thống**.

---

## Tổng quan dự án

StyleMind là một hệ thống E-commerce thời trang được thiết kế theo kiến trúc **Microservices**. Điểm nổi bật của hệ thống là tích hợp **AI Stylist** để hỗ trợ người dùng lựa chọn và phối đồ dựa trên:

* Hồ sơ phong cách cá nhân của khách hàng
* Dáng người, chiều cao, cân nặng, sở thích thời trang
* Metadata sản phẩm như chất liệu, mùa, phong cách, dịp sử dụng
* Tình trạng tồn kho theo thời gian thực
* Vector Search để tìm sản phẩm theo ngữ nghĩa
* Knowledge Graph để áp dụng luật phối đồ
* Recommendation System để hỗ trợ gợi ý sản phẩm phù hợp

Hệ thống hướng đến việc giảm tình trạng AI gợi ý sai hoặc gợi ý sản phẩm đã hết hàng.

---

## Tính năng chính

### Khách hàng

* Đăng ký và đăng nhập
* Tạo và cập nhật hồ sơ phong cách cá nhân
* Xem danh mục sản phẩm thời trang
* Tìm kiếm, lọc và sắp xếp sản phẩm
* Xem chi tiết sản phẩm
* Chat với AI Stylist để được tư vấn phối đồ
* Nhận gợi ý outfit từ các sản phẩm còn hàng
* Thêm từng sản phẩm hoặc toàn bộ outfit AI đề xuất vào giỏ hàng
* Quản lý giỏ hàng
* Checkout và thanh toán giả lập
* Theo dõi trạng thái đơn hàng
* Xem lịch sử mua hàng

### Admin / Chủ cửa hàng

* Xem dashboard tổng quan
* Quản lý sản phẩm
* Quản lý biến thể sản phẩm như size, màu sắc, SKU
* Quản lý tồn kho
* Quản lý đơn hàng
* Quản lý khách hàng
* Theo dõi trạng thái đồng bộ AI Pipeline
* Quản lý Knowledge Graph / luật thời trang
* Xem báo cáo phân tích hiệu quả gợi ý của AI
* Cấu hình hệ thống ở mức giao diện quản trị

---

## Kiến trúc hệ thống

Dự án được tổ chức theo hướng:

```text
Frontend ReactJS
        ↓
API Gateway
        ↓
Spring Boot Microservices
        ↓
Database / Message Broker / AI Services
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

* Spring Boot
* Spring Cloud Gateway
* Spring Security
* Spring Data JPA
* PostgreSQL
* Eureka Discovery Service
* RabbitMQ
* REST API
* Docker

### AI / Recommendation

* AI Stylist Service
* Vector Search
* Knowledge Graph
* Recommendation System
* Inventory-aware Recommendation

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

### Backend dự kiến

```text
BE/
├── api-gateway/
├── discovery-service/
├── config-service/
├── auth-service/
├── user-profile-service/
├── product-service/
├── inventory-service/
├── cart-service/
├── order-service/
├── payment-service/
├── ai-stylist-service/
├── recommendation-service/
├── knowledge-graph-service/
├── analytics-service/
└── common-lib/
```

---

## Các service backend dự kiến

| Service                 | Vai trò                                    |
| ----------------------- | ------------------------------------------ |
| API Gateway             | Cổng vào duy nhất cho Frontend             |
| Discovery Service       | Quản lý service discovery                  |
| Config Service          | Quản lý cấu hình tập trung                 |
| Auth Service            | Đăng ký, đăng nhập, JWT, phân quyền        |
| User Profile Service    | Quản lý hồ sơ người dùng và Style Profile  |
| Product Service         | Quản lý sản phẩm, danh mục, metadata       |
| Inventory Service       | Quản lý tồn kho theo size, màu, SKU        |
| Cart Service            | Quản lý giỏ hàng                           |
| Order Service           | Quản lý đơn hàng                           |
| Payment Service         | Xử lý thanh toán giả lập                   |
| AI Stylist Service      | Chat tư vấn phối đồ                        |
| Recommendation Service  | Gợi ý sản phẩm                             |
| Knowledge Graph Service | Quản lý luật phối đồ và quan hệ thời trang |
| Analytics Service       | Thống kê doanh thu, đơn hàng, hiệu quả AI  |

---

## Luồng người dùng chính

```text
Khách hàng truy cập hệ thống
→ Đăng ký / đăng nhập
→ Tạo Style Profile
→ Duyệt sản phẩm hoặc chat với AI Stylist
→ Nhận gợi ý outfit từ sản phẩm còn hàng
→ Thêm sản phẩm vào giỏ hàng
→ Checkout
→ Thanh toán giả lập
→ Tạo đơn hàng
→ Theo dõi trạng thái đơn hàng
```

---

## Luồng Admin chính

```text
Admin đăng nhập
→ Vào Dashboard
→ Quản lý sản phẩm
→ Quản lý tồn kho
→ Quản lý đơn hàng
→ Theo dõi AI Pipeline
→ Quản lý Knowledge Graph
→ Xem Recommendation Analytics
```

---

## AI Stylist

AI Stylist là chức năng nổi bật của hệ thống. Người dùng có thể nhập yêu cầu tự nhiên, ví dụ:

```text
Tôi cần một outfit lịch sự nhưng thoáng mát để đi phỏng vấn mùa hè.
```

Hệ thống sẽ xử lý dựa trên:

```text
Style Profile
+ Product Metadata
+ Inventory Status
+ Vector Search
+ Knowledge Graph Rules
+ Recommendation Signal
```

Kết quả trả về gồm:

* Lời tư vấn phối đồ
* Danh sách sản phẩm phù hợp
* Product cards trong chat
* Lý do vì sao outfit phù hợp
* Nút thêm từng sản phẩm hoặc toàn bộ outfit vào giỏ hàng

---

## Inventory-aware Recommendation

Một nguyên tắc quan trọng của hệ thống:

```text
AI không được gợi ý sản phẩm đã hết hàng.
```

Khi sản phẩm hết hàng, hệ thống cần cập nhật trạng thái tồn kho để AI Stylist không đưa sản phẩm đó vào kết quả gợi ý.

---

## Saga / Checkout Flow

Hệ thống có thiết kế luồng checkout theo hướng xử lý giao dịch phân tán.

Ví dụ luồng thành công:

```text
Tạo đơn hàng
→ Giữ / trừ tồn kho
→ Xử lý thanh toán
→ Xác nhận đơn hàng
```

Nếu thanh toán thất bại:

```text
Tạo đơn hàng
→ Giữ tồn kho
→ Thanh toán thất bại
→ Hoàn lại tồn kho
→ Hủy đơn hàng
```

Ở giai đoạn Frontend, payment được mô phỏng bằng **fake payment flow**.

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
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=StyleMind
```

Lưu ý:

```text
Không commit file .env lên GitHub.
Chỉ commit file .env.example nếu cần.
```

---

## Chạy Backend

Backend sẽ được phát triển bằng Spring Boot Microservices.

Dự kiến chạy local bằng Docker Compose:

```bash
cd BE
docker compose up
```

Hoặc chạy từng service Spring Boot riêng tùy giai đoạn phát triển.

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
* [ ] Xây dựng Spring Boot backend services
* [ ] Kết nối Frontend với Backend API
* [ ] Tích hợp AI Stylist Service
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

Mục tiêu không chỉ là xây dựng một website bán hàng, mà còn là mô phỏng một hệ thống có khả năng tư vấn thời trang cá nhân hóa, quản lý tồn kho thông minh và phân tích hiệu quả gợi ý của AI.
