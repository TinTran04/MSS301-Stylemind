# Testing Strategy — StyleMind

## 1. Test Pyramid

```text
E2E Tests
Contract Tests
Integration Tests
Unit Tests
```

## 2. Backend Testing

| Type | Tool | Scope |
|---|---|---|
| Unit Test | JUnit 5, Mockito | Service logic |
| Integration Test | Spring Boot Test, Testcontainers | Repository/API/database |
| Contract Test | Pact or Spring Cloud Contract | Service-service contracts |
| API Mocking | WireMock | Mock dependent services |

## 3. Frontend Testing

| Type | Tool | Scope |
|---|---|---|
| Unit/component | Vitest, React Testing Library | Components/hooks |
| E2E | Playwright | Login, browse, cart, checkout |
| API mock | MSW | FE API integration |

## 4. Critical Test Scenarios

- Register/login.
- Product listing/detail.
- Guest cart.
- Merge cart after login.
- Checkout success.
- Payment failure.
- Cart clear after checkout.
- Admin product CRUD.
- AI chat.
