CREATE TABLE events (
  id BIGSERIAL PRIMARY KEY,
  type VARCHAR(100) NOT NULL,
  restaurant_id BIGINT,
  booking_id BIGINT,
  order_id BIGINT,
  data JSONB,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_type_time ON events(type, timestamp);
CREATE INDEX idx_events_restaurant ON events(restaurant_id);
