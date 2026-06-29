# Roadmap — StyleMind

## Sprint 0 — Infrastructure

Completed:

- Bring services online.
- Verify gateway routes and CORS.
- Align JWT secret across services.
- Add Feign timeout defaults.

Remaining:

- Clean obsolete Docker Compose warning.
- Add stronger health checks.
- Standardize env variables.

## Sprint 1 — Core Customer Flow

Completed:

- Connect frontend auth to gateway.
- Connect product catalog to backend.
- Connect guest/auth cart to backend.

Remaining:

- Complete FE → BE checkout/order flow.
- Add order tracking integration.
- Implement `DELETE /api/cart`.
- Clear cart after checkout.
- Fetch authoritative price from product-service.

## Sprint 2 — Admin

Remaining:

- Admin user management.
- Admin order management.
- Product/category polish.
- AI index job management.
- Swagger/OpenAPI accessible from web.

## Sprint 3 — Reliability & Testing

Remaining:

- Contract testing.
- Integration tests with Testcontainers.
- Saga compensation.
- Observability baseline.
- Audit logs.

## Phase 2 — AI Stylist

Planned:

- Product embedding pipeline.
- Qdrant vector search.
- Neo4j fashion graph rules.
- AI re-ranking.
- Recommendation analytics.
- AI bundle generation.
