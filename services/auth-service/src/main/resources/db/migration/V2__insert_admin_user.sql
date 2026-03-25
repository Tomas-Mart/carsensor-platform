-- ============================================================
-- Инициализация базовых данных
-- ============================================================

-- Вставка базовых разрешений
INSERT INTO permissions (name, description) VALUES
    ('VIEW_CARS', 'Просмотр автомобилей'),
    ('CREATE_CARS', 'Создание автомобилей'),
    ('EDIT_CARS', 'Редактирование автомобилей'),
    ('DELETE_CARS', 'Удаление автомобилей'),
    ('VIEW_USERS', 'Просмотр пользователей'),
    ('MANAGE_USERS', 'Управление пользователями')
ON CONFLICT (name) DO NOTHING;

-- Вставка базовых ролей
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Обычный пользователь'),
    ('ROLE_ADMIN', 'Администратор')
ON CONFLICT (name) DO NOTHING;

-- Назначение разрешений роли ROLE_USER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_USER' AND p.name IN ('VIEW_CARS')
ON CONFLICT DO NOTHING;

-- Назначение разрешений роли ROLE_ADMIN (все разрешения)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- Создание тестового пользователя admin:admin123
-- Пароль: admin123 (BCrypt hash)
INSERT INTO users (
    username,
    email,
    password,
    first_name,
    last_name,
    is_active
) VALUES (
    'admin',
    'admin@carsensor.local',
    '$2a$12$39aOe7AKc.qVz8zmHvfv9elb0n/h/AOuis6lvfuuMW1Fi6csFsAX.', -- admin123
    'Admin',
    'User',
    true
) ON CONFLICT (username) DO NOTHING;

-- Назначение роли ADMIN пользователю admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;