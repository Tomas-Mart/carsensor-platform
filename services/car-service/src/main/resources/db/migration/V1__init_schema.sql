-- Создание таблицы cars
CREATE TABLE IF NOT EXISTS cars (
    id BIGSERIAL PRIMARY KEY,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INTEGER NOT NULL,
    mileage INTEGER NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    description TEXT,
    original_brand VARCHAR(100),
    original_model VARCHAR(100),
    exterior_color VARCHAR(50),
    interior_color VARCHAR(50),
    engine_capacity VARCHAR(20),
    transmission VARCHAR(30),
    drive_type VARCHAR(30),
    photo_urls JSONB,
    main_photo_url VARCHAR(500),
    parsed_at TIMESTAMP,
    source_url VARCHAR(500),
    external_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Индексы для оптимизации запросов
CREATE INDEX idx_cars_brand_model ON cars(brand, model);
CREATE INDEX idx_cars_year ON cars(year);
CREATE INDEX idx_cars_price ON cars(price);
CREATE INDEX idx_cars_mileage ON cars(mileage);
CREATE INDEX idx_cars_parsed_at ON cars(parsed_at);
CREATE INDEX idx_cars_external_id ON cars(external_id);
CREATE INDEX idx_cars_transmission ON cars(transmission);
CREATE INDEX idx_cars_drive_type ON cars(drive_type);

-- Составные индексы для частых комбинаций фильтров
CREATE INDEX idx_cars_brand_year ON cars(brand, year);
CREATE INDEX idx_cars_model_year ON cars(model, year);

-- Полнотекстовый поиск
CREATE INDEX idx_cars_search ON cars USING GIN (
    to_tsvector('russian', coalesce(brand, '') || ' ' || coalesce(model, '') || ' ' || coalesce(description, ''))
);