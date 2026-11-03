-- Separate sales aggregate: legacy orders/payment tables have incompatible historical layouts.
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMPTZ;
CREATE TABLE sales_quotes (
    id UUID PRIMARY KEY, customer_id UUID NOT NULL REFERENCES users(id), cart_version BIGINT NOT NULL,
    recipient VARCHAR(100) NOT NULL, phone VARCHAR(16) NOT NULL, address TEXT NOT NULL,
    shipping_fee NUMERIC(19,0) NOT NULL, expires_at TIMESTAMPTZ NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE sales_quote_items (
    quote_id UUID NOT NULL REFERENCES sales_quotes(id), product_id UUID NOT NULL REFERENCES products(id),
    name VARCHAR(255) NOT NULL, sku VARCHAR(100) NOT NULL, unit_price NUMERIC(19,0) NOT NULL,
    quantity INTEGER NOT NULL CHECK(quantity BETWEEN 1 AND 99), warranty_months INTEGER NOT NULL,
    PRIMARY KEY(quote_id, product_id)
);
CREATE TABLE sales_orders (
    id UUID PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE, customer_id UUID NOT NULL REFERENCES users(id),
    quote_id UUID NOT NULL UNIQUE REFERENCES sales_quotes(id), email VARCHAR(255),
    recipient VARCHAR(100) NOT NULL, phone VARCHAR(16) NOT NULL, address TEXT NOT NULL,
    shipping_fee NUMERIC(19,0) NOT NULL, total NUMERIC(19,0) NOT NULL,
    payment_method VARCHAR(10) NOT NULL, status VARCHAR(30) NOT NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID', reservation_status VARCHAR(20) NOT NULL DEFAULT 'HELD',
    version BIGINT NOT NULL DEFAULT 0, expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE sales_order_items (
    order_id UUID NOT NULL REFERENCES sales_orders(id), product_id UUID NOT NULL REFERENCES products(id),
    name VARCHAR(255) NOT NULL, sku VARCHAR(100) NOT NULL, unit_price NUMERIC(19,0) NOT NULL,
    quantity INTEGER NOT NULL CHECK(quantity BETWEEN 1 AND 99), warranty_months INTEGER NOT NULL,
    PRIMARY KEY(order_id, product_id)
);
CREATE TABLE sales_payment_attempts (
    order_id UUID PRIMARY KEY REFERENCES sales_orders(id), reference VARCHAR(50) NOT NULL UNIQUE,
    amount NUMERIC(19,0) NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    transaction_code VARCHAR(100), payment_url TEXT
);
CREATE TABLE sales_history (
    id BIGSERIAL PRIMARY KEY, order_id UUID NOT NULL REFERENCES sales_orders(id),
    from_status VARCHAR(30), to_status VARCHAR(30) NOT NULL, actor_id UUID REFERENCES users(id),
    reason VARCHAR(500) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE sales_idempotency (
    customer_id UUID NOT NULL REFERENCES users(id), key VARCHAR(100) NOT NULL,
    quote_id UUID NOT NULL, payment_method VARCHAR(10) NOT NULL, order_id UUID NOT NULL REFERENCES sales_orders(id),
    PRIMARY KEY(customer_id, key)
);
CREATE TABLE sales_payment_events (
    id BIGSERIAL PRIMARY KEY, order_id UUID NOT NULL REFERENCES sales_orders(id),
    transaction_code VARCHAR(100), response_code VARCHAR(10), transaction_status VARCHAR(10),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX sales_orders_customer ON sales_orders(customer_id, created_at DESC);
CREATE INDEX sales_orders_expiry ON sales_orders(expires_at) WHERE status IN ('PENDING_PAYMENT','PENDING_CONFIRMATION');
CREATE INDEX sales_history_order ON sales_history(order_id, id);
ALTER TABLE sales_quotes ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_quote_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_payment_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_idempotency ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_payment_events ENABLE ROW LEVEL SECURITY;
