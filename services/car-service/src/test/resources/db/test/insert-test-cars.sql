-- Очистка перед вставкой
TRUNCATE TABLE cars CASCADE;

-- Вставка тестовых автомобилей
INSERT INTO cars (id, brand, model, year, mileage, price, exterior_color, transmission, drive_type, parsed_at, created_at)
VALUES
    (1, 'Toyota', 'Camry', 2020, 50000, 2500000, 'White', 'AT', '2WD', NOW(), NOW()),
    (2, 'Toyota', 'RAV4', 2021, 30000, 3500000, 'Black', 'CVT', '4WD', NOW(), NOW()),
    (3, 'Honda', 'Civic', 2022, 10000, 1800000, 'Red', 'MT', '2WD', NOW(), NOW());

-- Сброс последовательности
SELECT setval('cars_id_seq', (SELECT MAX(id) FROM cars));