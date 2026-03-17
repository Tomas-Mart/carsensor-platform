-- Очистка данных перед вставкой (если нужно)
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username = 'admin');
DELETE FROM users WHERE username = 'admin';

-- Вставка тестового пользователя (пароль: admin123)
-- Без указания id - пусть генерируется автоматически
INSERT INTO users (username, email, password, first_name, last_name, is_active, created_at, updated_at)
VALUES (
    'admin',
    'admin@test.com',
    '$2a$12$39aOe7AKc.qVz8zmHvfv9elb0n/h/AOuis6lvfuuMW1Fi6csFsAX.',
    'Admin',
    'User',
    true,
    NOW(),
    NOW()
);

-- Назначение ролей - используем SELECT для получения ID пользователя
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;