CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    slug VARCHAR(90) NOT NULL UNIQUE,
    description TEXT,
    icon_url TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_categories_name_lower ON categories (LOWER(name));
CREATE INDEX idx_categories_active ON categories(active);

INSERT INTO categories (id, name, slug, description) VALUES
    (gen_random_uuid(), 'Wildlife', 'wildlife', 'Wildlife parks, reserves, and animal experiences'),
    (gen_random_uuid(), 'Nature', 'nature', 'Natural landscapes and outdoor scenery'),
    (gen_random_uuid(), 'Adventure', 'adventure', 'Adventure activities and active travel'),
    (gen_random_uuid(), 'Culture', 'culture', 'Cultural experiences and local traditions'),
    (gen_random_uuid(), 'Historical', 'historical', 'Historic sites and heritage attractions'),
    (gen_random_uuid(), 'Beaches', 'beaches', 'Beach and lakeside destinations')
ON CONFLICT (slug) DO NOTHING;
