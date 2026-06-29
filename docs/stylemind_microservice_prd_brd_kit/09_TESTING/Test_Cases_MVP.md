# MVP Test Cases — StyleMind

## Auth

| ID | Scenario | Expected |
|---|---|---|
| TC-AUTH-01 | Register valid account | Account created |
| TC-AUTH-02 | Login valid credentials | JWT returned |
| TC-AUTH-03 | Login invalid password | Error response |
| TC-AUTH-04 | Access protected API without JWT | 401 |

## Product

| ID | Scenario | Expected |
|---|---|---|
| TC-PROD-01 | Get product listing | Paginated response |
| TC-PROD-02 | Get product detail | Product with variants/images |
| TC-PROD-03 | Admin create product | Product created |
| TC-PROD-04 | Non-admin create product | 403 |

## Cart

| ID | Scenario | Expected |
|---|---|---|
| TC-CART-01 | Guest add item | Cart item added |
| TC-CART-02 | Customer add item | Cart item added |
| TC-CART-03 | Update quantity to 0 | Validation error |
| TC-CART-04 | Merge guest cart | Items merged |
| TC-CART-05 | Clear cart | Cart empty |

## Checkout

| ID | Scenario | Expected |
|---|---|---|
| TC-ORDER-01 | Checkout with valid cart | Order created |
| TC-ORDER-02 | Checkout empty cart | Error CART_EMPTY |
| TC-ORDER-03 | Payment failed | Order marked failed |
| TC-ORDER-04 | Checkout success | Cart cleared |

## AI

| ID | Scenario | Expected |
|---|---|---|
| TC-AI-01 | Send chat message | AI response returned |
| TC-AI-02 | Get chat history | History returned |
| TC-AI-03 | Admin create index job | Job created |
