-- V20: Selective cart checkout, verified administrative codes, shipping methods, vouchers, and snapshots.

ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS province_code INTEGER;
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS district_code INTEGER;
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS ward_code INTEGER;
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS shipping_method VARCHAR(30) NOT NULL DEFAULT 'EXPRESS';
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20) NOT NULL DEFAULT 'COD';
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS subtotal NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS voucher_code VARCHAR(50);
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS voucher_discount_type VARCHAR(20);
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS voucher_discount_value NUMERIC(19, 2);
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS voucher_min_subtotal NUMERIC(19, 2);
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS voucher_max_discount NUMERIC(19, 2);
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE sales_quotes ADD COLUMN IF NOT EXISTS total NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE sales_quotes ALTER COLUMN shipping_fee TYPE NUMERIC(19, 2);

ALTER TABLE sales_quote_items ALTER COLUMN unit_price TYPE NUMERIC(19, 2);
ALTER TABLE sales_quote_items ADD COLUMN IF NOT EXISTS line_total NUMERIC(19, 2) NOT NULL DEFAULT 0;

ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS province_code INTEGER;
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS district_code INTEGER;
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS ward_code INTEGER;
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS shipping_method VARCHAR(30) NOT NULL DEFAULT 'EXPRESS';
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS subtotal NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS voucher_code VARCHAR(50);
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS voucher_discount_type VARCHAR(20);
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS voucher_discount_value NUMERIC(19, 2);
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS voucher_min_subtotal NUMERIC(19, 2);
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS voucher_max_discount NUMERIC(19, 2);
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE sales_orders ALTER COLUMN shipping_fee TYPE NUMERIC(19, 2);
ALTER TABLE sales_orders ALTER COLUMN total TYPE NUMERIC(19, 2);
ALTER TABLE sales_orders ALTER COLUMN payment_method TYPE VARCHAR(20);

ALTER TABLE sales_order_items ALTER COLUMN unit_price TYPE NUMERIC(19, 2);
ALTER TABLE sales_order_items ADD COLUMN IF NOT EXISTS line_total NUMERIC(19, 2) NOT NULL DEFAULT 0;

ALTER TABLE sales_idempotency ALTER COLUMN payment_method TYPE VARCHAR(20);

CREATE TABLE IF NOT EXISTS sales_vouchers (
    code VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('FIXED', 'PERCENT')),
    discount_value NUMERIC(19, 2) NOT NULL CHECK (discount_value > 0),
    min_order_subtotal NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (min_order_subtotal >= 0),
    max_discount_amount NUMERIC(19, 2) CHECK (max_discount_amount IS NULL OR max_discount_amount > 0),
    usage_limit INTEGER CHECK (usage_limit IS NULL OR usage_limit >= 0),
    used_count INTEGER NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    per_user_limit INTEGER NOT NULL DEFAULT 1 CHECK (per_user_limit >= 1),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sales_voucher_usages (
    order_id UUID PRIMARY KEY REFERENCES sales_orders(id) ON DELETE CASCADE,
    voucher_code VARCHAR(50) NOT NULL REFERENCES sales_vouchers(code),
    customer_id UUID NOT NULL REFERENCES users(id),
    discount_amount NUMERIC(19, 2) NOT NULL CHECK (discount_amount >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sales_voucher_usages_customer
    ON sales_voucher_usages(voucher_code, customer_id);

ALTER TABLE sales_vouchers ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_voucher_usages ENABLE ROW LEVEL SECURITY;

INSERT INTO sales_vouchers (code, description, discount_type, discount_value, min_order_subtotal, max_discount_amount, usage_limit, per_user_limit, active, starts_at, expires_at)
VALUES
    ('PIXEL50K', 'Giảm 50.000đ cho đơn từ 1.000.000đ', 'FIXED', 50000, 1000000, NULL, 1000, 3, TRUE, '2025-01-01T00:00:00Z', '2030-12-31T23:59:59Z'),
    ('PIXEL5', 'Giảm 5% tối đa 500.000đ cho đơn từ 5.000.000đ', 'PERCENT', 5, 5000000, 500000, 500, 2, TRUE, '2025-01-01T00:00:00Z', '2030-12-31T23:59:59Z'),
    ('BUILDPC200K', 'Giảm 200.000đ cho đơn từ 15.000.000đ', 'FIXED', 200000, 15000000, NULL, 300, 1, TRUE, '2025-01-01T00:00:00Z', '2030-12-31T23:59:59Z')
ON CONFLICT (code) DO NOTHING;
