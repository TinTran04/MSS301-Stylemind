# Entity Draft — StyleMind

## auth-service

```text
accounts
- id
- email
- password_hash
- provider
- role
- status
- created_at
- updated_at
```

## user-service

```text
profiles
- id
- user_id
- full_name
- phone
- gender
- birth_date
- created_at
- updated_at

addresses
- id
- user_id
- recipient_name
- phone
- line1
- ward
- district
- province
- is_default
```

## product-service

```text
categories
- id
- name
- slug
- parent_id
- status

products
- id
- category_id
- name
- slug
- description
- base_price
- status

product_variants
- id
- product_id
- sku
- color
- size
- price
- status

product_images
- id
- product_id
- variant_id
- image_url
- sort_order
```

## cart-service

```text
carts
- id
- user_id
- guest_id
- status

cart_items
- id
- cart_id
- product_id
- variant_id
- quantity
- price_snapshot
```

## order-service

```text
orders
- id
- user_id
- status
- total_amount
- shipping_address_snapshot
- created_at

order_items
- id
- order_id
- product_id
- variant_id
- product_name_snapshot
- unit_price
- quantity
```

## payment-service

```text
transactions
- id
- order_id
- method
- amount
- status
- provider_reference
- created_at
```
