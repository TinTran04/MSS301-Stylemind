-- Init script for ai_db
-- AI Agent Service: Chat Sessions, Messages, Bundles, Analytics, Index Jobs

-- Chat Sessions
CREATE TABLE IF NOT EXISTS chat_sessions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(50),
    context_weather_temp DECIMAL(4, 1),
    context_weather_condition VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chat Messages
CREATE TABLE IF NOT EXISTS chat_messages (
    id VARCHAR(50) PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender_type VARCHAR(10) NOT NULL,
    message_text TEXT NOT NULL,
    has_product_block BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI Curated Bundles (outfits recommended by AI)
CREATE TABLE IF NOT EXISTS ai_curated_bundles (
    id VARCHAR(50) PRIMARY KEY,
    message_id VARCHAR(50) NOT NULL REFERENCES chat_messages(id),
    justification_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
