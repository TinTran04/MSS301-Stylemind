# AI Seed Data Generation Prompt

Copy this prompt and send to any AI (Gemini, ChatGPT, Claude) to generate complete seed data for StyleMind backend.

---

## PROMPT START

You are a database expert. Generate comprehensive seed data for a fashion e-commerce platform called StyleMind.

## DATABASE SCHEMAS

### 1. Auth DB (auth_db)
**Table: users**
- id VARCHAR(50) PRIMARY KEY
- email VARCHAR(100) UNIQUE NOT NULL
- password_hash VARCHAR(255)
- provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL'
- provider_id VARCHAR(100)
- role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER' (values: 'CUSTOMER', 'ADMIN')
- account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' (values: 'ACTIVE', 'DISABLED')
- password_setup_required BOOLEAN NOT NULL DEFAULT false
- password_setup_token_hash VARCHAR(255)
- password_setup_token_expires_at TIMESTAMP
- password_reset_otp_hash VARCHAR(255)
- password_reset_otp_expires_at TIMESTAMP
- password_reset_otp_attempts INTEGER NOT NULL DEFAULT 0
- password_reset_requested_at TIMESTAMP
- password_reset_token_hash VARCHAR(255)
- password_reset_token_expires_at TIMESTAMP
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**Existing users to preserve:**
- usr_admin / admin@stylemind.ai / ADMIN
- usr_customer / customer@stylemind.ai / CUSTOMER

### 2. User DB (user_db)
**Table: customer_style_profiles**
- user_id VARCHAR(50) PRIMARY KEY
- display_name VARCHAR(150)
- gender VARCHAR(20)
- age INT
- height_cm DECIMAL(5, 2)
- weight_kg DECIMAL(5, 2)
- body_morphology VARCHAR(50)
- preferred_fit VARCHAR(30)
- style_personas JSONB
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**Table: delivery_addresses**
- id VARCHAR(50) PRIMARY KEY
- user_id VARCHAR(50) NOT NULL
- recipient_name VARCHAR(100) NOT NULL
- phone_number VARCHAR(20) NOT NULL
- address_line TEXT NOT NULL
- city VARCHAR(100) NOT NULL
- is_default BOOLEAN NOT NULL DEFAULT FALSE
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

### 3. Product DB (product_db)
**Table: products**
- id VARCHAR(50) PRIMARY KEY
- category_id VARCHAR(50)
- name VARCHAR(200) NOT NULL
- description TEXT
- base_price DECIMAL(12,2) NOT NULL
- aesthetic_style VARCHAR(50)
- target_demographic VARCHAR(20) (values: 'MALE', 'FEMALE', 'UNISEX')
- seasonal_property VARCHAR(50)
- status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' (values: 'ACTIVE', 'INACTIVE')
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**Table: product_variants**
- id VARCHAR(50) PRIMARY KEY
- product_id VARCHAR(50) NOT NULL
- sku VARCHAR(50) UNIQUE NOT NULL
- size VARCHAR(20)
- color VARCHAR(50)
- material VARCHAR(100)
- price_override DECIMAL(12,2)
- stock_quantity INT NOT NULL DEFAULT 0
- active BOOLEAN NOT NULL DEFAULT TRUE
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**Note:** Product data already exists with IDs starting with 'SM-PRD-'. Use these existing product IDs in your seed data.

### 4. Cart DB (cart_db)
**Table: shopping_carts**
- id VARCHAR(50) PRIMARY KEY
- user_id VARCHAR(50) UNIQUE
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**Table: cart_items**
- id VARCHAR(50) PRIMARY KEY
- cart_id VARCHAR(50) NOT NULL
- variant_id VARCHAR(50) NOT NULL
- quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0)
- is_ai_recommended BOOLEAN NOT NULL DEFAULT FALSE
- source_bundle_id VARCHAR(50)
- added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

### 5. Order DB (order_db)
**Table: orders**
- id VARCHAR(50) PRIMARY KEY
- user_id VARCHAR(50) NOT NULL
- total_amount DECIMAL(12,2) NOT NULL
- order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' (values: 'PENDING', 'PAYMENT_PENDING', 'PAID', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'FAILED')
- shipping_address TEXT NOT NULL
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**Table: order_items**
- id VARCHAR(50) PRIMARY KEY
- order_id VARCHAR(50) NOT NULL
- variant_id VARCHAR(50) NOT NULL
- quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0)
- price_at_purchase DECIMAL(12,2) NOT NULL
- is_ai_conversion BOOLEAN NOT NULL DEFAULT FALSE
- source_bundle_id VARCHAR(50)
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

### 6. Payment DB (payment_db)
**Table: transactions**
- id VARCHAR(50) PRIMARY KEY
- order_id VARCHAR(50) NOT NULL
- user_id VARCHAR(50) NOT NULL
- amount DECIMAL(12,2) NOT NULL
- method VARCHAR(30) NOT NULL (values: 'SEPAY_QR', 'CASH', 'BANK_TRANSFER')
- status VARCHAR(30) NOT NULL (values: 'PENDING', 'COMPLETED', 'FAILED', 'CANCELLED')
- transaction_ref VARCHAR(100)
- transfer_content VARCHAR(100)
- gateway_transaction_id VARCHAR(100) UNIQUE
- expires_at TIMESTAMP
- paid_at TIMESTAMP
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

### 7. Notification DB (notification_db)
**Table: notification_logs**
- id BIGSERIAL PRIMARY KEY
- user_id VARCHAR(50)
- recipient_email VARCHAR(150)
- type VARCHAR(30) NOT NULL (values: 'ORDER_CONFIRMATION', 'PAYMENT_SUCCESS', 'ORDER_SHIPPED', 'ORDER_PROCESSING', 'WELCOME', 'PASSWORD_RESET')
- channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL'
- title VARCHAR(200)
- content TEXT
- status VARCHAR(20) NOT NULL (values: 'SENT', 'FAILED', 'PENDING')
- error_message VARCHAR(500)
- sent_at TIMESTAMP
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

### 8. AI DB (ai_db)
**Table: chat_sessions**
- id UUID PRIMARY KEY
- user_id VARCHAR(50)
- context_weather_temp DECIMAL(4, 1)
- context_weather_condition VARCHAR(30)
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**Table: chat_messages**
- id VARCHAR(50) PRIMARY KEY
- session_id UUID NOT NULL
- sender_type VARCHAR(10) NOT NULL (values: 'USER', 'AI')
- message_text TEXT NOT NULL
- has_product_block BOOLEAN NOT NULL DEFAULT FALSE
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**Table: ai_curated_bundles**
- id VARCHAR(50) PRIMARY KEY
- message_id VARCHAR(50) NOT NULL
- justification_summary TEXT
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**Table: ai_curated_bundle_items**
- bundle_id VARCHAR(50) NOT NULL
- product_id VARCHAR(50) NOT NULL
- PRIMARY KEY (bundle_id, product_id)

**Table: ai_analytics_logs**
- id VARCHAR(50) PRIMARY KEY
- user_id VARCHAR(50) NOT NULL
- bundle_id VARCHAR(50) NOT NULL
- interaction_type VARCHAR(30) NOT NULL (values: 'IMPRESSION', 'CLICK', 'ADD_TO_CART')
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

**Table: ai_index_jobs**
- id VARCHAR(50) PRIMARY KEY
- target_type VARCHAR(30) NOT NULL (values: 'PRODUCT', 'CATEGORY')
- target_id VARCHAR(50) NOT NULL
- operation_type VARCHAR(10) NOT NULL (values: 'CREATE', 'UPDATE', 'DELETE')
- status VARCHAR(20) NOT NULL (values: 'PENDING', 'COMPLETED', 'FAILED')
- retry_count INT DEFAULT 0
- last_error_message TEXT
- created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

## REQUIREMENTS

Generate INSERT statements for ALL tables with the following specifications:

1. **Users:** Keep existing 2 users (usr_admin, usr_customer), add 5-10 more test users with different roles
2. **Delivery Addresses:** 2-3 addresses per user, with realistic Vietnamese addresses
3. **Shopping Carts:** 1 cart per user
4. **Cart Items:** 3-5 items per cart, using existing product variant SKUs (SM-PRD-XXX-VXX format)
5. **Orders:** 2-3 orders per user with different statuses (COMPLETED, PROCESSING, PENDING)
6. **Order Items:** 2-4 items per order, matching cart items
7. **Transactions:** 1 transaction per order with appropriate status
8. **Notification Logs:** 3-5 notifications per user with different types
9. **Chat Sessions:** 2-3 sessions per user with weather context
10. **Chat Messages:** 4-6 messages per session (USER/AI conversation about fashion)
11. **AI Bundles:** 2-3 bundles per session with justifications
12. **Bundle Items:** 2-4 products per bundle
13. **Analytics Logs:** 3-5 interactions per bundle
14. **Index Jobs:** 5-10 completed jobs for products

## OUTPUT FORMAT

Provide the output as separate SQL files for each database:

```sql
-- 02-user-db-seed.sql
INSERT INTO delivery_addresses (...) VALUES (...);
...

-- 05-cart-db-seed.sql
INSERT INTO shopping_carts (...) VALUES (...);
INSERT INTO cart_items (...) VALUES (...);
...

-- 06-order-db-seed.sql
INSERT INTO orders (...) VALUES (...);
INSERT INTO order_items (...) VALUES (...);
...

-- 07-payment-db-seed.sql
INSERT INTO transactions (...) VALUES (...);
...

-- 09-notification-db-seed.sql
INSERT INTO notification_logs (...) VALUES (...);
...

-- 08-ai-db-seed.sql
INSERT INTO chat_sessions (...) VALUES (...);
INSERT INTO chat_messages (...) VALUES (...);
INSERT INTO ai_curated_bundles (...) VALUES (...);
INSERT INTO ai_curated_bundle_items (...) VALUES (...);
INSERT INTO ai_analytics_logs (...) VALUES (...);
INSERT INTO ai_index_jobs (...) VALUES (...);
```

Use realistic Vietnamese data for names, addresses, and chat messages. Ensure foreign key relationships are correct. Use ON CONFLICT clauses for safe re-runs.

## PROMPT END
