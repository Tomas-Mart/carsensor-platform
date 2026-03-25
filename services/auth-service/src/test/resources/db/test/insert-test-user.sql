-- ============================================================
-- Вставка тестовых данных для PostgreSQL
-- ============================================================

-- Вставка разрешений (без created_at/updated_at, они заполнятся DEFAULT)
INSERT INTO permissions (name, description) VALUES
    ('VIEW_CARS', 'Просмотр автомобилей'),
    ('EDIT_CARS', 'Редактирование автомобилей'),
    ('DELETE_CARS', 'Удаление автомобилей'),
    ('CREATE_CARS', 'Создание автомобилей')
ON CONFLICT (name) DO NOTHING;

-- Вставка ролей (без created_at/updated_at, они заполнятся DEFAULT)
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Обычный пользователь'),
    ('ROLE_ADMIN', 'Администратор')
ON CONFLICT (name) DO NOTHING;

-- Назначение разрешений роли ROLE_USER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_USER' AND p.name = 'VIEW_CARS'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Назначение разрешений роли ROLE_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN' AND p.name IN ('VIEW_CARS', 'EDIT_CARS', 'DELETE_CARS', 'CREATE_CARS')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Вставка пользователя admin с паролем admin123 (BCrypt hash)
INSERT INTO users (
    username,
    email,
    password,
    first_name,
    last_name,
    is_active,
    created_at,
    updated_at,
    version
) VALUES (
    'admin',
    'admin@test.com',
    '$2a$12$39aOe7AKc.qVz8zmHvfv9elb0n/h/AOuis6lvfuuMW1Fi6csFsAX.',
    'Admin',
    'User',
    true,
    NOW(),
    NOW(),
    0
) ON CONFLICT (username) DO UPDATE SET
    password = EXCLUDED.password,
    is_active = true,
    updated_at = NOW();

-- Назначение роли ADMIN пользователю admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;