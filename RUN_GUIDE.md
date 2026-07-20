# Hướng Dẫn Chạy Hệ Thống StyleMind (BE + FE) Từ A Đến Z

Tài liệu này tổng hợp toàn bộ các bước cấu hình, khởi tạo dữ liệu và câu lệnh để chạy đầy đủ các thành phần của hệ thống **StyleMind** trên môi trường local (Windows).

---

## Kiến Trúc Tổng Quan & Cổng Kết Nối (Ports)

| Dịch vụ | Port | Ghi chú |
| :--- | :--- | :--- |
| **Frontend (Vite + React)** | **5173** | Giao diện người dùng chính |
| **API Gateway (Java BE)** | **3000** | Đầu mối tiếp nhận của FE → route sang các dịch vụ khác |
| **Auth Service** | **8081** | Xác thực, đăng ký, đăng nhập, JWT |
| **User Service** | **8082** | Hồ sơ khách hàng, địa chỉ giao hàng |
| **Product Service** | **8083** | Quản lý danh mục sản phẩm, biến thể, ảnh |
| **AI Agent Service** | **8085** | Chatbot AI Stylist, bundles, index |
| **Cart Service** | **8086** | Giỏ hàng (Guest + User) |
| **Order Service** | **8087** | Quản lý đơn hàng, checkout flow |
| **Payment Service** | **8088** | Thanh toán COD + SePay VietQR |
| **Notification Service** | **8089** | Email OTP, thông báo |
| **PostgreSQL** | **5432** | Cơ sở dữ liệu chính (9 databases) |
| **Redis** | **6379** | Quản lý cache và rate-limit gateway |
| **Qdrant (Vector DB)** | **6333** | Tìm kiếm Vector sản phẩm |
| **Neo4j (Graph DB)** | **7687 / 7474** | Lưu trữ luật thời trang |
| **MinIO (Storage)** | **9000 / 9001** | Lưu trữ ảnh sản phẩm |

---

## Yêu Cầu Chuẩn Bị Trước

1. **Docker Desktop**: Cấu hình RAM cho WSL2 / Docker Desktop tối thiểu **8GB - 12GB** (do phải chạy 9 microservices Java cùng lúc).
2. **Node.js** (phiên bản v18 trở lên) để chạy Frontend.
3. **Git for Windows** (cần `openssl` để sinh khóa RSA JWT).

---

## BƯỚC 1: Sinh Khóa RSA JWT (Chỉ Làm Lần Đầu)

Hệ thống xác thực sử dụng JWT với cặp khóa RSA-2048. Chạy lệnh sau **tại thư mục gốc dự án** (`D:\MSS-ASS-Project`):

```powershell
.\generate-rsa-keys.ps1
```

> 💡 *Script sẽ tạo thư mục `.docker/certs/` chứa `private_key.pem` và `public_key.pem`. Các file này được mount vào Docker containers qua volumes.*

Nếu PowerShell báo lỗi không tìm thấy `openssl`, thêm Git vào PATH tạm thời:
```powershell
$env:Path += ";C:\Program Files\Git\usr\bin"
.\generate-rsa-keys.ps1
```

---

## BƯỚC 2: Cấu Hình & Chạy Backend (Java BE + Infra)

Thực hiện các lệnh sau tại thư mục **`D:\MSS-ASS-Project\BE`**:

### 1. Cấu hình biến môi trường
Sao chép tệp mẫu cấu hình:
```powershell
Copy-Item .env.example .env
```
Mở tệp `.env` vừa tạo và cập nhật các giá trị quan trọng:
- **`SEPAY_WEBHOOK_API_KEY`** — API key webhook từ SePay Dashboard
- **`SEPAY_ACCOUNT_NUMBER`** / **`SEPAY_ACCOUNT_NAME`** — Thông tin tài khoản ngân hàng SePay
- **`LLM_API_KEY`** — API key cho AI Stylist (nếu sử dụng)
- **`CLOUDINARY_*`** — Thông tin Cloudinary (nếu upload ảnh qua Cloudinary)

> ⚠️ *Không commit file `.env` lên GitHub. File `.env.example` chứa giá trị mẫu để tham khảo.*

### 2. Sửa lỗi kết thúc dòng trên Windows (CRLF → LF)
Chạy lệnh PowerShell sau để đảm bảo script khởi tạo DB chạy thành công trong container Linux:
```powershell
((Get-Content -Raw init-scripts\00-create-databases.sh) -replace "`r`n", "`n") | Set-Content -NoNewline init-scripts\00-create-databases.sh
```

### 3. Lựa chọn khởi động Backend

Dự án sử dụng **1 file `docker-compose.yml`** duy nhất với Docker Compose **profiles**:

| Profile | Mô tả | Dùng khi |
| :--- | :--- | :--- |
| `infra` | Chỉ chạy hạ tầng (PostgreSQL, Redis, Qdrant, Neo4j, MinIO) | Phát triển qua IDE (IntelliJ) |
| `app` | Hạ tầng + tất cả microservices Java | Chạy toàn bộ backend không cần IDE |
| `all` | Bao gồm tất cả (tương đương `app`) | Alias tiện dùng |

*   **Cách A: Chạy toàn bộ microservices + hạ tầng bằng Docker (Khuyên dùng)**
    ```powershell
    docker compose --profile app up -d --build
    ```

*   **Cách B: Chỉ khởi động hạ tầng bằng Docker (Nếu muốn chạy các service Java qua IDE/IntelliJ)**
    ```powershell
    docker compose --profile infra up -d
    ```

> 💡 *Đợi khoảng 60–90 giây để các cơ sở dữ liệu và hạ tầng (đặc biệt là Neo4j) khởi động hoàn tất. Gõ `docker ps` để kiểm tra các container báo trạng thái `healthy`.*

### 4. Kiểm tra trạng thái các container
```powershell
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

---

## BƯỚC 3: Cấu Hình & Khởi Chạy Frontend (UI)

Thực hiện các lệnh sau tại thư mục **`D:\MSS-ASS-Project\FE`**:

### 1. Cấu hình biến môi trường
Sao chép tệp mẫu cấu hình:
```powershell
Copy-Item .env.example .env
```
*(Mặc định `VITE_API_BASE_URL=http://localhost:3000` trỏ về API Gateway, không cần sửa.)*

### 2. Cài đặt các thư viện Frontend
```powershell
npm install
```

### 3. Khởi chạy Frontend React Dev Server
```powershell
npm run dev
```

Sau khi chạy thành công, truy cập giao diện tại: **http://localhost:5173**

---

## BƯỚC 4: Kiểm Tra Hệ Thống & Kiểm Thử (QA)

### 1. Kiểm tra sức khỏe (Health Check)
Mở cửa sổ PowerShell mới để kiểm tra trạng thái hoạt động của API Gateway và các service:
```powershell
# API Gateway
Invoke-RestMethod http://localhost:3000/actuator/health

# Auth Service
Invoke-RestMethod http://localhost:8081/actuator/health

# Product Service
Invoke-RestMethod http://localhost:8083/actuator/health
```

### 2. Đăng nhập và Test giao diện người dùng
1. Truy cập: **http://localhost:5173**
2. Đăng nhập với một trong hai tài khoản kiểm thử mặc định:
   *   **Tài khoản Admin:**
       - **Email:** `admin@stylemind.ai`
       - **Password:** `Admin@123` *(chữ A viết hoa)*
   *   **Tài khoản Customer:**
       - **Email:** `customer@stylemind.ai`
       - **Password:** `Customer@123` *(chữ C viết hoa)*

### 3. Xem logs Docker (khi gặp sự cố)
```powershell
# Xem logs toàn bộ services
docker compose --profile app logs -f

# Xem logs 1 service cụ thể
docker compose logs -f auth-service
docker compose logs -f order-service
```

---

## Tắt Hệ Thống

### Tắt toàn bộ (giữ lại dữ liệu)
```powershell
cd D:\MSS-ASS-Project\BE
docker compose --profile app down
```

### Tắt và xóa toàn bộ dữ liệu (reset sạch)
```powershell
cd D:\MSS-ASS-Project\BE
docker compose --profile app down -v
```

> ⚠️ *Lệnh `down -v` sẽ xóa toàn bộ volumes (databases, ảnh sản phẩm...). Chỉ dùng khi muốn làm mới hoàn toàn từ đầu.*

---

## Xử Lý Sự Cố Thường Gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
| :--- | :--- | :--- |
| FE báo "Network Error" | API Gateway chưa khởi động xong | Đợi 60–90s, kiểm tra `docker ps` |
| Đăng nhập báo lỗi 401 | auth-service chưa healthy | `docker compose logs -f auth-service` |
| Đăng ký không gửi email OTP | notification-service chưa healthy hoặc thiếu cấu hình SMTP | Kiểm tra `SPRING_MAIL_*` trong `.env` |
| Ảnh sản phẩm không hiển thị | MinIO chưa healthy hoặc bucket chưa tạo | Truy cập MinIO Console http://localhost:9001 (admin/password) |
| Container bị OOM Killed | Docker Desktop thiếu RAM | Tăng RAM lên 8–12GB trong Docker Desktop Settings |
| Port đã bị chiếm | Service khác đang dùng port | `netstat -ano | findstr :3000` rồi tắt process xung đột |
