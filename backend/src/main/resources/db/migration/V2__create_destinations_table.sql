CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE destinations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(150) NOT NULL,
    country VARCHAR(100) NOT NULL,
    city VARCHAR(100),

    description TEXT NOT NULL,
    category VARCHAR(80) NOT NULL,

    latitude DECIMAL(9, 6),
    longitude DECIMAL(9, 6),

    cover_image_url TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT destinations_name_not_blank
        CHECK (char_length(trim(name)) > 0),

    CONSTRAINT destinations_country_not_blank
        CHECK (char_length(trim(country)) > 0),

    CONSTRAINT destinations_description_not_blank
        CHECK (char_length(trim(description)) > 0),

    CONSTRAINT destinations_category_not_blank
        CHECK (char_length(trim(category)) > 0),

    CONSTRAINT destinations_latitude_range
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),

    CONSTRAINT destinations_longitude_range
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_destinations_name
    ON destinations (name);

CREATE INDEX idx_destinations_country
    ON destinations (country);

CREATE INDEX idx_destinations_city
    ON destinations (city);

CREATE INDEX idx_destinations_category
    ON destinations (category);

CREATE INDEX idx_destinations_created_at
    ON destinations (created_at DESC);