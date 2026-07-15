-- Init script for user_db
-- Customer Style Profiles and Delivery Addresses

-- Customer Style Profiles (1:1 with users)
CREATE TABLE IF NOT EXISTS customer_style_profiles (
    user_id VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(150),
    gender VARCHAR(20),
    age INT,
    height_cm DECIMAL(5, 2),
    weight_kg DECIMAL(5, 2),
    body_morphology VARCHAR(50),
    preferred_fit VARCHAR(30),
    style_personas JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Delivery Addresses
CREATE TABLE IF NOT EXISTS delivery_addresses (
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

-- Add updated_at column if it doesn't exist (for existing databases)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'delivery_addresses' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE delivery_addresses ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;
END $$;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_delivery_addresses_user_id ON delivery_addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_delivery_addresses_default ON delivery_addresses(user_id, is_default) WHERE is_default = true;

-- Seed Data for Customer Style Profiles
INSERT INTO customer_style_profiles (
    user_id, display_name, gender, age, height_cm, weight_kg, body_morphology, preferred_fit, style_personas, created_at, updated_at
) VALUES 
('usr_admin', 'System Admin', 'MALE', 30, 175.0, 70.0, 'Mesomorph', 'REGULAR', '{"minimalist": 0.8, "casual": 0.2}', NOW() - INTERVAL '60 days', NOW() - INTERVAL '60 days'),
('usr_customer', 'Khách Hàng Thử Nghiệm', 'MALE', 25, 172.5, 65.0, 'Ectomorph', 'REGULAR', '{"casual": 0.7, "minimalist": 0.3}', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'),
('usr_001', 'Nguyễn Văn An', 'MALE', 28, 175.0, 70.0, 'Mesomorph', 'LOOSE', '{"streetwear": 0.8, "sporty": 0.2}', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
('usr_002', 'Trần Thị Bình', 'FEMALE', 23, 158.0, 48.0, 'Hourglass', 'SLIM', '{"vintage": 0.5, "elegant": 0.5}', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
('usr_003', 'Lê Khánh Chi', 'FEMALE', 30, 162.0, 52.0, 'Pear', 'REGULAR', '{"office": 0.8, "minimalist": 0.2}', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),
('usr_004', 'Phạm Tiến Dũng', 'MALE', 35, 180.0, 85.0, 'Endomorph', 'OVERSIZED', '{"streetwear": 0.5, "casual": 0.5}', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
('usr_005', 'Hoàng Mỹ Em', 'FEMALE', 19, 155.0, 45.0, 'Rectangle', 'LOOSE', '{"korean_chic": 0.9, "indie": 0.1}', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days')
ON CONFLICT (user_id) DO NOTHING;

-- Seed Data for Delivery Addresses
INSERT INTO delivery_addresses (
    id, user_id, recipient_name, phone_number, address_line, city, is_default, created_at, updated_at
) VALUES 
('adr_001', 'usr_customer', 'Nguyễn Văn A', '0901234567', 'Chung cư Sunrise City, Đường Nguyễn Hữu Thọ, Phường Tân Hưng, Quận 7', 'Hồ Chí Minh', true, NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'),
('adr_002', 'usr_customer', 'Nguyễn Văn A (Văn phòng)', '0901234568', 'Tòa nhà FPT, Khu công nghệ cao, Quận 9', 'Hồ Chí Minh', false, NOW() - INTERVAL '29 days', NOW() - INTERVAL '29 days'),
('adr_003', 'usr_001', 'Nguyễn Văn An', '0912345678', 'Số 15 Ngõ 102, Đường Khuất Duy Tiến, Quận Thanh Xuân', 'Hà Nội', true, NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
('adr_004', 'usr_002', 'Trần Thị Bình', '0923456789', '245 Lê Duẩn, Phường Tân Chính, Quận Thanh Khê', 'Đà Nẵng', true, NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
('adr_005', 'usr_002', 'Trần Thị Bình (Cơ quan)', '0923456789', '84 Hùng Vương, Quận Hải Châu', 'Đà Nẵng', false, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days'),
('adr_006', 'usr_003', 'Lê Khánh Chi', '0934567890', 'Căn hộ 15.04, Block B, Masteri Thảo Điền, Quận 2', 'Hồ Chí Minh', true, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),
('adr_007', 'usr_004', 'Phạm Tiến Dũng', '0945678901', '45 Hoàng Diệu, Phường Phú Hội', 'Huế', true, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
('adr_008', 'usr_005', 'Hoàng Mỹ Em', '0956789012', 'Số 12 Nguyễn Văn Cừ, Quận Ninh Kiều', 'Cần Thơ', true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days')
ON CONFLICT (id) DO NOTHING;
