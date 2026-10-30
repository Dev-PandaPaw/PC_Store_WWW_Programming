-- Add fields required by development identity to the UUID user schema used by cart/orders.
-- Preserve existing users, password hashes, roles and verification timestamps.
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(150);
ALTER TABLE users ADD COLUMN IF NOT EXISTS province VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS district VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS ward VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS address_line VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS status_reason VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS session_version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Legacy verification_challenges may use BIGINT ids and a different OTP layout.
CREATE TABLE IF NOT EXISTS identity_verification_challenges (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    purpose VARCHAR(50) NOT NULL,
    target VARCHAR(255) NOT NULL,
    secret_digest VARCHAR(255) NOT NULL,
    session_binding VARCHAR(255),
    auth_version BIGINT,
    attempts SMALLINT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    resend_after TIMESTAMPTZ,
    verified_at TIMESTAMPTZ,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS identity_challenges_target_purpose
    ON identity_verification_challenges(target, purpose, created_at DESC);
ALTER TABLE identity_verification_challenges ENABLE ROW LEVEL SECURITY;
