-- Support both the deployed UUID item primary key and V8's composite primary key.
-- Preserve existing carts and quantities; older clients can rely on the UUID default.
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS id UUID DEFAULT gen_random_uuid();
ALTER TABLE cart_items ALTER COLUMN id SET DEFAULT gen_random_uuid();
UPDATE cart_items SET id = gen_random_uuid() WHERE id IS NULL;
ALTER TABLE cart_items ALTER COLUMN id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_cart_items_id ON cart_items(id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_cart_items_cart_product ON cart_items(cart_id, product_id);
CREATE TABLE IF NOT EXISTS cart_merge_receipts (
    guest_id UUID PRIMARY KEY,
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE cart_merge_receipts ENABLE ROW LEVEL SECURITY;
