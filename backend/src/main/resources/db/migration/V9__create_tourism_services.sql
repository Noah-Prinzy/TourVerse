CREATE TABLE tourism_services (
    id UUID PRIMARY KEY,
    owner_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    destination_id UUID REFERENCES destinations(id) ON DELETE SET NULL,
    name VARCHAR(160) NOT NULL,
    service_type VARCHAR(40) NOT NULL,
    description TEXT,
    phone VARCHAR(40),
    email VARCHAR(255),
    website_url TEXT,
    address TEXT,
    price_from NUMERIC(12,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT tourism_services_price_nonnegative CHECK (price_from IS NULL OR price_from >= 0)
);
CREATE INDEX idx_tourism_services_destination ON tourism_services(destination_id);
CREATE INDEX idx_tourism_services_type ON tourism_services(service_type);
