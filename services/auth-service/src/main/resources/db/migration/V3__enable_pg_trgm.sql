-- ============================================================
-- Миграция для включения полнотекстового поиска
-- ============================================================
-- Внимание: Эта миграция опциональна и выполняется только в production
-- Для тестов используется упрощенная схема без pg_trgm

-- Включаем расширение pg_trgm для триграммного поиска
-- Это расширение позволяет использовать операторы % (схожесть) и LIKE с индексами
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Проверяем, что расширение успешно установлено
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm'
    ) THEN
        RAISE EXCEPTION 'pg_trgm extension was not installed successfully';
    END IF;
END $$;

-- Создаем GIN индексы для полнотекстового поиска по имени и фамилии
-- GIN (Generalized Inverted Index) оптимизирован для поиска по триграммам
-- Используем CONCURRENTLY чтобы не блокировать таблицу

-- Индекс для поиска по имени
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_first_name_trgm
    ON users USING GIN (first_name gin_trgm_ops);

-- Индекс для поиска по фамилии
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_last_name_trgm
    ON users USING GIN (last_name gin_trgm_ops);

-- Комбинированный индекс для поиска по полному имени (имя + пробел + фамилия)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_full_name_trgm
    ON users USING GIN ((first_name || ' ' || last_name) gin_trgm_ops);

-- Индекс для поиска по email (частичное совпадение)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email_trgm
    ON users USING GIN (email gin_trgm_ops);

-- ============================================================
-- Примеры использования триграммного поиска:
-- ============================================================
-- Поиск похожих имен:
-- SELECT * FROM users WHERE first_name % 'Jhon';
--
-- Поиск по части имени (с использованием индекса):
-- SELECT * FROM users WHERE first_name ILIKE '%john%';
--
-- Поиск по полному имени:
-- SELECT * FROM users WHERE (first_name || ' ' || last_name) ILIKE '%john doe%';
--
-- Поиск по схожести (с порогом):
-- SELECT * FROM users WHERE similarity(first_name, 'jonh') > 0.3;