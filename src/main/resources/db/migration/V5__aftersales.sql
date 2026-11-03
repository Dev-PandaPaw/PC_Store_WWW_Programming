-- V5__aftersales.sql: Aftersales & Warranty (BC-06)

CREATE TABLE IF NOT EXISTS warranty_requests (
    id BIGSERIAL PRIMARY KEY,
    request_code VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    customer_id BIGINT NOT NULL REFERENCES users(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    issue_description TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    staff_notes TEXT,
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS warranty_request_images (
    id BIGSERIAL PRIMARY KEY,
    warranty_request_id BIGINT NOT NULL REFERENCES warranty_requests(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS warranty_request_events (
    id BIGSERIAL PRIMARY KEY,
    warranty_request_id BIGINT NOT NULL REFERENCES warranty_requests(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    actor_id BIGINT REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_warranty_customer ON warranty_requests(customer_id);
CREATE INDEX IF NOT EXISTS idx_warranty_order ON warranty_requests(order_id);
CREATE INDEX IF NOT EXISTS idx_warranty_code ON warranty_requests(request_code);
