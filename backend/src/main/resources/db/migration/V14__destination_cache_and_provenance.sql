ALTER TABLE destinations
    ADD COLUMN data_origin VARCHAR(30) NOT NULL DEFAULT 'TOURVERSE_CURATED',
    ADD COLUMN cache_status VARCHAR(30) NOT NULL DEFAULT 'NOT_APPLICABLE',
    ADD COLUMN last_verified_at TIMESTAMPTZ,
    ADD COLUMN expires_at TIMESTAMPTZ,
    ADD COLUMN content_hash VARCHAR(64),
    ADD COLUMN verification_status VARCHAR(30) NOT NULL DEFAULT 'VERIFIED',
    ADD COLUMN verification_confidence DECIMAL(5, 4),
    ADD COLUMN editorially_locked BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE destinations
    ADD CONSTRAINT destinations_data_origin CHECK (
        data_origin IN ('TOURVERSE_CURATED', 'EXTERNAL', 'HYBRID', 'DEVELOPMENT_SEED')
    ),
    ADD CONSTRAINT destinations_cache_status CHECK (
        cache_status IN ('FRESH', 'STALE', 'REFRESH_PENDING', 'REFRESH_FAILED', 'NOT_APPLICABLE')
    ),
    ADD CONSTRAINT destinations_verification_status CHECK (
        verification_status IN ('VERIFIED', 'PARTIALLY_VERIFIED', 'REVIEW_REQUIRED', 'REJECTED')
    ),
    ADD CONSTRAINT destinations_verification_confidence CHECK (
        verification_confidence IS NULL OR
        verification_confidence BETWEEN 0.0000 AND 1.0000
    ),
    ADD CONSTRAINT destinations_content_hash_format CHECK (
        content_hash IS NULL OR content_hash ~ '^[0-9a-f]{64}$'
    );

UPDATE destinations
SET data_origin = 'DEVELOPMENT_SEED',
    cache_status = 'NOT_APPLICABLE',
    verification_status = 'PARTIALLY_VERIFIED',
    verification_confidence = 0.7500
WHERE LOWER(country) = 'uganda'
  AND name IN (
    'Bwindi Impenetrable National Park',
    'Murchison Falls National Park',
    'Queen Elizabeth National Park',
    'Kidepo Valley National Park',
    'Mgahinga Gorilla National Park',
    'Mount Elgon National Park',
    'Rwenzori Mountains National Park',
    'Lake Mburo National Park',
    'Kibale National Park',
    'Semuliki National Park',
    'Jinja',
    'Sipi Falls',
    'Lake Bunyonyi',
    'Ssese Islands',
    'Kasubi Tombs',
    'Uganda Museum',
    'Nyero Rock Paintings',
    'Source of the Nile',
    'Ziwa Rhino Sanctuary',
    'Entebbe Botanical Gardens',
    'Kampala',
    'Fort Portal',
    'Kabale',
    'Gaddafi National Mosque',
    'Namugongo Martyrs Shrine',
    'Baha''i Temple',
    'Itanda Falls',
    'Pian Upe Wildlife Reserve',
    'Mabamba Bay',
    'Amabere Caves',
    'Tororo Rock',
    'Lake Mutanda',
    'Sezibwa Falls',
    'Bigodi Wetland Sanctuary',
    'Karamoja Cultural Region',
    'Ndere Cultural Centre'
  );

CREATE INDEX idx_destinations_data_origin ON destinations(data_origin);
CREATE INDEX idx_destinations_cache_status ON destinations(cache_status);
CREATE INDEX idx_destinations_expires_at ON destinations(expires_at)
    WHERE expires_at IS NOT NULL;
CREATE INDEX idx_destinations_verification_status ON destinations(verification_status);

ALTER TABLE destination_source_references
    ADD COLUMN provider_content_updated_at TIMESTAMPTZ,
    ADD COLUMN attribution VARCHAR(500),
    ADD COLUMN licence VARCHAR(150),
    ADD COLUMN provider_place_id VARCHAR(255),
    ADD COLUMN metadata_hash VARCHAR(64),
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT destination_source_references_metadata_hash_format CHECK (
        metadata_hash IS NULL OR metadata_hash ~ '^[0-9a-f]{64}$'
    );

CREATE INDEX idx_destination_source_references_active
    ON destination_source_references(destination_id, active);

CREATE TABLE destination_field_provenance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    destination_id UUID NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,
    field_name VARCHAR(50) NOT NULL,
    source_provider VARCHAR(40) NOT NULL,
    provider_reference_id UUID REFERENCES destination_source_references(id) ON DELETE SET NULL,
    retrieved_at TIMESTAMPTZ NOT NULL,
    last_verified_at TIMESTAMPTZ,
    confidence DECIMAL(5, 4),
    editorially_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT destination_field_provenance_field CHECK (
        field_name IN (
            'name', 'coordinates', 'countryCode', 'city',
            'officialWebsite', 'category', 'description', 'image'
        )
    ),
    CONSTRAINT destination_field_provenance_confidence CHECK (
        confidence IS NULL OR confidence BETWEEN 0.0000 AND 1.0000
    ),
    CONSTRAINT destination_field_provenance_unique UNIQUE(destination_id, field_name)
);

CREATE INDEX idx_destination_field_provenance_destination
    ON destination_field_provenance(destination_id);
