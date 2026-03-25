-- TutTime Integration tables
CREATE TABLE integration_credentials
(
    id            BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT       NOT NULL,
    pos_system    VARCHAR(50)  NOT NULL CHECK (pos_system IN ('iiko', 'r_keeper')),
    api_url       VARCHAR(500) NOT NULL,
    api_key       VARCHAR(500),
    api_token     VARCHAR(500),
    last_sync     TIMESTAMP,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (restaurant_id, pos_system)
);

CREATE TABLE sync_log
(
    id            BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT      NOT NULL,
    type          VARCHAR(50) NOT NULL, -- MENU, ORDER, STATUS, TABLE
    pos_response  JSONB,
    status        VARCHAR(50),          -- SUCCESS, ERROR
    error_message TEXT,
    sync_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dead_letter_events
(
    id          BIGSERIAL PRIMARY KEY,
    sync_log_id BIGINT REFERENCES sync_log (id),
    event_data  JSONB,
    retry_count INT       DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for polling
CREATE INDEX idx_sync_log_restaurant_time ON sync_log (restaurant_id, sync_time DESC);
