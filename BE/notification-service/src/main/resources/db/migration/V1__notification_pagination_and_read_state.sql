-- Adds customer read state and bounded list-query indexes for notification logs.
-- Forward-only and safe to rerun on existing PostgreSQL databases.

ALTER TABLE notification_logs
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_notification_logs_user_created_id
    ON notification_logs(user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_notification_logs_user_read_created_id
    ON notification_logs(user_id, read_at, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_notification_logs_user_unread_created_id
    ON notification_logs(user_id, created_at DESC, id DESC)
    WHERE read_at IS NULL;
