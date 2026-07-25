-- Init script for notification_db
-- Notification Logs

-- Notification Logs
CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50),
    recipient_email VARCHAR(150),
    type VARCHAR(30) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    title VARCHAR(200),
    content TEXT,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(500),
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_notification_logs_user_id ON notification_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_logs_status ON notification_logs(status);
CREATE INDEX IF NOT EXISTS idx_notification_logs_created_at ON notification_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_logs_user_created_id ON notification_logs(user_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_notification_logs_user_read_created_id ON notification_logs(user_id, read_at, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_notification_logs_user_unread_created_id ON notification_logs(user_id, created_at DESC, id DESC) WHERE read_at IS NULL;

-- Seed Data for Notification Logs
INSERT INTO notification_logs (user_id, recipient_email, type, channel, title, content, status, sent_at)
VALUES
    ('usr_customer', 'customer@stylemind.ai', 'ORDER_CONFIRMATION', 'EMAIL', 'Đơn hàng đã được xác nhận', 'Đơn hàng #order_001 của bạn đã được xác nhận và đang được xử lý.', 'SENT', NOW() - INTERVAL '2 days'),
    ('usr_customer', 'customer@stylemind.ai', 'PAYMENT_SUCCESS', 'EMAIL', 'Thanh toán thành công', 'Thanh toán cho đơn hàng #order_001 đã thành công với số tiền 687,000 VND.', 'SENT', NOW() - INTERVAL '2 days'),
    ('usr_customer', 'customer@stylemind.ai', 'ORDER_SHIPPED', 'EMAIL', 'Đơn hàng đang giao', 'Đơn hàng #order_001 đang được giao đến địa chỉ của bạn.', 'SENT', NOW() - INTERVAL '1 day'),
    ('usr_customer', 'customer@stylemind.ai', 'ORDER_PROCESSING', 'EMAIL', 'Đơn hàng đang xử lý', 'Đơn hàng #order_002 đang được xử lý.', 'SENT', NOW() - INTERVAL '12 hours'),
    ('usr_admin', 'admin@stylemind.ai', 'WELCOME', 'EMAIL', 'Chào mừng đến với StyleMind', 'Cảm ơn bạn đã đăng ký tài khoản StyleMind.', 'SENT', NOW() - INTERVAL '5 days'),
    ('usr_001', 'nguyenvan.an@example.com', 'WELCOME', 'EMAIL', 'Chào mừng đến với StyleMind', 'Cảm ơn bạn đã đăng ký tài khoản StyleMind.', 'SENT', NOW() - INTERVAL '25 days'),
    ('usr_001', 'nguyenvan.an@example.com', 'ORDER_CONFIRMATION', 'EMAIL', 'Đơn hàng đã được xác nhận', 'Đơn hàng #order_004 của bạn đã được xác nhận.', 'SENT', NOW() - INTERVAL '3 days'),
    ('usr_001', 'nguyenvan.an@example.com', 'ORDER_SHIPPED', 'EMAIL', 'Đơn hàng đang giao', 'Đơn hàng #order_004 đang được giao.', 'SENT', NOW() - INTERVAL '2 days'),
    ('usr_002', 'tranbinh@example.com', 'WELCOME', 'EMAIL', 'Chào mừng đến với StyleMind', 'Cảm ơn bạn đã đăng ký tài khoản StyleMind.', 'SENT', NOW() - INTERVAL '20 days'),
    ('usr_002', 'tranbinh@example.com', 'ORDER_PROCESSING', 'EMAIL', 'Đơn hàng đang xử lý', 'Đơn hàng #order_005 đang được xử lý.', 'SENT', NOW() - INTERVAL '1 day'),
    ('usr_003', 'khanchi@example.com', 'WELCOME', 'EMAIL', 'Chào mừng đến với StyleMind', 'Cảm ơn bạn đã đăng ký tài khoản StyleMind.', 'SENT', NOW() - INTERVAL '15 days'),
    ('usr_003', 'khanchi@example.com', 'ORDER_CONFIRMATION', 'EMAIL', 'Đơn hàng đã được xác nhận', 'Đơn hàng #order_006 của bạn đã được xác nhận.', 'SENT', NOW() - INTERVAL '1 day'),
    ('usr_004', 'tiendung@example.com', 'WELCOME', 'EMAIL', 'Chào mừng đến với StyleMind', 'Cảm ơn bạn đã đăng ký tài khoản StyleMind.', 'SENT', NOW() - INTERVAL '10 days'),
    ('usr_004', 'tiendung@example.com', 'PAYMENT_FAILED', 'EMAIL', 'Thanh toán thất bại', 'Thanh toán cho đơn hàng #order_007 đã thất bại. Vui lòng thử lại.', 'SENT', NOW() - INTERVAL '8 hours'),
    ('usr_005', 'myem@example.com', 'WELCOME', 'EMAIL', 'Chào mừng đến với StyleMind', 'Cảm ơn bạn đã đăng ký tài khoản StyleMind.', 'SENT', NOW() - INTERVAL '5 days'),
    ('usr_005', 'myem@example.com', 'ORDER_CONFIRMATION', 'EMAIL', 'Đơn hàng đã được xác nhận', 'Đơn hàng #order_008 của bạn đã được xác nhận.', 'SENT', NOW() - INTERVAL '6 hours')
ON CONFLICT DO NOTHING;
