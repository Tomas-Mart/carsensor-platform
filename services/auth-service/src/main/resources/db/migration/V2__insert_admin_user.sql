-- Создание тестового пользователя admin:admin123
-- Пароль: admin123 (BCrypt hash)
INSERT INTO users (username, email, password, first_name, last_name, is_active)
VALUES (
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