# Local Development Guide - Running Services Without Docker

This guide explains how to run StyleMind backend services directly on your local machine without using Docker Compose.

## Prerequisites

1. **Java 17+** installed
2. **Maven 3.6+** installed
3. **PostgreSQL 15+** running locally (or use Docker for databases only)
4. **Redis** running locally (or use Docker)
5. **Neo4j** running locally (optional, for AI service)
6. **Qdrant** running locally (optional, for AI service)
7. **MinIO** running locally (optional, for product images)

## Quick Setup

### 1. Environment Configuration

The `.env.local` file contains all environment variables needed for local development. It's already configured with:

- **Database URLs**: Pointing to localhost ports 5433-5439
- **Service URLs**: Pointing to localhost service ports 8081-8089
- **JWT Keys**: Pointing to local certificate files
- **External Services**: Cloudinary, SePay, Gmail SMTP

### 2. Database Setup

You have two options for databases:

#### Option A: Use Docker for databases only (Recommended)
```bash
cd BE
docker compose up -d postgres-auth postgres-user postgres-product postgres-cart postgres-order postgres-payment postgres-notification postgres-ai redis
```

#### Option B: Install PostgreSQL locally
Create 9 databases locally:
- `auth_db` (port 5433)
- `user_db` (port 5434)
- `product_db` (port 5435)
- `cart_db` (port 5436)
- `order_db` (port 5437)
- `payment_db` (port 5438)
- `notification_db` (port 5439)
- `ai_db` (port 5430)

Run init scripts to create tables and seed data.

### 3. Generate JWT Keys

```bash
cd BE
.\generate-rsa-keys.ps1
```

This creates RSA keys in `.docker/certs/` directory.

## Running Services

### Method 1: Using Maven (Command Line)

Navigate to each service directory and run:

```bash
cd BE/auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Repeat for all services:
- `auth-service` (port 8081)
- `user-service` (port 8082)
- `product-service` (port 8083)
- `ai-agent-service` (port 8085)
- `cart-service` (port 8086)
- `order-service` (port 8087)
- `payment-service` (port 8088)
- `notification-service` (port 8089)
- `api-gateway` (port 3000)

### Method 2: Using IDE (IntelliJ IDEA / Eclipse)

1. Import the project as a Maven project
2. For each service:
   - Open the main application class
   - Right-click and select "Run" or "Debug"
   - Set active profile to `local` in Run Configuration
   - Set working directory to the service directory

### Method 3: Using Spring Boot Dashboard (IntelliJ)

1. Open Spring Boot Dashboard
2. All services should appear automatically
3. Click the run button for each service
4. Ensure profile is set to `local`

## Service Startup Order

Services should be started in this order:

1. **Infrastructure** (if using Docker):
   ```bash
   docker compose up -d postgres-auth postgres-user postgres-product postgres-cart postgres-order postgres-payment postgres-notification postgres-ai redis
   ```

2. **Core Services**:
   - `auth-service` (8081) - Authentication
   - `user-service` (8082) - User profiles
   - `product-service` (8083) - Product catalog

3. **Business Services**:
   - `cart-service` (8086) - Shopping cart
   - `order-service` (8087) - Order management
   - `payment-service` (8088) - Payment processing
   - `notification-service` (8089) - Notifications
   - `ai-agent-service` (8085) - AI recommendations

4. **Gateway**:
   - `api-gateway` (3000) - API Gateway

## Port Mappings

| Service | Local Port | Database Port | Database Name |
|---------|-----------|--------------|---------------|
| auth-service | 8081 | 5433 | auth_db |
| user-service | 8082 | 5434 | user_db |
| product-service | 8083 | 5435 | product_db |
| ai-agent-service | 8085 | 5430 | ai_db |
| cart-service | 8086 | 5436 | cart_db |
| order-service | 8087 | 5437 | order_db |
| payment-service | 8088 | 5438 | payment_db |
| notification-service | 8089 | 5439 | notification_db |
| api-gateway | 3000 | - | - |

## Environment Variables

The `.env.local` file is automatically loaded by Spring Boot through the `application-local.yml` configuration. Key variables include:

- **JWT Keys**: Point to `.docker/certs/` directory
- **Database URLs**: Point to localhost with correct ports
- **Service URLs**: Point to localhost service ports
- **External Services**: Cloudinary, SePay, Gmail SMTP

## Testing Services

### Test API Gateway
```bash
curl http://localhost:3000/api/health
```

### Test Auth Service
```bash
curl http://localhost:8081/api/auth/health
```

### Test Product Service
```bash
curl http://localhost:8083/api/products
```

## Troubleshooting

### Issue: "Connection refused" to database
**Solution**: Ensure PostgreSQL is running and databases exist on the correct ports.

### Issue: "JWT key file not found"
**Solution**: Run `.\generate-rsa-keys.ps1` to generate RSA keys.

### Issue: "Port already in use"
**Solution**: Change the port in `.env.local` or stop the conflicting service.

### Issue: Services can't communicate
**Solution**: Ensure all services are running and check service URLs in `.env.local`.

### Issue: Flyway migration errors
**Solution**: Check database connection and ensure init scripts have run.

## IDE Configuration

### IntelliJ IDEA

1. **Set working directory** for each service:
   - Run Configuration → Working Directory → Set to service directory (e.g., `BE/auth-service`)

2. **Set active profile**:
   - Run Configuration → Active Profiles → `local`

3. **Enable Spring Boot DevTools** (optional):
   - Add dependency to `pom.xml`
   - Changes will auto-reload

### VS Code

1. Install Spring Boot Extension Pack
2. Configure launch settings in `.vscode/launch.json`
3. Set working directory and profile for each service

## Hot Reload

For development with hot reload:

1. Add Spring Boot DevTools to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

2. Enable in IDE:
   - IntelliJ: Enable "Build project automatically"
   - VS Code: Spring Boot DevTools extension

## Switching Between Docker and Local

### To use Docker:
```bash
cd BE
docker compose up -d
```
- Uses `.env` file
- Uses `application.yml` (no profile)

### To use Local:
```bash
cd BE
# Start databases with Docker (optional)
docker compose up -d postgres-auth postgres-user postgres-product postgres-cart postgres-order postgres-payment postgres-notification postgres-ai redis

# Start services locally
cd auth-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
```
- Uses `.env.local` file
- Uses `application-local.yml` profile

## Debugging

### Remote Debugging

Add JVM arguments to enable remote debugging:
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

Then attach debugger in IDE to port 5005.

## Performance Tips

1. **Increase JVM memory** for services:
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx1024m -Xms512m"
```

2. **Disable unnecessary services** if not needed (e.g., AI service)

3. **Use Docker for databases only** to reduce local resource usage

## Next Steps

After setting up local development:

1. Test all services are running
2. Test API Gateway routing
3. Test authentication flow
4. Test product catalog
5. Test cart and order flow
6. Test payment integration (SePay)
7. Test AI recommendations

## Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
