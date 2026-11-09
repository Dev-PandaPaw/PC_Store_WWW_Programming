-- V15__create_user_activity_logs.sql
-- Bang luu tru nhat ky hanh vi nguoi dung (User Activity Tracking)
-- Phuc vu phan tich so thich nguoi dung va Spring AI Personalization

CREATE TABLE IF NOT EXISTS user_activity_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    session_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    product_id UUID,
    category_slug VARCHAR(100),
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_activity_session ON user_activity_logs(session_id);
CREATE INDEX IF NOT EXISTS idx_user_activity_user ON user_activity_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_user_activity_created ON user_activity_logs(created_at);

