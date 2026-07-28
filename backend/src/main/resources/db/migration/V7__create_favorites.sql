CREATE TABLE favorites (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    destination_id UUID NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT favorites_unique_user_destination UNIQUE (user_id, destination_id)
);
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_destination_id ON favorites(destination_id);
