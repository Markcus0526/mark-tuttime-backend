CREATE TABLE restaurants (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  address TEXT,
  rating DECIMAL(3,2) DEFAULT 0.0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE branches (
  id BIGSERIAL PRIMARY KEY,
  restaurant_id BIGINT REFERENCES restaurants(id),
  name VARCHAR(255),
  address TEXT
);

CREATE TABLE zones (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT REFERENCES branches(id),
  name VARCHAR(50),
  capacity INT
);

CREATE TABLE tables (
  id BIGSERIAL PRIMARY KEY,
  zone_id BIGINT REFERENCES zones(id),
  number INT,
  capacity INT,
  status VARCHAR(20) DEFAULT 'AVAILABLE'
);

CREATE INDEX idx_restaurants_name ON restaurants(name);
CREATE INDEX idx_tables_zone_id ON tables(zone_id);
