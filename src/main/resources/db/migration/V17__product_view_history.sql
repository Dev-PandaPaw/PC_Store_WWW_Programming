-- One authenticated view per user/product/minute. No client-supplied user/category/time.
CREATE TABLE product_view_events (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    category VARCHAR(150) NOT NULL,
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT date_trunc('minute', CURRENT_TIMESTAMP),
    PRIMARY KEY (user_id, product_id, viewed_at)
);
CREATE INDEX product_view_events_recent_idx ON product_view_events (user_id, viewed_at DESC);
ALTER TABLE product_view_events ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON product_view_events FROM PUBLIC;
-- Supabase Data API roles must not read or forge another account's history.
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='anon') THEN
        REVOKE ALL ON product_view_events FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN
        REVOKE ALL ON product_view_events FROM authenticated;
    END IF;
END $$;
