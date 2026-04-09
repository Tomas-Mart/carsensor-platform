-- ============================================================
-- Инициализация базовых данных
-- ============================================================

-- Вставка базовых разрешений
INSERT INTO permissions (name, description, created_at, updated_at, version) VALUES
    ('VIEW_CARS', 'Просмотр автомобилей', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('CREATE_CARS', 'Создание автомобилей', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('EDIT_CARS', 'Редактирование автомобилей', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('DELETE_CARS', 'Удаление автомобилей', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('VIEW_USERS', 'Просмотр пользователей', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('MANAGE_USERS', 'Управление пользователями', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (name) DO NOTHING;

-- Вставка базовых ролей
INSERT INTO roles (name, description, created_at, updated_at, version) VALUES
    ('ROLE_USER', 'Обычный пользователь', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('ROLE_ADMIN', 'Администратор', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (name) DO NOTHING;

-- Назначение разрешений роли ROLE_USER
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r, permissions p
WHERE r.name = 'ROLE_USER' AND p.name IN ('VIEW_CARS')
ON CONFLICT DO NOTHING;

-- Назначение разрешений роли ROLE_ADMIN (все разрешения)
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
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
    is_active,
    is_locked,
    failed_attempts,
    lock_time,
    created_at,
    updated_at,
    version
) VALUES (
    'admin',
    'admin@carsensor.local',
    '$2a$12$39aOe7AKc.qVz8zmHvfv9elb0n/h/AOuis6lvfuuMW1Fi6csFsAX.', -- admin123
    'Admin',
    'User',
    true,           -- is_active
    false,          -- is_locked
    0,              -- failed_attempts
    NULL,           -- lock_time
    CURRENT_TIMESTAMP, -- created_at
    CURRENT_TIMESTAMP, -- updated_at
    0               -- version
) ON CONFLICT (username) DO NOTHING;

-- Назначение роли ADMIN пользователю admin
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;