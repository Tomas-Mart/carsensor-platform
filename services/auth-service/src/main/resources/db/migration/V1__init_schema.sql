-- Создание таблицы permissions
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200)
);

-- Создание таблицы roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200)
);

-- Создание таблицы role_permissions
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Создание таблицы users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Создание таблицы user_roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Индексы для оптимизации запросов
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(is_active);

-- Инициализация базовых ролей и разрешений
INSERT INTO permissions (name, description) VALUES
    ('CAR_VIEW', 'Просмотр автомобилей'),
    ('CAR_CREATE', 'Создание автомобилей'),
    ('CAR_EDIT', 'Редактирование автомобилей'),
    ('CAR_DELETE', 'Удаление автомобилей'),
    ('USER_VIEW', 'Просмотр пользователей'),
    ('USER_MANAGE', 'Управление пользователями')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Обычный пользователь'),
    ('ROLE_ADMIN', 'Администратор')
ON CONFLICT (name) DO NOTHING;

-- Назначение разрешений ролям
-- USER: только просмотр
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_USER' AND p.name IN ('CAR_VIEW')
ON CONFLICT DO NOTHING;

-- ADMIN: все разрешения
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- Создание тестового пользователя admin:admin123
INSERT INTO users (username, email, password, first_name, last_name, is_active)
VALUES (
    'admin',
    'admin@carsensor.local',
    '$2a$10$rTqUJvKzLQpLQpLQpLQpLQpLQpLQpLQpLQpLQpLQpLQpLQpLQpLQ', -- admin123
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