-- Fresh local database only. Identity simulation has no password/login implementation.
CREATE TABLE users (
    id UUID PRIMARY KEY, email VARCHAR(255) UNIQUE, phone VARCHAR(16) UNIQUE,
    full_name VARCHAR(150) NOT NULL, status VARCHAR(20) NOT NULL,
    phone_verified_at TIMESTAMPTZ
);
CREATE TABLE user_roles (user_id UUID REFERENCES users(id), role VARCHAR(50), PRIMARY KEY(user_id,role));
CREATE TABLE categories (
    id UUID PRIMARY KEY, name VARCHAR(150) NOT NULL, slug VARCHAR(150) UNIQUE NOT NULL,
    description TEXT, version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE products (
    id UUID PRIMARY KEY, category_id UUID NOT NULL REFERENCES categories(id), sku VARCHAR(100) UNIQUE NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL, name VARCHAR(255) NOT NULL, brand VARCHAR(100), description TEXT,
    price NUMERIC(15,2) NOT NULL, status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', warranty_months SMALLINT,
    stock_on_hand INTEGER NOT NULL DEFAULT 0, reserved_quantity INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE product_specs (
    product_id UUID PRIMARY KEY REFERENCES products(id), cpu VARCHAR(255), gpu VARCHAR(255), ram_gb INTEGER,
    storage_gb INTEGER, storage_type VARCHAR(255), motherboard VARCHAR(255), psu VARCHAR(255),
    case_name VARCHAR(255), operating_system VARCHAR(255)
);
CREATE TABLE product_suitability (
    id UUID PRIMARY KEY, product_id UUID NOT NULL REFERENCES products(id), need_profile VARCHAR(255) NOT NULL,
    score SMALLINT, suitability_reason VARCHAR(255), limitation VARCHAR(255)
);
CREATE TABLE product_images (
    id UUID PRIMARY KEY, product_id UUID REFERENCES products(id), storage_key VARCHAR(255),
    is_cover BOOLEAN NOT NULL DEFAULT FALSE, sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE storefront_products (
    product_id UUID PRIMARY KEY REFERENCES products(id), catalog_key VARCHAR(150) UNIQUE NOT NULL,
    original_price NUMERIC(15,2) NOT NULL, specs JSONB NOT NULL DEFAULT '[]', provenance JSONB NOT NULL DEFAULT '{}', sort_order INTEGER NOT NULL
);
CREATE TABLE storefront_articles (slug VARCHAR(255) PRIMARY KEY, content JSONB NOT NULL, published BOOLEAN NOT NULL DEFAULT TRUE, sort_order INTEGER NOT NULL);
CREATE TABLE carts (
    id UUID PRIMARY KEY, user_id UUID UNIQUE NOT NULL REFERENCES users(id), version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE cart_items (
    cart_id UUID REFERENCES carts(id) ON DELETE CASCADE, product_id UUID REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK(quantity BETWEEN 1 AND 99), PRIMARY KEY(cart_id,product_id)
);
CREATE TABLE cart_merge_receipts (
    guest_id UUID PRIMARY KEY, cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
