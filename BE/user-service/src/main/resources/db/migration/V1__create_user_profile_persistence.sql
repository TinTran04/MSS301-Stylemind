CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,
    full_name VARCHAR(150),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    gender VARCHAR(30),
    date_of_birth DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_profiles_phone ON user_profiles(phone);

CREATE TABLE customer_style_profiles (
    user_id VARCHAR(50) PRIMARY KEY,
    gender VARCHAR(20),
    age INTEGER,
    height_cm DECIMAL(5, 2),
    weight_kg DECIMAL(5, 2),
    body_morphology VARCHAR(50),
    preferred_fit VARCHAR(30),
    style_personas JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE delivery_addresses (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address_line TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_delivery_addresses_user_id ON delivery_addresses(user_id);
CREATE INDEX idx_delivery_addresses_default ON delivery_addresses(user_id, is_default);
