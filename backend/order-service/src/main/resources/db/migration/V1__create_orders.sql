CREATE TYPE order_status AS ENUM (
  'CREATED', 
  'PAID', 
  'SENT_TO_RESTAURANT', 
  'ACCEPTED', 
  'PREPARING', 
  'READY', 
  'COMPLETED', 
  'CANCELED', 
  'REFUNDED'
);

CREATE TABLE orders (
  id BIGSERIAL PRIMARY KEY,
  booking_id BIGINT REFERENCES bookings(id),
  status order_status DEFAULT 'CREATED',
  items JSONB,
  total_amount DECIMAL(10,2),
  paid_amount DECIMAL(10,2),
  cancel_reason VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_booking_status ON orders(booking_id, status);
CREATE INDEX idx_orders_status_time ON orders(status, updated_at);
