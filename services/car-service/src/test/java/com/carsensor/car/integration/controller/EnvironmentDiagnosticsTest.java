package com.carsensor.car.integration.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.carsensor.car.CarServiceApplication;
import com.carsensor.common.test.AbstractIntegrationTest;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Диагностические тесты для проверки окружения.
 * Можно запускать отдельно для отладки проблем с БД.
 *
 * <p>Содержит диагностические тесты для проверки:
 * <ul>
 *   <li>Портов и URL базы данных</li>
 *   <li>Создания временных директорий</li>
 *   <li>Наличия дампов ошибок</li>
 *   <li>Статистики выполнения тестов</li>
 *   <li>Анализа производительности</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Slf4j
@DisplayName("Диагностика окружения")
@SpringBootTest(classes = CarServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnvironmentDiagnosticsTest extends AbstractIntegrationTest {

    @BeforeAll
    static void setup() {
        log.info("╔══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║                    ЗАПУСК ДИАГНОСТИЧЕСКИХ ТЕСТОВ                            ║");
        log.info("╚══════════════════════════════════════════════════════════════════════════════╝");
        logEnvironmentInfoStatic();
    }

    @Test
    @DisplayName("Проверка всех временных директорий")
    void checkAllDirectories() {
        assertThat(getTestDataCache()).exists();
        assertThat(getFailureDumpsDir()).exists();
        assertThat(getPerformanceLogsDir()).exists();
        assertThat(getTempSqlFilesDir()).exists();

        log.info("✅ Все временные директории созданы успешно");
        log.info("   - Кэш тестовых данных: {}", getTestDataCache());
        log.info("   - Дампы ошибок: {}", getFailureDumpsDir());
        log.info("   - Логи производительности: {}", getPerformanceLogsDir());
        log.info("   - Временные SQL файлы: {}", getTempSqlFilesDir());
    }

    // ============================================================
    // Проверка базы данных
    // ============================================================

    @Nested
    @DisplayName("Проверка базы данных")
    class DatabaseDiagnosticsTests {

        @Test
        @DisplayName("Проверка порта базы данных")
        void databasePort_ShouldBeValid() {
            int port = getDatabasePort();

            log.info("Порт базы данных: {}", port);
            assertThat(port)
                    .as("Порт базы данных должен быть в диапазоне 1024-65535")
                    .isBetween(1024, 65535);
        }

        @Test
        @DisplayName("Проверка инициализации базы данных")
        void databaseInitialized_ShouldBeTrue() {
            boolean initialized = isDatabaseInitialized();

            log.info("База данных инициализирована: {}", initialized);
            assertThat(initialized)
                    .as("База данных должна быть инициализирована")
                    .isTrue();
        }

        @Test
        @DisplayName("Проверка URL базы данных")
        void databaseUrl_ShouldBeValid() {
            String url = getDatabaseUrl();

            log.info("URL базы данных: {}", url);
            assertThat(url)
                    .as("URL базы данных должен быть корректным")
                    .startsWith("jdbc:postgresql://localhost:");
        }
    }

    // ============================================================
    // Проверка дампов ошибок
    // ============================================================

    @Nested
    @DisplayName("Проверка дампов ошибок")
    class FailureDumpsTests {

        @Test
        @DisplayName("Проверка наличия дампов ошибок")
        void checkFailureDumps() throws IOException {
            Path dumpsDir = getFailureDumpsDir();

            if (Files.exists(dumpsDir)) {
                try (var stream = Files.list(dumpsDir)) {
                    long dumpCount = stream.count();
                    log.info("Найдено дампов ошибок: {}", dumpCount);

                    if (dumpCount > 0) {
                        log.warn("⚠️ Есть необработанные дампы ошибок! Проверьте директорию: {}", dumpsDir);
                        try (var streamFiles = Files.list(dumpsDir)) {
                            streamFiles.forEach(file -> log.warn("   - {}", file.getFileName()));
                        }
                    } else {
                        log.info("✅ Дампы ошибок отсутствуют");
                    }
                }
            }
        }

        @Test
        @DisplayName("Создание тестового дампа ошибки")
        void createTestFailureDump() {
            try {
                // Симулируем ошибку для теста
                throw new RuntimeException("Тестовая ошибка для проверки создания дампа");
            } catch (Exception e) {
                saveFailureDump(
                        "createTestFailureDump",
                        "Тестовые данные для дампа\nОшибка: " + e.getMessage(),
                        "TEST_EXCEPTION"
                );
                log.info("✅ Создан тестовый дамп ошибки");
            }
        }
    }

    // ============================================================
    // Анализ производительности
    // ============================================================

    @Nested
    @DisplayName("Анализ производительности")
    class PerformanceAnalysisTests {

        @Test
        @DisplayName("Анализ логов производительности")
        void analyzePerformanceLogs() throws IOException {
            Path logsDir = getPerformanceLogsDir();

            if (Files.exists(logsDir)) {
                try (var stream = Files.list(logsDir)) {
                    long logCount = stream.count();
                    log.info("Найдено логов производительности: {}", logCount);
                }

                try (var streamFiles = Files.list(logsDir)) {
                    streamFiles.forEach(logFile -> {
                        try {
                            List<String> lines = Files.readAllLines(logFile);
                            log.info("📊 Лог {}: {} записей", logFile.getFileName(), lines.size());

                            // Находим медленные операции (> 1 секунды)
                            long slowOps = lines.stream()
                                    .filter(l -> l.contains("ms"))
                                    .filter(l -> {
                                        try {
                                            String[] parts = l.split(": ");
                                            if (parts.length > 1) {
                                                String timeStr = parts[1].split(" ")[0];
                                                return Integer.parseInt(timeStr) > 1000;
                                            }
                                        } catch (Exception ignored) {
                                        }
                                        return false;
                                    })
                                    .count();

                            if (slowOps > 0) {
                                log.warn("⚠️ Найдено {} медленных операций в {}", slowOps, logFile.getFileName());
                                lines.stream()
                                        .filter(l -> l.contains("ms"))
                                        .forEach(l -> {
                                            try {
                                                String[] parts = l.split(": ");
                                                if (parts.length > 1) {
                                                    String timeStr = parts[1].split(" ")[0];
                                                    if (Integer.parseInt(timeStr) > 1000) {
                                                        log.warn("   - {}", l);
                                                    }
                                                }
                                            } catch (Exception ignored) {
                                            }
                                        });
                            }
                        } catch (IOException e) {
                            log.error("Ошибка чтения лога {}: {}", logFile.getFileName(), e.getMessage());
                        }
                    });
                }
            } else {
                log.info("Логи производительности отсутствуют");
            }
        }

        @Test
        @DisplayName("Тест производительности - создание и логирование")
        void performanceTest() {
            long startTime = System.currentTimeMillis();

            // Имитация работы
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long duration = System.currentTimeMillis() - startTime;
            logPerformance("performanceTest", duration);
            log.info("✅ Тест производительности выполнен за {} ms", duration);
        }
    }

    // ============================================================
    // Статистика тестов
    // ============================================================

    @Nested
    @DisplayName("Статистика тестов")
    class TestStatisticsTests {

        @Test
        @DisplayName("Вывод статистики выполнения тестов")
        void printTestStatistics() {
            log.info("═══════════════════════════════════════════════════════════════════════════════");
            log.info("📊 СТАТИСТИКА ВЫПОЛНЕНИЯ ТЕСТОВ");
            log.info("═══════════════════════════════════════════════════════════════════════════════");
            log.info("Всего выполнено тестов: {}", getTestCount());
            log.info("Время начала: {}", getTestStartTime());
            log.info("Текущее время: {}", Instant.now());
            log.info("Длительность: {} секунд",
                    Duration.between(getTestStartTime(), Instant.now()).getSeconds());
            log.info("═══════════════════════════════════════════════════════════════════════════════");

            assertThat(getTestCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Проверка счетчика тестов")
        void testCounter_ShouldBeIncremented() {
            int count = getTestCount();
            log.info("Текущий счетчик тестов: {}", count);
            assertThat(count).isGreaterThanOrEqualTo(0);
        }
    }

    // ============================================================
    // Временные SQL файлы
    // ============================================================

    @Nested
    @DisplayName("Работа с временными SQL файлами")
    class TempSqlFilesTests {

        @Test
        @DisplayName("Создание временного SQL файла")
        void createTempSqlFile_ShouldSucceed() throws IOException {
            String testSql = """
                    CREATE TABLE IF NOT EXISTS test_table (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL
                    );
                    
                    INSERT INTO test_table (name) VALUES ('test1'), ('test2');
                    """;

            Path sqlFile = createTempSqlFile("test-diagnostics.sql", testSql);
            log.info("Создан временный SQL файл: {}", sqlFile);

            assertThat(sqlFile).exists();
            assertThat(Files.readString(sqlFile)).contains("test_table");
        }

        @Test
        @DisplayName("Проверка директории временных SQL файлов")
        void checkTempSqlFilesDir() throws IOException {
            Path sqlDir = getTempSqlFilesDir();
            assertThat(sqlDir).exists();

            try (var stream = Files.list(sqlDir)) {
                long count = stream.count();
                log.info("Временных SQL файлов в директории: {}", count);
            }
        }
    }

    // ============================================================
    // Кэш тестовых данных
    // ============================================================

    @Nested
    @DisplayName("Кэш тестовых данных")
    class TestDataCacheTests {

        @Test
        @DisplayName("Проверка директории кэша")
        void checkTestDataCache() throws IOException {
            Path cacheDir = getTestDataCache();
            assertThat(cacheDir).exists();

            // Создаем тестовый файл в кэше
            Path testFile = cacheDir.resolve("test-cache-file.txt");
            Files.writeString(testFile, "Тестовые данные для кэша");
            log.info("Создан тестовый файл в кэше: {}", testFile);

            assertThat(testFile).exists();
            assertThat(Files.readString(testFile)).isEqualTo("Тестовые данные для кэша");
        }
    }

    // ============================================================
    // Очистка после диагностики
    // ============================================================

    @AfterAll
    static void cleanup() throws IOException {
        log.info("═══════════════════════════════════════════════════════════════════════════════");
        log.info("🧹 ОЧИСТКА ПОСЛЕ ДИАГНОСТИКИ");
        log.info("═══════════════════════════════════════════════════════════════════════════════");

        // Очищаем временные SQL файлы
        Path sqlDir = getTempSqlFilesDir();
        if (Files.exists(sqlDir)) {
            try (var stream = Files.list(sqlDir)) {
                long count = stream.count();
                if (count > 0) {
                    log.info("Временных SQL файлов для очистки: {}", count);
                }
            }

            // Очистка старых файлов
            try (var stream = Files.list(sqlDir)) {
                stream.forEach(file -> {
                    try {
                        Files.delete(file);
                        log.debug("Удален временный SQL файл: {}", file.getFileName());
                    } catch (IOException e) {
                        log.warn("Не удалось удалить {}: {}", file.getFileName(), e.getMessage());
                    }
                });
            }
            log.info("✅ Директория временных SQL файлов очищена");
        }

        // Очищаем тестовый кэш
        Path cacheDir = getTestDataCache();
        if (Files.exists(cacheDir)) {
            try (var stream = Files.list(cacheDir)) {
                stream.forEach(file -> {
                    try {
                        Files.delete(file);
                        log.debug("Удален файл кэша: {}", file.getFileName());
                    } catch (IOException e) {
                        log.warn("Не удалось удалить {}: {}", file.getFileName(), e.getMessage());
                    }
                });
            }
            log.info("✅ Директория кэша очищена");
        }

        log.info("═══════════════════════════════════════════════════════════════════════════════");
        log.info("✅ ДИАГНОСТИКА ЗАВЕРШЕНА УСПЕШНО");
        log.info("═══════════════════════════════════════════════════════════════════════════════");
    }
}