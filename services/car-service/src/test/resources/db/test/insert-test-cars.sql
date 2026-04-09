-- ============================================================
-- Очистка таблицы перед вставкой
-- ============================================================
TRUNCATE TABLE cars RESTART IDENTITY CASCADE;

-- ============================================================
-- Вставка тестовых данных для автомобилей
-- ============================================================
INSERT INTO cars (
    brand, model, year, mileage, price, external_id,
    transmission, drive_type, exterior_color,
    parsed_at, created_at, updated_at, version
) VALUES
    -- Toyota
    ('Toyota', 'Camry', 2020, 50000, 2500000.00, 'unique-id-123',
     'AT', '2WD', 'White',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

    ('Toyota', 'RAV4', 2021, 45000, 2800000.00, NULL,
     'CVT', '4WD', 'Black',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

    -- Honda
    ('Honda', 'Civic', 2021, 30000, 2200000.00, NULL,
     'MT', '2WD', 'Red',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

    ('Honda', 'Accord', 2022, 25000, 2700000.00, NULL,
     'CVT', '2WD', 'Blue',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

    -- Nissan
    ('Nissan', 'X-Trail', 2021, 20000, 2700000.00, NULL,
     'CVT', '4WD', 'Silver',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

    -- Mazda
    ('Mazda', 'CX-5', 2022, 15000, 2800000.00, NULL,
     'AT', '4WD', 'Gray',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============================================================
-- Сброс последовательности ID
-- ============================================================
SELECT setval('cars_id_seq', (SELECT MAX(id) FROM cars));