CREATE TABLE menu_categories (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  restaurant_id BIGINT REFERENCES restaurants(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE menu_items (
  id BIGSERIAL PRIMARY KEY,
  category_id BIGINT REFERENCES menu_categories(id),
  name VARCHAR(200) NOT NULL,
  description TEXT,
  price DECIMAL(10,2) NOT NULL,
  image_url VARCHAR(500),
  is_available BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE modifiers (
  id BIGSERIAL PRIMARY KEY,
  item_id BIGINT REFERENCES menu_items(id),
  name VARCHAR(100) NOT NULL,
  price DECIMAL(6,2) DEFAULT 0,
  required BOOLEAN DEFAULT false
);
