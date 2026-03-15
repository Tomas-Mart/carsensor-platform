-- Очистка данных (если нужно)
DELETE FROM user_roles WHERE user_id = 1;
DELETE FROM users WHERE username = 'admin';

-- Вставка тестового пользователя (пароль: admin123)
INSERT INTO users (id, username, email, password, first_name, last_name, is_active, created_at, updated_at)
VALUES (
    1,
    'admin',
    'admin@test.com',
    '$2a$12$KHB9EeB01exKk/CA/Kvm9.fMAa4960q/36g8N0HcYynFPIt5HCBOG',
    'Admin',
    'User',
    true,
    NOW(),
    NOW()
);

-- Назначение ролей (предполагается, что роль ROLE_ADMIN уже существует)
INSERT INTO user_roles (user_id, role_id)
SELECT 1, id FROM roles WHERE name = 'ROLE_ADMIN';