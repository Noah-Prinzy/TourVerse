CREATE TABLE trips (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT trips_title_not_blank CHECK (char_length(trim(title)) > 0),
    CONSTRAINT trips_date_order CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX idx_trips_user_id ON trips(user_id);

CREATE TABLE trip_destinations (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    destination_id UUID NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,
    visit_date DATE,
    notes TEXT,
    display_order INTEGER NOT NULL DEFAULT 0 CHECK (display_order >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT trip_destinations_unique UNIQUE (trip_id, destination_id)
);
CREATE INDEX idx_trip_destinations_trip_id ON trip_destinations(trip_id);
