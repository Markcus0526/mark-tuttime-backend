CREATE TABLE bookings (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  restaurant_id BIGINT REFERENCES restaurants(id),
  branch_id BIGINT REFERENCES branches(id),
  table_id BIGINT REFERENCES tables(id),
  status VARCHAR(20) DEFAULT 'PENDING',
  booking_time TIMESTAMP NOT NULL,
  num_guests INT,
  prep_time INT DEFAULT 0,
  total_discount DECIMAL(5,2) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
  id BIGSERIAL PRIMARY KEY,
  booking_id BIGINT REFERENCES bookings(id),
  status VARCHAR(20) DEFAULT 'CREATED',
  total_amount DECIMAL(10,2),
  paid_amount DECIMAL(10,2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
