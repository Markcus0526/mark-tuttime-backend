CREATE TABLE reviews
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT REFERENCES users (id),
    restaurant_id BIGINT REFERENCES restaurants (id),
    rating        DECIMAL(3, 2) CHECK (rating >= 1 AND rating <= 5),
    comment       TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reviews_restaurant_id ON reviews (restaurant_id);
CREATE INDEX idx_reviews_user_id ON reviews (user_id);
