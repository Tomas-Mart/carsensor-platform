-- Таблица для хранения информации о парсинге
CREATE TABLE IF NOT EXISTS parsed_data (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    items_count INTEGER,
    error_message TEXT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);