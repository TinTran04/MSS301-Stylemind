# Checkout Saga — StyleMind

## 1. Current Problem

Các technical debt cần xử lý:

- Cart chưa được clear sau checkout.
- Order saga chưa có compensation đầy đủ.
- Order price đang đọc từ cart DTO nên có thể stale.

## 2. Proposed Checkout Saga

```text
1. Customer submits checkout
2. order-service gets current cart from cart-service
3. order-service fetches authoritative prices from product-service
4. order-service creates order with status PENDING
5. order-service requests payment-service
6. payment-service creates transaction
7. order-service updates order status
8. notification-service logs notification
9. cart-service clears cart
```

## 3. Compensation Strategy

| Failed Step | Compensation |
|---|---|
| Product price fetch failed | Reject order creation |
| Payment failed | Mark order as PAYMENT_FAILED |
| Notification failed | Keep order success, retry notification later |
| Clear cart failed | Retry clear cart async/scheduled |
| Order update failed after payment | Mark transaction for reconciliation |

## 4. Order Status

```text
PENDING
CONFIRMED
PAYMENT_PENDING
PAID
PROCESSING
SHIPPED
COMPLETED
CANCELLED
FAILED
PAYMENT_FAILED
```

## 5. MVP Recommendation

MVP có thể dùng orchestration trong `order-service`. Chưa cần Kafka nếu scope còn nhỏ.
