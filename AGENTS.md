# StyleMind - Agent Blueprint & Developer Rules

This document is optimized for LLM/AI coding agents to quickly understand the project architecture, port mapping, data model, and existing codebase bugs without scanning the entire workspace. 

---

## 1. System Architecture & Ports

StyleMind is a fashion e-commerce platform with an AI Stylist, utilizing a **ReactJS Frontend** and **Spring Boot Microservices** backend.

```
Frontend React (Port 5173) -> API Gateway (Port 3000) -> 8 Back-end Services + 5 Infrastructures
```

### Infrastructure Components
*   **PostgreSQL**: Port `5432` (Contains 9 databases: `auth_db`, `user_db`, `product_db`, `cart_db`, `order_db`, `payment_db`, `ai_db`, `notification_db`, `inventory_db`)
*   **Redis**: Port `6379` (Caching & Session storage)
*   **Qdrant (Vector DB)**: Port `6333` (Vector search for products)
*   **Neo4j (Graph DB)**: Port `7474` (HTTP), `7687` (Bolt) (Knowledge Graph for fashion taxonomy)
*   **MinIO (S3-compatible Object Storage)**: Port `9000` (API), `9001` (Console) (Image assets storage)

### Microservices Port Map
All external clients route requests via **API Gateway (Port 3000)** under `/api/**`.

| Service | Local Port | Database | Purpose / Scope |
| :--- | :--- | :--- | :--- |
| `api-gateway` | `3000` | None | Gateway, JWT Auth verification, rate limiting |
| `auth-service` | `8081` | `auth_db` | Authentication, RBAC, users management |
| `user-service` | `8082` | `user_db` | Biometric profiles, delivery addresses |
| `product-service` | `8083` | `product_db` | Categories, products, variants, images |
| `ai-agent-service` | `8085` | `ai_db` | Chatbot, AI bundles, analytics, vector indexing |
| `cart-service` | `8086` | `cart_db` | Shopping carts, item selections |
| `order-service` | `8087` | `order_db` | Orders management, Saga state transitions |
| `payment-service` | `8088` | `payment_db` | Simulates payment checkout and refunds |
| `notification-service` | `8089` | `notification_db` | Logs notifications (email/SMS/push) |

*Note: `inventory-service` (port `8084`) and `discovery-service` (Eureka, port `8761`) are optional or referenced in config but not currently present in the codebase.*

---

## 2. Critical Codebase Bugs & Fixes (Must Know)

Before compiling or running the backend, these known issues must be addressed:

### A. Qdrant Client Coordinate Error (Wrong Artifact ID)
*   **Symptoms**: Maven compile fails for `ai-agent-service` claiming `io.qdrant:qdrant-client` is absent.
*   **Location**: [BE/pom.xml](file:///c:/Users/KHAI/Documents/semester%208/MSS301-Code/MSS301-Stylemind/BE/pom.xml) (line 115) and [BE/ai-agent-service/pom.xml](file:///c:/Users/KHAI/Documents/semester%208/MSS301-Code/MSS301-Stylemind/BE/ai-agent-service/pom.xml) (line 55).
*   **Fix**: Change `<artifactId>qdrant-client</artifactId>` to `<artifactId>client</artifactId>`. (Group ID remains `io.qdrant`, version `1.5.0` or newer).

### B. Missing Module in Parent POM
*   **Location**: [BE/pom.xml](file:///c:/Users/KHAI/Documents/semester%208/MSS301-Code/MSS301-Stylemind/BE/pom.xml) under `<modules>`.
*   **Symptom**: `mvn clean install` from the parent directory ignores `ai-agent-service`.
*   **Fix**: Add `<module>ai-agent-service</module>` to the `<modules>` list.

### C. Missing Lombok and List Imports in Feign Client DTOs
*   **Location**: [ProductClient.java](file:///c:/Users/KHAI/Documents/semester%208/MSS301-Code/MSS301-Stylemind/BE/ai-agent-service/src/main/java/com/stylemind/ai/feign/ProductClient.java) and [OrderClient.java](file:///c:/Users/KHAI/Documents/semester%208/MSS301-Code/MSS301-Stylemind/BE/ai-agent-service/src/main/java/com/stylemind/ai/feign/OrderClient.java).
*   **Symptom**: Compilation fails with "cannot find symbol" for Lombok annotations (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) and `List`.
*   **Fix**: Add the following imports:
    ```java
    import lombok.*;
    import java.util.List;
    ```

### D. Missing `InventoryClient.java`
*   **Location**: [AiIndexJobService.java](file:///c:/Users/KHAI/Documents/semester%208/MSS301-Code/MSS301-Stylemind/BE/ai-agent-service/src/main/java/com/stylemind/ai/service/AiIndexJobService.java).
*   **Symptom**: Service calls `InventoryClient` but the interface does not exist in `com.stylemind.ai.feign`.
*   **Fix**: Define the `InventoryClient` interface pointing to `inventory-service` (or mock the return value if inventory is merged into product-service):
    ```java
    package com.stylemind.ai.feign;
    
    import com.stylemind.common.dto.ApiResponse;
    import org.springframework.cloud.openfeign.FeignClient;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.PathVariable;
    
    @FeignClient(name = "inventory-service", url = "${INVENTORY_SERVICE_URL:http://localhost:8084}")
    public interface InventoryClient {
        @GetMapping("/internal/inventory/{sku}")
        ApiResponse<Object> getInventory(@PathVariable("sku") String sku);
    }
    ```

### E. Missing `inventory-service` Directory
*   **Location**: Dockerfiles for all microservices have: `COPY inventory-service/pom.xml inventory-service/pom.xml`.
*   **Symptom**: `docker compose build` fails because the `inventory-service` folder doesn't exist.
*   **Fix**: Either remove these copy lines from the Dockerfiles (if inventory-service is indeed removed) or mock/create a skeleton `inventory-service` directory.

---

## 3. Database Schema Blueprint

PostgreSQL databases are initialized automatically via `docker compose up` using scripts under `BE/init-scripts/`.

1.  **`auth_db`**: `users` (id, email, password_hash, full_name, provider, provider_id, role)
2.  **`user_db`**: `customer_style_profiles` (user_id, gender, age, height_cm, weight_kg, body_morphology, preferred_fit, style_personas), `delivery_addresses` (id, user_id, recipient_name, phone_number, address_line, city, is_default)
3.  **`product_db`**: `categories` (id, name, parent_id, slug), `products` (id, category_id, name, description, base_price, aesthetic_style, target_demographic, seasonal_property, status), `product_variants` (id, product_id, sku, size, color, material, price_override), `product_images` (id, product_id, image_url, is_primary)
4.  **`cart_db`**: `shopping_carts` (id, user_id), `cart_items` (id, cart_id, variant_id, quantity, is_ai_recommended, source_bundle_id)
5.  **`order_db`**: `orders` (id, user_id, total_amount, order_status, shipping_address), `order_items` (id, order_id, variant_id, quantity, price_at_purchase, is_ai_conversion, source_bundle_id)
6.  **`payment_db`**: `transactions` (id, order_id, user_id, amount, method, status, transaction_ref)
7.  **`ai_db`**: `chat_sessions`, `chat_messages`, `ai_curated_bundles`, `ai_curated_bundle_items`, `ai_analytics_logs`, `ai_index_jobs`
8.  **`notification_db`**: `notification_logs` (id, user_id, type, title, content, status, sent_at)

---

## 4. Run & Development Commands

### A. Run Infrastructure via Docker Compose
To run Postgres, Redis, Qdrant, Neo4j, and MinIO in the background:
```bash
cd BE
docker compose up -d postgres redis qdrant neo4j minio
```
Wait 15 seconds for databases to initialize.

### B. Compile Backend Locally
```bash
cd BE
mvn clean install -DskipTests
```

### C. Run Microservices
Run the compiled services locally via CLI or your IDE:
```bash
cd BE/[service-directory]
mvn spring-boot:run
```

### D. Run Frontend React App
1.  Navigate to `FE`:
    ```bash
    cd FE
    ```
2.  Install dependencies:
    ```bash
    npm install
    ```
3.  Ensure `.env` matches backend:
    ```env
    VITE_API_GATEWAY=http://localhost:3000/api
    ```
4.  Start dev server:
    ```bash
    npm run dev
    ```
    Access UI at: `http://localhost:5173`.
