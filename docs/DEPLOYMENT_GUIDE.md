# HƯỚNG DẪN TRIỂN KHAI HỆ THỐNG STYLEMIND (DEPLOYMENT GUIDE)

Tài liệu này hướng dẫn chi tiết cách thiết lập, cấu hình và chạy hệ thống Stylemind ở môi trường phát triển (Local) và chạy đóng gói bằng Docker Compose.

---

## 1. Cách chạy Local (Chưa có Docker)

Để phát triển dự án độc lập, bạn cần chuẩn bị các môi trường và công cụ sau:

### 1.1. Yêu cầu hệ thống
* **Java:** JDK 17 hoặc JDK 21 (Spring Boot 3.x yêu cầu tối thiểu Java 17).
* **Build Tool:** Apache Maven (hoặc Gradle).
* **PostgreSQL 14+:** Cơ sở dữ liệu quan hệ cho các dịch vụ nghiệp vụ.
* **Qdrant:** Vector Database cho tìm kiếm ngữ nghĩa (có thể tải bản chạy exe hoặc dùng Qdrant Cloud miễn phí).
* **Neo4j 5.x:** Đồ thị tri thức (có thể dùng Neo4j Desktop hoặc Neo4j AuraDB miễn phí).
* **MinIO:** Dùng để chạy máy chủ Object Storage local tương thích S3.

### 1.2. Khởi tạo Cơ sở dữ liệu
Truy cập PostgreSQL client (PgAdmin hoặc DBeaver) và chạy câu lệnh tạo 8 databases riêng biệt cho từng service:
```sql
CREATE DATABASE auth_db;
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE inventory_db;
CREATE DATABASE cart_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE ai_db;
```

### 1.3. Khởi chạy từng Microservice bằng Spring Boot
Với mỗi service trong thư mục `BE/`:
1. Mở file `src/main/resources/application.yml` và cấu hình kết nối Database, Port và các Service URLs tương ứng.
2. Mở Command Prompt / Terminal tại thư mục của service đó và chạy lệnh:
   ```bash
   mvn clean spring-boot:run
   ```

---

## 2. Cách chạy bằng Docker Compose (Đề xuất)

Để khởi chạy toàn bộ hệ thống (gồm cả CSDL và mã nguồn backend) chỉ bằng một câu lệnh, chúng ta cấu hình tệp `docker-compose.yml` ở thư mục gốc của dự án.

### 2.1. File `docker-compose.yml` cấu hình mẫu cho MVP

```yaml
version: '3.8'

services:
  # ----------------------------------------------------
  # 1. Cơ sở dữ liệu & Hạ tầng nền tảng
  # ----------------------------------------------------
  postgres:
    image: postgres:15-alpine
    container_name: stylemind-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata:/var/lib/postgresql/data
      # Tự động tạo 8 databases khi container khởi chạy lần đầu
      - ./init-scripts:/docker-entrypoint-initdb.d
    networks:
      - stylemind-network

  redis:
    image: redis:7-alpine
    container_name: stylemind-redis
    ports:
      - "6379:6379"
    networks:
      - stylemind-network

  qdrant:
    image: qdrant/qdrant:latest
    container_name: stylemind-qdrant
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_data:/qdrant/storage
    networks:
      - stylemind-network

  neo4j:
    image: neo4j:5-community-alpine
    container_name: stylemind-neo4j
    ports:
      - "7474:7474"
      - "7687:7687"
    environment:
      NEO4J_AUTH: neo4j/password
    volumes:
      - neo4j_data:/data
    networks:
      - stylemind-network

  minio:
    image: minio/minio:latest
    container_name: stylemind-minio
    ports:
      - "9000:9000" -- API Port
      - "9001:9001" -- Console Port
    environment:
      MINIO_ROOT_USER: admin
      MINIO_ROOT_PASSWORD: password
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data
    networks:
      - stylemind-network

  # ----------------------------------------------------
  # 2. API Gateway & Dịch vụ Xác thực
  # ----------------------------------------------------
  api-gateway:
    image: stylemind/api-gateway:latest
    container_name: stylemind-gateway
    ports:
      - "3000:3000"
    environment:
      SERVER_PORT: 3000
      JWT_SECRET: ${JWT_SECRET}
      AUTH_SERVICE_URL: http://auth-service:8081
      PRODUCT_SERVICE_URL: http://product-service:8083
      USER_SERVICE_URL: http://user-service:8082
      CART_SERVICE_URL: http://cart-service:8086
      ORDER_SERVICE_URL: http://order-service:8087
      AI_SERVICE_URL: http://ai-agent-service:8085
    depends_on:
      - postgres
    networks:
      - stylemind-network

  auth-service:
    image: stylemind/auth-service:latest
    container_name: auth-service
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/auth_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      - postgres
    networks:
      - stylemind-network

  # ----------------------------------------------------
  # 3. Dịch vụ Nghiệp vụ chính (Business Services)
  # ----------------------------------------------------
  user-service:
    image: stylemind/user-service:latest
    container_name: user-service
    ports:
      - "8082:8082"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/user_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
    depends_on:
      - postgres
    networks:
      - stylemind-network

  product-service:
    image: stylemind/product-service:latest
    container_name: product-service
    ports:
      - "8083:8083"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/product_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      S3_ENDPOINT: http://minio:9000
      S3_ACCESS_KEY: admin
      S3_SECRET_KEY: password
      S3_BUCKET: stylemind-products
      INTERNAL_TOKEN: ${X_INTERNAL_TOKEN}
    depends_on:
      - postgres
      - minio
    networks:
      - stylemind-network

  inventory-service:
    image: stylemind/inventory-service:latest
    container_name: inventory-service
    ports:
      - "8084:8084"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/inventory_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      INTERNAL_TOKEN: ${X_INTERNAL_TOKEN}
    depends_on:
      - postgres
    networks:
      - stylemind-network

  cart-service:
    image: stylemind/cart-service:latest
    container_name: cart-service
    ports:
      - "8086:8086"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/cart_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
    depends_on:
      - postgres
    networks:
      - stylemind-network

  order-service:
    image: stylemind/order-service:latest
    container_name: order-service
    ports:
      - "8087:8087"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/order_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      INVENTORY_SERVICE_URL: http://inventory-service:8084
      PAYMENT_SERVICE_URL: http://payment-service:8088
      INTERNAL_TOKEN: ${X_INTERNAL_TOKEN}
    depends_on:
      - postgres
    networks:
      - stylemind-network

  payment-service:
    image: stylemind/payment-service:latest
    container_name: payment-service
    ports:
      - "8088:8088"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/payment_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      INTERNAL_TOKEN: ${X_INTERNAL_TOKEN}
    depends_on:
      - postgres
    networks:
      - stylemind-network

  notification-service:
    image: stylemind/notification-service:latest
    container_name: notification-service
    ports:
      - "8089:8089"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/notification_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
    depends_on:
      - postgres
    networks:
      - stylemind-network

  # ----------------------------------------------------
  # 4. Dịch vụ Trí tuệ nhân tạo (AI Services)
  # ----------------------------------------------------
  ai-agent-service:
    image: stylemind/ai-agent-service:latest
    container_name: ai-agent-service
    ports:
      - "8085:8085"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ai_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      QDRANT_HOST: qdrant
      QDRANT_PORT: 6333
      NEO4J_URI: bolt://neo4j:7687
      NEO4J_USERNAME: neo4j
      NEO4J_PASSWORD: password
      LLM_API_KEY: ${LLM_API_KEY}
      INTERNAL_TOKEN: ${X_INTERNAL_TOKEN}
      PRODUCT_SERVICE_URL: http://product-service:8083
      INVENTORY_SERVICE_URL: http://inventory-service:8084
      ORDER_SERVICE_URL: http://order-service:8087
    depends_on:
      - postgres
      - qdrant
      - neo4j
    networks:
      - stylemind-network

  # ----------------------------------------------------
  # 5. Giao diện (Frontend Client)
  # ----------------------------------------------------
  stylemind-fe:
    image: stylemind/frontend:latest
    container_name: stylemind-fe
    ports:
      - "80:80"
    environment:
      - VITE_API_GATEWAY=http://localhost:3000/api
    depends_on:
      - api-gateway
    networks:
      - stylemind-network

networks:
  stylemind-network:
    driver: bridge

volumes:
  pgdata:
  qdrant_data:
  neo4j_data:
  minio_data:
```

### 2.2. Khởi chạy hệ thống
Tại thư mục gốc chứa file `docker-compose.yml`, chạy lệnh:
```bash
docker-compose up -d
```

---

## 3. Quản lý Biến môi trường (Environment Variables)

Tạo một tệp tin `.env` ở thư mục gốc để lưu trữ các thông tin cấu hình nhạy cảm. **Không bao giờ đẩy tệp `.env` này lên hệ thống kiểm soát phiên bản (Git).**

```ini
# --- JWT Security Config ---
JWT_SECRET=super-secure-stylemind-secret-key-signature-2026-xyz

# --- AI Model Config ---
# Đặt API Key của nhà cung cấp LLM (như Gemini từ Google hoặc OpenAI)
LLM_API_KEY=AIzaSyD_ExampleKey1234567890abcdef

# --- Internal Security ---
# Token bí mật phục vụ giao tiếp liên dịch vụ (Service-to-Service)
X_INTERNAL_TOKEN=sm-secret-internal-service-token-key-2026
```

---

## 4. Thứ tự khởi chạy hệ thống (Startup Order)

Để đảm bảo các dịch vụ không bị chết do lỗi kết nối khi khởi chạy, thứ tự start các container được khuyến nghị như sau:

1. **Khởi chạy CSDL & Lưu trữ đối tượng (Nhóm 1):**
   `postgres`, `redis`, `qdrant`, `neo4j`, `minio`.
   *Đợi khoảng 15-20 giây để các cơ sở dữ liệu hoàn tất tiến trình khởi tạo và sẵn sàng nhận kết nối.*
2. **Khởi chạy Dịch vụ Định tuyến & Nền tảng (Nhóm 2):**
   `api-gateway`, `auth-service`, `user-service`, `product-service`, `inventory-service`.
3. **Khởi chạy Dịch vụ Nghiệp vụ (Nhóm 3):**
   `cart-service`, `order-service`, `payment-service`, `notification-service`.
4. **Khởi chạy AI Agent & UI (Nhóm 4):**
   `ai-agent-service`, `stylemind-fe`.

---

## 5. Hướng dẫn Debug và Khắc phục lỗi

### 5.1. Lỗi Connection Refused hoặc Unknown Host khi Feign gọi nhau
* **Nguyên nhân:** Lỗi phân giải tên miền (DNS) trong mạng Docker hoặc container chưa khởi động xong.
* **Cách xử lý:** 
  1. Trong Docker, các microservice phải gọi nhau bằng **tên Service** định nghĩa trong file compose (ví dụ: `http://product-service:8083`), tuyệt đối không dùng `localhost:8083` vì `localhost` trong container sẽ trỏ về chính nó.
  2. Kiểm tra danh sách mạng nội bộ:
     ```bash
     docker network inspect stylemind-network
     ```
  3. Kiểm tra xem các container đã nằm chung một network chưa.

### 5.2. Kiểm tra log của một container cụ thể
* Để theo dõi hoạt động và bắt lỗi Java Exception:
  ```bash
  docker logs --tail 100 -f <container-name>
  ```
  *(Ví dụ: `docker logs -f ai-agent-service`)*

### 5.3. Sử dụng Spring Boot Actuator để kiểm tra sức khỏe dịch vụ
* **Kiểm tra trạng thái chung:** Gửi request `GET http://localhost:8085/actuator/health` đến `ai-agent-service` để kiểm tra kết nối với Qdrant và Neo4j.
* Nếu phản hồi trả về `"status": "DOWN"`, Actuator sẽ chỉ rõ lỗi kết nối đến thành phần nào ở bên dưới.

### 5.4. Lỗi "Bucket not found" ở MinIO khi tải ảnh sản phẩm
* **Nguyên nhân:** Bucket `stylemind-products` chưa được khởi tạo ở MinIO.
* **Cách xử lý:** Truy cập giao diện quản trị MinIO Console tại `http://localhost:9001` (User: `admin` / Pass: `password`), tạo mới một bucket có tên `stylemind-products` và chuyển phân quyền truy cập (Access Policy) của bucket đó sang **Public**.
