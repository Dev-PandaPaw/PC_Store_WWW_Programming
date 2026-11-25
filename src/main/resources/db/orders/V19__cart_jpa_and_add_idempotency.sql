-- Ensure pc_build columns exist on carts across all profiles (including order-fixtures).
ALTER TABLE carts ADD COLUMN IF NOT EXISTS pc_build_checkout BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE carts ADD COLUMN IF NOT EXISTS pc_build_requirements JSONB NOT NULL DEFAULT '{}'::jsonb;

-- Idempotency receipts for add-to-cart operations so retries after lost responses
-- (even after reloading the cart and sending a newer cartVersion) never double-add.
CREATE TABLE IF NOT EXISTS cart_add_operations (
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    operation_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    result_version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cart_id, operation_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_add_operations_created_at
    ON cart_add_operations(cart_id, created_at DESC);

ALTER TABLE cart_add_operations ENABLE ROW LEVEL SECURITY;
