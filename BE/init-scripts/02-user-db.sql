-- Init script for user_db
-- Basic customer profiles and delivery addresses

CREATE TABLE IF NOT EXISTS user_profiles (
    user_id VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Local administrative lookup data is imported by User Service from its pinned
-- bundled dataset after these tables are created.
CREATE TABLE IF NOT EXISTS administrative_provinces (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    data_version VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS administrative_wards (
    code VARCHAR(10) PRIMARY KEY,
    province_code VARCHAR(10) NOT NULL REFERENCES administrative_provinces(code),
    name VARCHAR(150) NOT NULL,
    type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    data_version VARCHAR(50) NOT NULL
);

-- Delivery Addresses
CREATE TABLE IF NOT EXISTS delivery_addresses (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address_line TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    province_code VARCHAR(10),
    province_name VARCHAR(150),
    ward_code VARCHAR(10),
    ward_name VARCHAR(150),
    shipping_note TEXT,
    validation_status VARCHAR(30) NOT NULL DEFAULT 'LEGACY_UNVERIFIED'
        CONSTRAINT ck_delivery_addresses_validation_status CHECK (validation_status IN ('VALID', 'LEGACY_UNVERIFIED')),
    administrative_data_version VARCHAR(50),
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
CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_addresses_one_default_per_user ON delivery_addresses(user_id) WHERE is_default = true;
CREATE INDEX IF NOT EXISTS idx_admin_provinces_active_name ON administrative_provinces(active, name);
CREATE INDEX IF NOT EXISTS idx_admin_wards_province_active_name ON administrative_wards(province_code, active, name);
CREATE INDEX IF NOT EXISTS idx_delivery_addresses_validation_status ON delivery_addresses(user_id, validation_status);

-- Seed data retains basic display names only; deprecated style preferences are not seeded.
INSERT INTO user_profiles (user_id, display_name, created_at, updated_at) VALUES
('usr_admin', 'System Admin', NOW() - INTERVAL '60 days', NOW() - INTERVAL '60 days'),
('usr_customer', 'Khách Hàng Thử Nghiệm', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'),
('usr_001', 'Nguyễn Văn An', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
('usr_002', 'Trần Thị Bình', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
('usr_003', 'Lê Khánh Chi', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),
('usr_004', 'Phạm Tiến Dũng', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
('usr_005', 'Hoàng Mỹ Em', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days')
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
