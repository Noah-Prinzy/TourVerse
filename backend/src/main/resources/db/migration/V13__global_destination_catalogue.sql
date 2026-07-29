ALTER TABLE destinations
    ADD COLUMN country_code VARCHAR(2);

UPDATE destinations
SET country_code = CASE LOWER(TRIM(country))
    WHEN 'uganda' THEN 'UG'
    WHEN 'kenya' THEN 'KE'
    WHEN 'france' THEN 'FR'
    WHEN 'japan' THEN 'JP'
    WHEN 'united states' THEN 'US'
    WHEN 'united states of america' THEN 'US'
    WHEN 'usa' THEN 'US'
    WHEN 'u.s.a.' THEN 'US'
    ELSE NULL
END;

UPDATE destinations SET country = 'United States' WHERE country_code = 'US';

ALTER TABLE destinations
    ADD CONSTRAINT destinations_country_code_format
        CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$');

CREATE INDEX idx_destinations_country_code ON destinations(country_code);

CREATE TABLE destination_import_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(40) NOT NULL,
    requested_by UUID NOT NULL REFERENCES users(id),
    country_code VARCHAR(2),
    city VARCHAR(100),
    query_text VARCHAR(200),
    requested_limit INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    retrieved_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT destination_import_batches_country_code_format
        CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT destination_import_batches_limit
        CHECK (requested_limit BETWEEN 1 AND 100)
);

CREATE TABLE destination_import_candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES destination_import_batches(id) ON DELETE CASCADE,
    source_provider VARCHAR(40) NOT NULL,
    external_id VARCHAR(200) NOT NULL,
    source_url TEXT,
    name VARCHAR(150) NOT NULL,
    country_code VARCHAR(2),
    country VARCHAR(100) NOT NULL,
    region VARCHAR(100),
    city VARCHAR(100),
    latitude DECIMAL(9, 6),
    longitude DECIMAL(9, 6),
    category_hint VARCHAR(80),
    mapped_category VARCHAR(80),
    description_hint TEXT,
    official_website TEXT,
    image_reference TEXT,
    image_licence VARCHAR(150),
    image_attribution VARCHAR(500),
    image_licence_url TEXT,
    source_classifications TEXT,
    retrieved_at TIMESTAMPTZ NOT NULL,
    review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    rejection_reason VARCHAR(500),
    duplicate_of_destination_id UUID REFERENCES destinations(id),
    approved_destination_id UUID REFERENCES destinations(id),
    reviewed_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT destination_import_candidates_source_unique
        UNIQUE(source_provider, external_id),
    CONSTRAINT destination_import_candidates_country_code_format
        CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT destination_import_candidates_latitude
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT destination_import_candidates_longitude
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT destination_import_candidates_review_status
        CHECK (review_status IN (
            'PENDING_REVIEW', 'APPROVED', 'REJECTED',
            'POSSIBLE_DUPLICATE', 'IMPORT_FAILED'
        ))
);

CREATE TABLE destination_source_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    destination_id UUID NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,
    source_provider VARCHAR(40) NOT NULL,
    external_id VARCHAR(200) NOT NULL,
    source_url TEXT,
    retrieved_at TIMESTAMPTZ NOT NULL,
    last_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT destination_source_references_source_unique
        UNIQUE(source_provider, external_id)
);

CREATE INDEX idx_destination_import_batches_created_at
    ON destination_import_batches(created_at DESC);
CREATE INDEX idx_destination_import_candidates_batch
    ON destination_import_candidates(batch_id);
CREATE INDEX idx_destination_import_candidates_status
    ON destination_import_candidates(review_status);
CREATE INDEX idx_destination_source_references_destination
    ON destination_source_references(destination_id);

INSERT INTO categories (id, name, slug, description)
VALUES
    (gen_random_uuid(), 'Hiking', 'hiking', 'Walking, trekking, and mountain destinations'),
    (gen_random_uuid(), 'Architecture', 'architecture', 'Buildings and designed landmarks'),
    (gen_random_uuid(), 'Museums', 'museums', 'Museums, galleries, and learning destinations'),
    (gen_random_uuid(), 'Religious', 'religious', 'Places associated with faith and religious heritage'),
    (gen_random_uuid(), 'Food', 'food', 'Food markets and culinary destinations'),
    (gen_random_uuid(), 'Nightlife', 'nightlife', 'Evening entertainment destinations'),
    (gen_random_uuid(), 'Urban', 'urban', 'Cities and town-based experiences'),
    (gen_random_uuid(), 'Waterfalls', 'waterfalls', 'Waterfall destinations'),
    (gen_random_uuid(), 'Islands', 'islands', 'Island destinations'),
    (gen_random_uuid(), 'National Parks', 'national-parks', 'Protected national park destinations')
ON CONFLICT (slug) DO NOTHING;
