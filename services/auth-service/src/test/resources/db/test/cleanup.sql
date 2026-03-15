-- Очистка таблиц перед тестами
TRUNCATE TABLE users CASCADE;
TRUNCATE TABLE user_roles CASCADE;
TRUNCATE TABLE roles CASCADE;
TRUNCATE TABLE permissions CASCADE;
TRUNCATE TABLE role_permissions CASCADE;