# Payment Redirect Issue - Technical Documentation

## Issue Summary
**Problem:** After user completes bank transfer via QR code, payment page does not redirect to order success page. Transaction status remains `PENDING` instead of `PAID`.

**Impact:** Users cannot complete checkout flow, leading to abandoned orders and poor UX.

**Priority:** HIGH - Core payment functionality broken

## Root Cause Analysis

### Current Payment Flow
```
1. User creates order → Payment Service creates transaction (status: PENDING)
2. User scans QR code → Transfers money via bank app
3. SePay detects transaction → Should call webhook endpoint
4. Payment Service processes webhook → Updates transaction status to PAID
5. Order Service updates order status → Frontend redirects to success
```

### Failure Point
**Step 3 fails:** SePay webhook callback never reaches the system.

**Why:**
- Webhook endpoint: `http://localhost:3000/api/v1/payments/webhook/sepay`
- SePay server (internet) cannot call `localhost` URLs
- No public webhook URL configured in SePay dashboard
- No fallback mechanism (polling) implemented

### Evidence
1. **Transaction stuck in PENDING:**
   ```sql
   -- Transaction ID: 21fdfe8b6c984eb7a68e46f4dbcab1c6
   -- Order ID: 9cc60c7627cd4370a1aa9f8db1e62a7d
   -- Status: PENDING (should be PAID after transfer)
   -- Paid at: NULL (should have timestamp)
   ```

2. **No webhook logs in Payment Service:**
   ```
   payment-service logs after 12:29:09: No webhook requests received
   ```

3. **Order status remains PAYMENT_PENDING:**
   ```sql
   -- Order ID: 9cc60c7627cd4370a1aa9f8db1e62a7d
   -- Status: PAYMENT_PENDING (should be PAID/COMPLETED)
   ```

## Technical Details

### Webhook Configuration
**Endpoint:** `POST /api/v1/payments/webhook/sepay`

**Routing:**
```
API Gateway (port 3000) → Payment Service (port 8088)
Path: /api/v1/payments/webhook/sepay
Controller: SepayWebhookController.handleSepayWebhook()
```

**Authentication:**
- Header: `Authorization: Apikey <SEPAY_WEBHOOK_API_KEY>`
- API Key: `GZRG6C8WW7ALNAHQM1DYRIGOW0HZRQKM2VFUZBTEMUX9VSPJUTCSLVQYLKNRZOGQ`

**Environment Variables (All configured):**
```env
SEPAY_BANK_SHORT_NAME=VietinBank
SEPAY_ACCOUNT_NUMBER=101876751836
SEPAY_ACCOUNT_NAME=NGUYEN MINH KHOI
SEPAY_BANK_HUB_PREFIX=SEVQR
SEPAY_PAYMENT_EXPIRE_MINUTES=15
SEPAY_QR_BASE_URL=https://img.vietqr.io/image
SEPAY_ENABLED=true
SEPAY_MODE=live
SEPAY_PAYMENT_CODE_PREFIX=STYLEMIND
SEPAY_WEBHOOK_AUTH_MODE=API_KEY
SEPAY_WEBHOOK_API_KEY=GZRG6C8WW7ALNAHQM1DYRIGOW0HZRQKM2VFUZBTEMUX9VSPJUTCSLVQYLKNRZOGQ
```

**Missing:**
```env
SEPAY_WEBHOOK_URL=<public-url>/api/v1/payments/webhook/sepay
```

### Current Architecture
```
SePay Server (Internet)
    ↓ Cannot call
localhost:3000 (Local Docker)
```

## Proposed Solutions

### Solution 1: Public Webhook URL (Primary)
**Implementation:**
1. Use ngrok or similar tunnel service for local development:
   ```bash
   ngrok http 3000
   # Output: https://abc123.ngrok.io
   ```
2. Register public URL in SePay dashboard:
   ```
   Webhook URL: https://abc123.ngrok.io/api/v1/payments/webhook/sepay
   ```
3. Add environment variable:
   ```env
   SEPAY_WEBHOOK_URL=https://abc123.ngrok.io/api/v1/payments/webhook/sepay
   ```

**Pros:**
- Webhook is the intended mechanism
- Real-time payment updates
- No polling overhead

**Cons:**
- Requires public URL (ngrok for local, domain for production)
- External dependency on SePay webhook reliability

### Solution 2: Frontend Polling (Fallback - Recommended for immediate fix)
**Implementation:**
1. Frontend polls payment status every 5-10 seconds
2. When status = PAID → redirect to order success
3. Timeout after 5 minutes (payment expiry)

**Frontend Logic:**
```javascript
// After payment creation
const pollPaymentStatus = async (transactionId) => {
  const maxAttempts = 30; // 5 minutes with 10s intervals
  let attempts = 0;
  
  const interval = setInterval(async () => {
    attempts++;
    const status = await getPaymentStatus(transactionId);
    
    if (status === 'PAID') {
      clearInterval(interval);
      redirectToOrderSuccess();
    } else if (attempts >= maxAttempts) {
      clearInterval(interval);
      showPaymentTimeoutError();
    }
  }, 10000);
};
```

**Pros:**
- No public URL required
- Works in local development
- Immediate user feedback
- Fallback for webhook failures

**Cons:**
- Additional API calls
- Slight delay in redirect
- Polling overhead

### Solution 3: Backend Scheduled Job (Long-term Fallback)
**Implementation:**
1. Scheduled job runs every 1 minute
2. Checks pending payments older than 15 minutes
3. Expires stale payments
4. Logs webhook failures

**Pros:**
- Cleanup of stale payments
- Monitoring of webhook failures
- Data consistency

**Cons:**
- Doesn't solve immediate redirect issue
- Additional backend complexity

### Solution 4: SePay API Integration (Optional)
**Implementation:**
1. Call SePay API to check transaction status
2. Use when webhook fails
3. Requires SePay API documentation

**Pros:**
- Direct verification
- Independent of webhook

**Cons:**
- Requires SePay API access
- Additional external dependency

## Recommended Implementation Plan

### Phase 1: Immediate Fix (Frontend Polling)
1. Implement frontend polling mechanism
2. Add payment status check API endpoint
3. Test payment flow with polling
4. Deploy to production

### Phase 2: Production Webhook Setup
1. Set up public webhook URL (production domain)
2. Register in SePay dashboard
3. Test webhook integration
4. Monitor webhook reliability

### Phase 3: Backend Fallback
1. Implement scheduled job for payment expiry
2. Add webhook failure monitoring
3. Implement SePay API integration (optional)

## Testing Steps

### Manual Testing (Current State)
1. Create order with SePay payment
2. Complete bank transfer
3. **Expected:** Transaction status changes to PAID, order redirects to success
4. **Actual:** Transaction remains PENDING, no redirect

### Testing After Fix
1. **Frontend Polling:**
   - Create order with SePay payment
   - Complete bank transfer
   - Manually update transaction status to PAID in database
   - Verify frontend detects status change and redirects

2. **Webhook Integration:**
   - Set up ngrok tunnel
   - Register webhook URL in SePay dashboard
   - Create order with SePay payment
   - Complete bank transfer
   - Verify webhook callback received
   - Verify transaction status updates to PAID
   - Verify order status updates
   - Verify frontend redirects

## Files to Modify

### Frontend (Polling Implementation)
- Payment page component
- Payment service API calls
- Status polling logic

### Backend (Optional Enhancements)
- Payment Service: Add scheduled job
- Payment Service: Add SePay API integration
- Environment variables: Add SEPAY_WEBHOOK_URL

## Environment Variables Needed

### Current (All configured)
```env
SEPAY_BANK_SHORT_NAME=VietinBank
SEPAY_ACCOUNT_NUMBER=101876751836
SEPAY_ACCOUNT_NAME=NGUYEN MINH KHOI
SEPAY_BANK_HUB_PREFIX=SEVQR
SEPAY_PAYMENT_EXPIRE_MINUTES=15
SEPAY_QR_BASE_URL=https://img.vietqr.io/image
SEPAY_ENABLED=true
SEPAY_MODE=live
SEPAY_PAYMENT_CODE_PREFIX=STYLEMIND
SEPAY_WEBHOOK_AUTH_MODE=API_KEY
SEPAY_WEBHOOK_API_KEY=GZRG6C8WW7ALNAHQM1DYRIGOW0HZRQKM2VFUZBTEMUX9VSPJUTCSLVQYLKNRZOGQ
```

### Additional (For webhook integration)
```env
SEPAY_WEBHOOK_URL=<public-url>/api/v1/payments/webhook/sepay
```

## Database Schema

### Transactions Table
```sql
CREATE TABLE transactions (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_ref VARCHAR(100),
    transfer_content VARCHAR(500),
    gateway_transaction_id VARCHAR(100),
    expires_at TIMESTAMP,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Orders Table
```sql
CREATE TABLE orders (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    order_status VARCHAR(20) NOT NULL,
    shipping_address TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Related Issues
- Invalid cart items with non-existent product variants (fixed)
- Order service PRODUCT_SERVICE_URL configuration (fixed)
- Qdrant healthcheck configuration (fixed)

## Notes
- All other services (Auth, User, Product, Cart, Order, Notification) are functioning normally
- JWT warnings in consumer services are expected behavior
- Webhook endpoint routing is correctly configured in API Gateway
- Payment service webhook processing logic is implemented correctly
- Issue is purely due to lack of public webhook URL for SePay integration

## Contact
For questions about this issue, refer to:
- Payment Service: `BE/payment-service/src/main/java/com/stylemind/payment/`
- API Gateway: `BE/api-gateway/src/main/resources/application.yml`
- Environment Variables: `BE/.env`
