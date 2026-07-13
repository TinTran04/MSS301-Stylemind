# Docker Debug Guide - StyleMind

## Bản chất cốt lõi

### 1. Port Mapping
- **Cơ chế:** Map port từ container ra host để truy cập service từ máy local
- **Cú pháp:** `HOST_PORT:CONTAINER_PORT`
- **Ví dụ:** `8081:8081` → Truy cập `localhost:8081` trên host = `container:8081`
- **Debug port:** Thêm `5005:5005` cho Java remote debugging

### 2. Volume Mounting (Hot-Reload)
- **Cơ chế:** Map thư mục từ host vào container để code changes được reflect
- **Cú pháp:** `HOST_PATH:CONTAINER_PATH:MODE`
- **Ví dụ:** `./src:/app/src:rw` → Code trong `./src` trên host = code trong `/app/src` trong container
- **Lưu ý quan trọng:** Java là compiled language. Mount file `.java` vào container **KHÔNG** tự ăn vào file `app.jar` đang chạy. Cần re-compile `.java` → `.class` rồi mới reload vào JVM.

**2 giải pháp Hot-reload:**
- **Giải pháp 1 (IDE-based):** Chạy `.jar`, dùng IDE compile và push `.class` qua JDWP
- **Giải pháp 2 (Container-based):** Chạy `mvn spring-boot:run` + `spring-boot-devtools`, container tự watch và re-compile

### 3. Remote Debugging
- **Cơ chế:** JVM trong container mở debug port, IDE trên host kết nối vào đó
- **Protocol:** JDWP (Java Debug Wire Protocol)
- **Default port:** 5005
- **Flow:** IDE (host) → JDWP → JVM (container) → Breakpoints hit → Debug session

---

## Cấu hình Dockerfile (Development)

### Approach 1: IDE-based Hot-reload (Run JAR)

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage with debug support
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Debug environment variables
ENV JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

EXPOSE 8081 5005

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Key points:**
- Chạy file `.jar` đã build sẵn
- IDE compile và push `.class` qua JDWP
- Không cần mount source code vào container

### Approach 2: Container-based Hot-reload (Spring Boot DevTools)

```dockerfile
FROM maven:3.9-eclipse-temurin-17
WORKDIR /app

# Copy source code for hot-reload
COPY pom.xml .
COPY src ./src

# Install dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Debug environment variables
ENV JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

EXPOSE 8081 5005

# Run with Spring Boot DevTools for auto-reload
ENTRYPOINT ["sh", "-c", "mvn spring-boot:run -Dspring-boot.run.jvmArguments=\"$JAVA_OPTS\""]
```

**Key points:**
- Chạy `mvn spring-boot:run` thay vì `.jar`
- Container tự watch file changes và re-compile
- Cần mount source code vào container
- `spring-boot-devtools` trong `pom.xml` để enable auto-reload

---

## Cấu hình docker-compose.yml

### Approach 1: IDE-based Hot-reload (Run JAR)

```yaml
services:
  auth-service:
    build:
      context: .
      dockerfile: auth-service/Dockerfile
    ports:
      - "8081:8081"    # Application port
      - "5005:5005"    # Debug port
    environment:
      - JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
      - SPRING_PROFILES_ACTIVE=dev
    networks:
      - stylemind-network
```

**Key points:**
- Port 5005 phải được map
- `JAVA_OPTS` override JDWP configuration
- **KHÔNG** cần mount source code (IDE push `.class` qua JDWP)

### Approach 2: Container-based Hot-reload (Spring Boot DevTools)

```yaml
services:
  auth-service:
    build:
      context: .
      dockerfile: auth-service/Dockerfile
    ports:
      - "8081:8081"    # Application port
      - "5005:5005"    # Debug port
    environment:
      - JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
      - SPRING_PROFILES_ACTIVE=dev
    volumes:
      # Mount source code cho hot-reload
      - ./auth-service/src:/app/src:rw
      - ./auth-service/pom.xml:/app/pom.xml:rw
    networks:
      - stylemind-network
```

**Key points:**
- Port 5005 phải được map
- `JAVA_OPTS` override JDWP configuration
- **CẦN** mount source code vào `/app/src` (WORKDIR)
- `rw` mode cho development
- Container tự watch và re-compile khi file changes

---

## Các lệnh Docker Compose

### Khởi chạy service với debug

```bash
# Build và start service
docker compose -f docker-compose-separated.yml up --build auth-service

# Start ở background
docker compose -f docker-compose-separated.yml up -d auth-service

# View logs
docker compose -f docker-compose-separated.yml logs -f auth-service

# Stop service
docker compose -f docker-compose-separated.yml stop auth-service

# Restart service
docker compose -f docker-compose-separated.yml restart auth-service

# Xóa container và rebuild
docker compose -f docker-compose-separated.yml down
docker compose -f docker-compose-separated.yml up --build auth-service
```

---

## Cấu hình Remote Debug trên IDE

### IntelliJ IDEA

1. **Add Remote Debug Configuration**
   - Run → Edit Configurations → + → Remote JVM Debug
   - Name: `auth-service-debug`
   - Host: `localhost`
   - Port: `5005`
   - Use module classpath: Chọn module tương ứng

2. **Attach Debugger**
   - Start service: `docker compose up -d auth-service`
   - Chọn configuration `auth-service-debug`
   - Click Debug icon (bug icon)
   - Set breakpoints trong code
   - Gọi API → Debugger sẽ hit breakpoints

3. **Hot-reload với Approach 1 (IDE-based)**
   - Sau khi attach debugger, sửa code trong IDE
   - **Ctrl + Shift + F9** (Windows/Linux) hoặc **Cmd + Shift + R** (Mac) → "Reload Changed Classes"
   - IDE compile và push `.class` qua JDWP vào JVM container
   - Changes được apply ngay lập tức mà không cần restart container

### VS Code

1. **Install Extension**
   - Cài `Debugger for Java` extension

2. **Create launch.json**
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "Attach to Remote JVM",
         "request": "attach",
         "hostName": "localhost",
         "port": 5005
       }
     ]
   }
   ```

3. **Attach Debugger**
   - Start service: `docker compose up -d auth-service`
   - F5 → Chọn "Attach to Remote JVM"
   - Set breakpoints
   - Gọi API → Debug session active

4. **Hot-reload với Approach 1 (IDE-based)**
   - Sau khi attach debugger, sửa code trong VS Code
   - **Ctrl + Shift + F9** → "Reload Changed Classes"
   - VS Code compile và push `.class` qua JDWP
   - Changes được apply ngay lập tức

---

## Troubleshooting

### Port conflict
```bash
# Check port đang dùng
netstat -ano | findstr :8081
netstat -ano | findstr :5005

# Kill process đang dùng port
taskkill /PID <PID> /F
```

### Container không start
```bash
# Check logs
docker compose logs auth-service

# Check container status
docker compose ps

# Inspect container
docker inspect <container_id>
```

### Debug port không accessible
```bash
# Test connection
telnet localhost 5005
# Hoặc
nc -zv localhost 5005

# Check nếu port được expose
docker port <container_id>
```

### Hot-reload không work
```bash
# Verify volume mount
docker inspect <container_id> | grep -A 10 Mounts

# Check permissions trong container
docker exec <container_id> ls -la /app/src
```

---

## Best Practices

1. **Development vs Production**
   - Development: Enable debug port, hot-reload volumes
   - Production: Disable debug port, remove hot-reload volumes

2. **Security**
   - Không expose debug port trong production
   - Use `ro` volumes cho production
   - Limit debug port access với firewall

3. **Performance**
   - Hot-reload có thể làm chậm I/O
   - Disable hot-reload khi benchmark
   - Use layer caching trong Dockerfile

4. **Debugging Strategy**
   - Start với log level DEBUG
   - Sử dụng breakpoints thay vì print statements
   - Debug từng service độc lập trước khi test integration
