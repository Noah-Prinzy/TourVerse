CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    destination_id UUID NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT reviews_one_per_user_destination UNIQUE (user_id, destination_id)
);
CREATE INDEX idx_reviews_destination_id ON reviews(destination_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
