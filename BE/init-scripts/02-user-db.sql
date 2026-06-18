-- Init script for user_db
-- Customer Style Profiles and Delivery Addresses

-- Customer Style Profiles (1:1 with users)
CREATE TABLE IF NOT EXISTS customer_style_profiles (
    user_id VARCHAR(50) PRIMARY KEY,
    gender VARCHAR(20),
    age INT,
    height_cm DECIMAL(5, 2),
    weight_kg DECIMAL(5, 2),
    body_morphology VARCHAR(50),
    preferred_fit VARCHAR(30),
    style_personas JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Delivery Addresses
CREATE TABLE IF NOT EXISTS delivery_addresses (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address_line TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_delivery_addresses_user_id ON delivery_addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_delivery_addresses_default ON delivery_addresses(user_id, is_default) WHERE is_default = true;