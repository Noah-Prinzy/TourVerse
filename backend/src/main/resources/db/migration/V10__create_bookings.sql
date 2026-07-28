CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES tourism_services(id) ON DELETE RESTRICT,
    booking_date DATE NOT NULL,
    number_of_people INTEGER NOT NULL,
    total_price NUMERIC(12,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT bookings_people_positive CHECK (number_of_people > 0),
    CONSTRAINT bookings_total_nonnegative CHECK (total_price IS NULL OR total_price >= 0)
);
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_service ON bookings(service_id);
