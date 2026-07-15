-- Init script for ai_db
-- AI Agent Service: Chat Sessions, Messages, Bundles, Analytics, Index Jobs

-- Chat Sessions
CREATE TABLE IF NOT EXISTS chat_sessions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(50),
    context_weather_temp DECIMAL(4, 1),
    context_weather_condition VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Chat Messages
CREATE TABLE IF NOT EXISTS chat_messages (
    id VARCHAR(50) PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender_type VARCHAR(10) NOT NULL,
    message_text TEXT NOT NULL,
    has_product_block BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- AI Curated Bundles (outfits recommended by AI)
CREATE TABLE IF NOT EXISTS ai_curated_bundles (
    id VARCHAR(50) PRIMARY KEY,
    message_id VARCHAR(50) NOT NULL REFERENCES chat_messages(id),
    justification_summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- AI Curated Bundle Items (many-to-many between bundles and products)
CREATE TABLE IF NOT EXISTS ai_curated_bundle_items (
    bundle_id VARCHAR(50) NOT NULL REFERENCES ai_curated_bundles(id) ON DELETE CASCADE,
    product_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (bundle_id, product_id)
);

-- AI Analytics Logs (impression, click, add_to_cart)
CREATE TABLE IF NOT EXISTS ai_analytics_logs (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    bundle_id VARCHAR(50) NOT NULL REFERENCES ai_curated_bundles(id),
    interaction_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- AI Index Jobs (for syncing data to Qdrant/Neo4j)
CREATE TABLE IF NOT EXISTS ai_index_jobs (
    id VARCHAR(50) PRIMARY KEY,
    target_type VARCHAR(30) NOT NULL,
    target_id VARCHAR(50) NOT NULL,
    operation_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT DEFAULT 0,
    last_error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_curated_bundles_message_id ON ai_curated_bundles(message_id);
CREATE INDEX IF NOT EXISTS idx_ai_analytics_logs_user_id ON ai_analytics_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_analytics_logs_bundle_id ON ai_analytics_logs(bundle_id);
CREATE INDEX IF NOT EXISTS idx_ai_analytics_logs_interaction_type ON ai_analytics_logs(interaction_type);
CREATE INDEX IF NOT EXISTS idx_ai_index_jobs_status ON ai_index_jobs(status);
CREATE INDEX IF NOT EXISTS idx_ai_index_jobs_target ON ai_index_jobs(target_type, target_id);

-- Seed Data for Chat Sessions
INSERT INTO chat_sessions (id, user_id, context_weather_temp, context_weather_condition)
VALUES
    (gen_random_uuid(), 'usr_customer', 28.5, 'Sunny'),
    (gen_random_uuid(), 'usr_customer', 32.0, 'Hot'),
    (gen_random_uuid(), 'usr_001', 25.0, 'Cloudy'),
    (gen_random_uuid(), 'usr_002', 30.0, 'Rainy'),
    (gen_random_uuid(), 'usr_003', 22.0, 'Sunny')
ON CONFLICT (id) DO NOTHING;

-- Seed Data for Chat Messages
INSERT INTO chat_messages (id, session_id, sender_type, message_text, has_product_block)
VALUES
    ('msg_001', (SELECT id FROM chat_sessions WHERE user_id = 'usr_customer' LIMIT 1), 'USER', 'Tôi cần tìm một bộ đồ đi làm công sở cho mùa hè', false),
    ('msg_002', (SELECT id FROM chat_sessions WHERE user_id = 'usr_customer' LIMIT 1), 'AI', 'Dựa trên yêu cầu của bạn, tôi gợi ý một bộ đồ công sở thoải mái cho mùa hè: Áo sơ mi lụa cổ Đức kết hợp với quần kaki.', true),
    ('msg_003', (SELECT id FROM chat_sessions WHERE user_id = 'usr_customer' LIMIT 1), 'USER', 'Có màu nào khác không?', false),
    ('msg_004', (SELECT id FROM chat_sessions WHERE user_id = 'usr_customer' LIMIT 1), 'AI', 'Có, áo sơ mi có màu trắng, xanh nhạt và ghi. Quần kaki có màu be, đen và xanh navy.', true),
    ('msg_005', (SELECT id FROM chat_sessions WHERE user_id = 'usr_customer' OFFSET 1 LIMIT 1), 'USER', 'Gợi ý đồ đi chơi cuối tuần', false),
    ('msg_006', (SELECT id FROM chat_sessions WHERE user_id = 'usr_customer' OFFSET 1 LIMIT 1), 'AI', 'Cho cuối tuần năng động, tôi gợi ý áo thun oversize kết hợp với quần jeans baggy và áo khoác bomber.', true),
    ('msg_007', (SELECT id FROM chat_sessions WHERE user_id = 'usr_001' LIMIT 1), 'USER', 'Tôi thích phong cách streetwear', false),
    ('msg_008', (SELECT id FROM chat_sessions WHERE user_id = 'usr_001' LIMIT 1), 'AI', 'Tuyệt vời! Tôi gợi ý áo hoodie zip, quần joggers và giày sneaker cao cổ cho phong cách streetwear của bạn.', true)
ON CONFLICT (id) DO NOTHING;

-- Seed Data for AI Curated Bundles
INSERT INTO ai_curated_bundles (id, message_id, justification_summary)
VALUES
    ('bundle_001', 'msg_002', 'Bộ đồ công sở mùa hè với chất liệu thoáng khí, màu sắc trung tính phù hợp môi trường chuyên nghiệp'),
    ('bundle_002', 'msg_004', 'Các lựa chọn màu sắc đa dạng cho áo sơ mi và quần kaki, dễ phối đồ'),
    ('bundle_003', 'msg_006', 'Set đồ casual năng động cho cuối tuần, phom dáng hiện đại và thoải mái'),
    ('bundle_004', 'msg_008', 'Phối đồ streetwear với hoodie và joggers, thoải mái và cá tính')
ON CONFLICT (id) DO NOTHING;

-- Seed Data for AI Curated Bundle Items
INSERT INTO ai_curated_bundle_items (bundle_id, product_id)
VALUES
    ('bundle_001', 'SM-PRD-021'),
    ('bundle_001', 'SM-PRD-033'),
    ('bundle_002', 'SM-PRD-021'),
    ('bundle_002', 'SM-PRD-033'),
    ('bundle_003', 'SM-PRD-035'),
    ('bundle_003', 'SM-PRD-043'),
    ('bundle_003', 'SM-PRD-039'),
    ('bundle_004', 'SM-PRD-042'),
    ('bundle_004', 'SM-PRD-016')
ON CONFLICT (bundle_id, product_id) DO NOTHING;

-- Seed Data for AI Analytics Logs
INSERT INTO ai_analytics_logs (id, user_id, bundle_id, interaction_type)
VALUES
    ('analytics_001', 'usr_customer', 'bundle_001', 'IMPRESSION'),
    ('analytics_002', 'usr_customer', 'bundle_001', 'CLICK'),
    ('analytics_003', 'usr_customer', 'bundle_001', 'ADD_TO_CART'),
    ('analytics_004', 'usr_customer', 'bundle_002', 'IMPRESSION'),
    ('analytics_005', 'usr_customer', 'bundle_003', 'IMPRESSION'),
    ('analytics_006', 'usr_customer', 'bundle_003', 'CLICK'),
    ('analytics_007', 'usr_001', 'bundle_004', 'IMPRESSION'),
    ('analytics_008', 'usr_001', 'bundle_004', 'CLICK'),
    ('analytics_009', 'usr_001', 'bundle_004', 'ADD_TO_CART')
ON CONFLICT (id) DO NOTHING;

-- Seed Data for AI Index Jobs
INSERT INTO ai_index_jobs (id, target_type, target_id, operation_type, status)
VALUES
    ('job_001', 'PRODUCT', 'SM-PRD-001', 'CREATE', 'COMPLETED'),
    ('job_002', 'PRODUCT', 'SM-PRD-002', 'CREATE', 'COMPLETED'),
    ('job_003', 'PRODUCT', 'SM-PRD-003', 'CREATE', 'COMPLETED'),
    ('job_004', 'PRODUCT', 'SM-PRD-035', 'UPDATE', 'COMPLETED'),
    ('job_005', 'PRODUCT', 'SM-PRD-040', 'DELETE', 'COMPLETED'),
    ('job_006', 'CATEGORY', 'ao', 'UPDATE', 'COMPLETED'),
    ('job_007', 'PRODUCT', 'SM-PRD-021', 'CREATE', 'COMPLETED'),
    ('job_008', 'PRODUCT', 'SM-PRD-033', 'CREATE', 'COMPLETED')
ON CONFLICT (id) DO NOTHING;
