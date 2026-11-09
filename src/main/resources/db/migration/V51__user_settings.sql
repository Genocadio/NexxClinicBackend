-- Migration V51: Create user_settings table for user preferences (font size, theme, etc.)
CREATE TABLE IF NOT EXISTS user_settings (
    id UUID PRIMARY KEY,
    worker_id UUID NOT NULL UNIQUE REFERENCES workers(id) ON DELETE CASCADE,
    font_size VARCHAR(32),
    theme VARCHAR(32) DEFAULT 'system',
    settings_json TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_settings_worker_id ON user_settings(worker_id);
