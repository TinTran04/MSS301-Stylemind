.PHONY: infra-up infra-down full-up full-down logs ps check-ports

# Run only infrastructure (Databases, Redis, MinIO, etc.)
# Use this when running Spring Boot services locally via IntelliJ
infra-up:
	docker compose -f docker-compose.infra.yml up -d

infra-down:
	docker compose -f docker-compose.infra.yml down

# Run the full stack (Infrastructure + all Microservices)
# Use this to run the entire backend system without IntelliJ
full-up:
	docker compose -f docker-compose.full.yml up -d --build

full-down:
	docker compose -f docker-compose.full.yml down

# View logs for full docker compose
logs:
	docker compose -f docker-compose.full.yml logs -f

# View running containers
ps:
	docker ps

# Check ports on localhost
check-ports:
	@echo "Checking infrastructure ports on localhost..."
	@nc -zv localhost 5432 || echo "PostgreSQL (5432) is not running"
	@nc -zv localhost 6379 || echo "Redis (6379) is not running"
	@nc -zv localhost 9000 || echo "MinIO (9000) is not running"
	@nc -zv localhost 6333 || echo "Qdrant (6333) is not running"
	@nc -zv localhost 7687 || echo "Neo4j (7687) is not running"
	@echo "Checking service ports on localhost..."
	@nc -zv localhost 3001 || echo "API Gateway (3001) is not running"
	@nc -zv localhost 8081 || echo "Auth Service (8081) is not running"
	@nc -zv localhost 8082 || echo "User Service (8082) is not running"
	@nc -zv localhost 8083 || echo "Product Service (8083) is not running"
	@nc -zv localhost 8085 || echo "AI Agent Service (8085) is not running"
	@nc -zv localhost 8086 || echo "Cart Service (8086) is not running"
	@nc -zv localhost 8087 || echo "Order Service (8087) is not running"
	@nc -zv localhost 8088 || echo "Payment Service (8088) is not running"
	@nc -zv localhost 8089 || echo "Notification Service (8089) is not running"
