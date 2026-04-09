package com.carsensor.common.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.extern.slf4j.Slf4j;

/**
 * Абстрактный базовый класс для интеграционных тестов.
 *
 * <h2>Особенности реализации:</h2>
 * <ul>
 *   <li>Использует Embedded PostgreSQL для изоляции тестов</li>
 *   <li>Автоматическое управление временными директориями внутри target/</li>
 *   <li>Оптимизирован для Java 21 и JUnit 5</li>
 *   <li>Корректное управление транзакциями с autoCommit=true</li>
 *   <li>Все ресурсы закрываются через try-with-resources</li>
 * </ul>
 *
 * <h2>Принципы работы:</h2>
 * <ul>
 *   <li><b>Изоляция:</b> Каждый тестовый класс использует свой экземпляр PostgreSQL</li>
 *   <li><b>Очистка:</b> Удаляются только временные файлы внутри target/ текущего проекта</li>
 *   <li><b>Производительность:</b> Минимальные накладные расходы на инициализацию</li>
 *   <li><b>Безопасность:</b> Не затрагиваются глобальные кэши и другие проекты</li>
 * </ul>
 *
 * <h2>Использование:</h2>
 * <pre>{@code
 * @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * class MyIntegrationTest extends AbstractIntegrationTest {
 *
 *     @Test
 *     void test() {
 *         // Тест автоматически получает доступ к embedded PostgreSQL
 *     }
 * }
 * }</pre>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Slf4j
@ActiveProfiles("test")
@Timeout(value = 120, unit = TimeUnit.SECONDS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {
    // ============================================================
    // Управление временными директориями (Enterprise Grade)
    // ============================================================
    /**
     * JUnit управляет очисткой этой директории автоматически.
     * Используется для временных файлов, которые не требуют ручного удаления.
     */
    @TempDir
    static Path tempDir;

    private static Path testDataCache;
    private static Path failureDumpsDir;
    private static Path tempSqlFilesDir;
    private static Instant testStartTime;
    private static Path performanceLogsDir;
    private static final AtomicInteger testCounter = new AtomicInteger(0);
    private static final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);

    @BeforeAll
    static void setupTempDir() throws IOException {
        testStartTime = Instant.now();
        log.info("╔══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║                    ИНИЦИАЛИЗАЦИЯ ТЕСТОВОГО ОКРУЖЕНИЯ v2.0                     ║");
        log.info("╚══════════════════════════════════════════════════════════════════════════════╝");
        log.info("📁 JUnit временная директория: {}", tempDir);
        log.info("📁 PostgreSQL директория данных: {}", projectTempDir);

        // Директория для кэша тестовых данных
        testDataCache = tempDir.resolve("test-data-cache");
        Files.createDirectories(testDataCache);
        log.info("✅ Кэш тестовых данных: {}", testDataCache);

        // Директория для дампов при ошибках
        failureDumpsDir = tempDir.resolve("failure-dumps");
        Files.createDirectories(failureDumpsDir);
        log.info("✅ Директория дампов ошибок: {}", failureDumpsDir);

        // Директория для логов производительности
        performanceLogsDir = tempDir.resolve("performance-logs");
        Files.createDirectories(performanceLogsDir);
        log.info("✅ Логи производительности: {}", performanceLogsDir);

        // Директория для временных SQL файлов
        tempSqlFilesDir = tempDir.resolve("temp-sql");
        Files.createDirectories(tempSqlFilesDir);
        log.info("✅ Временные SQL файлы: {}", tempSqlFilesDir);

        // Сохранить метаинформацию о тестовом запуске
        saveTestRunMetadata();

        // Зарегистрировать JVM shutdown hook для аварийного сохранения
        registerShutdownHook();

        log.info("═══════════════════════════════════════════════════════════════════════════════");
        log.info("✅ ИНИЦИАЛИЗАЦИЯ ТЕСТОВОГО ОКРУЖЕНИЯ ЗАВЕРШЕНА УСПЕШНО");
        log.info("═══════════════════════════════════════════════════════════════════════════════");
    }

    /**
     * Сохраняет метаинформацию о тестовом запуске в JSON формате.
     */
    private static void saveTestRunMetadata() throws IOException {
        Path metadata = tempDir.resolve("test-run-metadata.json");
        String json = """
                {
                  "testRunId": "%s",
                  "startTime": "%s",
                  "javaVersion": "%s",
                  "javaVendor": "%s",
                  "os": "%s",
                  "osArch": "%s",
                  "userDir": "%s",
                  "tempDir": "%s",
                  "projectTempDir": "%s",
                  "testDataCache": "%s",
                  "failureDumpsDir": "%s",
                  "performanceLogsDir": "%s",
                  "tempSqlFilesDir": "%s"
                }
                """.formatted(
                UUID.randomUUID(),
                testStartTime,
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("os.name") + " " + System.getProperty("os.version"),
                System.getProperty("os.arch"),
                System.getProperty("user.dir"),
                tempDir,
                projectTempDir,
                testDataCache,
                failureDumpsDir,
                performanceLogsDir,
                tempSqlFilesDir
        );
        Files.writeString(metadata, json, StandardCharsets.UTF_8);
        log.debug("📄 Метаданные тестового запуска сохранены: {}", metadata);
    }

    /**
     * Регистрирует shutdown hook для сохранения статистики при аварийном завершении.
     */
    private static void registerShutdownHook() {
        if (shutdownHookRegistered.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (testCounter.get() > 0) {
                    log.info("📊 Статистика тестов: выполнено {} тестов", testCounter.get());
                }
                try {
                    saveTestRunSummary();
                } catch (Exception e) {
                    log.warn("Не удалось сохранить итоговую статистику: {}", e.getMessage());
                }
            }));
        }
    }

    /**
     * Сохраняет итоговую статистику тестового запуска.
     */
    private static void saveTestRunSummary() throws IOException {
        Path summary = tempDir.resolve("test-run-summary.json");
        String json = """
                {
                  "testRunId": "%s",
                  "endTime": "%s",
                  "durationSeconds": %d,
                  "totalTests": %d
                }
                """.formatted(
                getTestRunId(),
                Instant.now(),
                Duration.between(testStartTime, Instant.now()).getSeconds(),
                testCounter.get()
        );
        Files.writeString(summary, json, StandardCharsets.UTF_8);
        log.debug("📊 Итоговая статистика сохранена: {}", summary);
    }

    /**
     * Возвращает ID текущего тестового запуска.
     */
    private static String getTestRunId() throws IOException {
        Path metadata = tempDir.resolve("test-run-metadata.json");
        if (Files.exists(metadata)) {
            String content = Files.readString(metadata);
            return content.split("\"testRunId\": \"")[1].split("\"")[0];
        }
        return "unknown";
    }

    @BeforeEach
    void trackTestStart(TestInfo testInfo) {
        testCounter.incrementAndGet();
        log.debug("▶️  Запуск теста: {}", testInfo.getDisplayName());
    }

    @AfterEach
    void trackTestFinish(TestInfo testInfo) {
        log.debug("✅ Завершен тест: {}", testInfo.getDisplayName());
    }

    /**
     * Сохраняет дамп данных при ошибке теста.
     *
     * @param testName имя теста
     * @param data     данные для сохранения
     * @param reason   причина ошибки
     */
    protected void saveFailureDump(String testName, String data, String reason) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String safeReason = reason.replaceAll("[^a-zA-Z0-9а-яА-Я_\\-]", "_");
            Path dumpFile = failureDumpsDir.resolve(String.format("%s_%s_%s.dump",
                    testName, timestamp, safeReason));
            Files.writeString(dumpFile, data, StandardCharsets.UTF_8);
            log.warn("💾 Сохранен дамп ошибки: {}", dumpFile);
        } catch (IOException e) {
            log.error("Не удалось сохранить дамп ошибки: {}", e.getMessage());
        }
    }

    /**
     * Логирует время выполнения операции для анализа производительности.
     *
     * @param operation  название операции
     * @param durationMs длительность в миллисекундах
     */
    protected void logPerformance(String operation, long durationMs) {
        try {
            String date = LocalDate.now().toString();
            Path logFile = performanceLogsDir.resolve(String.format("performance_%s.log", date));
            String entry = String.format("[%s] %s: %d ms%n",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    operation, durationMs);
            Files.writeString(logFile, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (durationMs > 5000) {
                log.warn("⚠️ Медленная операция: {} - {} ms", operation, durationMs);
            }
        } catch (IOException e) {
            log.warn("Не удалось записать лог производительности: {}", e.getMessage());
        }
    }

    /**
     * Создает временный SQL файл для тестов.
     *
     * @param fileName   имя файла
     * @param sqlContent содержимое SQL
     * @return путь к созданному файлу
     */
    protected Path createTempSqlFile(String fileName, String sqlContent) throws IOException {
        Path sqlFile = tempSqlFilesDir.resolve(fileName);
        Files.writeString(sqlFile, sqlContent, StandardCharsets.UTF_8);
        log.debug("📄 Создан временный SQL файл: {}", sqlFile);
        return sqlFile;
    }

    /**
     * Получает директорию для кэша тестовых данных.
     */
    protected static Path getTestDataCache() {
        return testDataCache;
    }

    /**
     * Получает директорию для дампов ошибок.
     */
    protected static Path getFailureDumpsDir() {
        return failureDumpsDir;
    }

    /**
     * Получает директорию для логов производительности.
     */
    protected static Path getPerformanceLogsDir() {
        return performanceLogsDir;
    }

    /**
     * Получает директорию для временных SQL файлов.
     */
    protected static Path getTempSqlFilesDir() {
        return tempSqlFilesDir;
    }

    /**
     * Получает количество выполненных тестов.
     */
    protected static int getTestCount() {
        return testCounter.get();
    }

    /**
     * Получает время начала тестового запуска.
     */
    protected static Instant getTestStartTime() {
        return testStartTime;
    }

    /**
     * Логирует информацию о тестовом окружении.
     */
    protected void logEnvironmentInfo() {
        log.info("🔧 Информация о тестовом окружении:");
        log.info("   - Java версия: {}", System.getProperty("java.version"));
        log.info("   - ОС: {}", System.getProperty("os.name"));
        log.info("   - Рабочая директория: {}", System.getProperty("user.dir"));
        log.info("   - Временная директория JUnit: {}", tempDir);
        log.info("   - Директория PostgreSQL: {}", projectTempDir);
        log.info("   - Кэш тестовых данных: {}", testDataCache);
    }

    /**
     * Логирует информацию о тестовом окружении (статический метод для @BeforeAll).
     */
    protected static void logEnvironmentInfoStatic() {
        log.info("🔧 ИНФОРМАЦИЯ О ТЕСТОВОМ ОКРУЖЕНИИ");
        log.info("═══════════════════════════════════════════════════════════════════════════════");
        log.info("   - Java версия: {}", System.getProperty("java.version"));
        log.info("   - Java вендор: {}", System.getProperty("java.vendor"));
        log.info("   - ОС: {}", System.getProperty("os.name"));
        log.info("   - Архитектура: {}", System.getProperty("os.arch"));
        log.info("   - Рабочая директория: {}", System.getProperty("user.dir"));
        log.info("   - Временная директория JUnit: {}", getTestDataCache().getParent());
        log.info("   - Директория PostgreSQL данных: {}", getDatabaseUrl());
        log.info("   - Порт базы данных: {}", getDatabasePort());
        log.info("   - База данных инициализирована: {}", isDatabaseInitialized());
        log.info("═══════════════════════════════════════════════════════════════════════════════");
        log.info("   - URL базы данных: {}", getDatabaseUrl());

    }

    @LocalServerPort
    private int port;

    private static volatile EmbeddedPostgres embeddedPostgres;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final ReentrantLock lock = new ReentrantLock();
    private static volatile String databaseUrl;
    private static volatile int databasePort;
    private static volatile Path projectTempDir;

    private static final int CONNECTION_VALIDITY_TIMEOUT_SECONDS = 5;
    private static final int WAIT_FOR_DATABASE_RETRY_MS = 500;
    private static final int WAIT_FOR_DATABASE_TIMEOUT_SECONDS = 30;

    static {
        initializeProjectTempDirectory();

        try {
            log.info("Инициализация встроенной PostgreSQL в статическом блоке...");
            initializeDatabase();
        } catch (Exception e) {
            log.error("Не удалось инициализировать базу данных: {}", e.getMessage());
            throw new RuntimeException("Не удалось инициализировать базу данных", e);
        }
    }

    /**
     * Инициализирует временную директорию проекта внутри target/.
     */
    private static void initializeProjectTempDirectory() {
        try {
            String projectBaseDir = getProjectBaseDir();
            log.info("Базовая директория проекта: {}", projectBaseDir);

            projectTempDir = Paths.get(projectBaseDir, "target", "tmp");

            if (!Files.exists(projectTempDir)) {
                Files.createDirectories(projectTempDir);
                log.info("Создана временная директория проекта: {}", projectTempDir);
            }

        } catch (Exception e) {
            log.error("Не удалось инициализировать временную директорию проекта: {}", e.getMessage());
            projectTempDir = Paths.get(System.getProperty("java.io.tmpdir", "/tmp"));
            log.info("Используется системная временная директория как запасной вариант: {}", projectTempDir);
        }
    }

    /**
     * Определяет базовую директорию текущего модуля.
     */
    private static String getProjectBaseDir() {
        String basedir = System.getProperty("basedir");
        if (basedir != null && !basedir.isBlank()) {
            Path path = Paths.get(basedir).normalize();
            log.info("Используется basedir из Maven: {}", path);
            return path.toString();
        }

        String userDir = System.getProperty("user.dir");
        Path currentPath = Paths.get(userDir).normalize();
        log.info("Используется user.dir из IDE: {}", currentPath);

        Path moduleRoot = findModuleRoot(currentPath);
        if (moduleRoot != null) {
            log.info("Найден корень модуля: {}", moduleRoot);
            return moduleRoot.toString();
        }

        log.warn("Не удалось определить корень модуля, используется user.dir: {}", currentPath);
        return currentPath.toString();
    }

    /**
     * Рекурсивно ищет корень модуля (директорию с pom.xml).
     */
    private static Path findModuleRoot(Path startPath) {
        Path current = startPath;

        for (int i = 0; i < 10; i++) {
            Path pomFile = current.resolve("pom.xml");
            if (Files.exists(pomFile)) {
                log.debug("Найден pom.xml по пути: {}", pomFile);
                return current;
            }

            Path parent = current.getParent();
            if (parent == null) {
                break;
            }
            current = parent;
        }

        log.debug("Не найден pom.xml в иерархии путей от: {}", startPath);
        return null;
    }

    /**
     * Возвращает базовый URL для HTTP запросов.
     */
    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Инициализирует экземпляр Embedded PostgreSQL.
     * Все соединения закрываются через try-with-resources.
     */
    private static void initializeDatabase() throws IOException, SQLException {
        lock.lock();
        try {
            if (initialized.get()) {
                return;
            }

            log.info("projectTempDir = {}", projectTempDir);

            Path dataPath = projectTempDir.resolve("embedded-postgres-data");
            log.info("Создание директории для данных: {}", dataPath);
            Files.createDirectories(dataPath);

            if (!Files.exists(dataPath)) {
                throw new IOException("Не удалось создать директорию для данных: " + dataPath);
            }
            log.info("Директория для данных существует: {}", dataPath);

            // Опционально: вывод содержимого для отладки
            if (Files.exists(dataPath)) {
                log.info("Содержимое директории данных:");
                try (var stream = Files.list(dataPath)) {
                    stream.forEach(p -> log.info("  {}", p.getFileName()));
                } catch (IOException e) {
                    log.warn("Не удалось вывести список содержимого директории данных: {}", e.getMessage());
                }
            }

            int freePort;
            try {
                freePort = DatabasePropertyFactory.findFreePort();
            } catch (IOException e) {
                throw new IOException("Не удалось найти свободный порт для PostgreSQL: " + e.getMessage(), e);
            }
            log.info("Найден свободный порт для PostgreSQL: {}", freePort);

            EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder()
                    .setPort(freePort)
                    .setDataDirectory(dataPath.toString())
                    .setServerConfig("max_connections", "10")
                    .setServerConfig("shared_buffers", "128MB")
                    .setServerConfig("log_statement", "none")
                    .setServerConfig("listen_addresses", "localhost");

            log.info("Запуск встроенной PostgreSQL...");
            try {
                embeddedPostgres = builder.start();
            } catch (IOException e) {
                throw new IOException("Не удалось запустить PostgreSQL: " + e.getMessage(), e);
            }
            databasePort = embeddedPostgres.getPort();
            databaseUrl = "jdbc:postgresql://localhost:%d/postgres".formatted(databasePort);
            log.info("PostgreSQL запущена на порту: {}", databasePort);

            log.info("Ожидание готовности базы данных к подключению...");
            int maxRetries = 30;
            boolean connected = false;
            for (int i = 0; i < maxRetries; i++) {
                try (Connection conn = DriverManager.getConnection(databaseUrl, "postgres", "postgres")) {
                    if (conn.isValid(CONNECTION_VALIDITY_TIMEOUT_SECONDS)) {
                        log.info("Подключение к базе данных успешно!");
                        connected = true;
                        break;
                    }
                } catch (SQLException e) {
                    log.info("Ожидание базы данных... попытка {}/{}", i + 1, maxRetries);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Прервано ожидание подключения к базе данных", ie);
                    }
                }
            }

            if (!connected) {
                throw new SQLException("База данных не запустилась после " + maxRetries + " секунд");
            }

            // ========== ИНИЦИАЛИЗАЦИЯ СХЕМЫ ==========
            try (Connection conn = DriverManager.getConnection(databaseUrl, "postgres", "postgres");
                 Statement stmt = conn.createStatement()) {

                conn.setAutoCommit(true);
                stmt.execute("CREATE SCHEMA IF NOT EXISTS public");

                String schemaSql = loadSchemaScript();
                stmt.execute(schemaSql);
                log.info("Схема базы данных инициализирована успешно");
            } catch (SQLException e) {
                log.error("Ошибка при инициализации схемы: {}", e.getMessage());
                throw e;
            }
            // ======================================

            if (Files.exists(dataPath)) {
                log.info("Содержимое директории данных:");
                try (var stream = Files.list(dataPath)) {
                    stream.forEach(p -> log.info("  {}", p.getFileName()));
                } catch (IOException e) {
                    log.warn("Не удалось вывести список содержимого директории данных: {}", e.getMessage());
                }
            } else {
                log.error("Директория данных не существует после запуска!");
            }

            initialized.set(true);
            log.info("=== ИНИЦИАЛИЗАЦИЯ БАЗЫ ДАННЫХ ЗАВЕРШЕНА ===");

        } catch (IOException e) {
            log.error("Ошибка ввода-вывода при запуске PostgreSQL: {}", e.getMessage(), e);
            throw e;
        } catch (SQLException e) {
            log.error("Ошибка SQL при запуске PostgreSQL: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при запуске PostgreSQL: {}", e.getMessage(), e);
            throw new RuntimeException("Непредвиденная ошибка при запуске PostgreSQL", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Загружает SQL скрипт для инициализации схемы.
     */
    private static String loadSchemaScript() throws IOException {
        ClassLoader classLoader = AbstractIntegrationTest.class.getClassLoader();
        String[] possiblePaths = {"db/test/schema.sql", "/db/test/schema.sql"};

        for (String path : possiblePaths) {
            try (var inputStream = classLoader.getResourceAsStream(path)) {
                if (inputStream != null) {
                    log.info("Загружена схема из: {}", path);
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        String userDir = System.getProperty("user.dir");
        String[] fsPaths = {
                userDir + "/src/test/resources/db/test/schema.sql",
                userDir + "/target/test-classes/db/test/schema.sql",
                userDir + "/../common/common-test/src/main/resources/db/test/schema.sql"
        };

        for (String fsPath : fsPaths) {
            File file = new File(fsPath);
            if (file.exists()) {
                log.info("Найден schema.sql в файловой системе: {}", fsPath);
                return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
        }

        throw new IOException("""
                Скрипт схемы не найден в classpath или файловой системе.
                Ожидаемое расположение: src/test/resources/db/test/schema.sql
                """);
    }

    /**
     * Рекурсивно удаляет директорию и все её содержимое.
     */
    private static void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        log.warn("Не удалось удалить файл: {}", file.getAbsolutePath());
                    }
                }
            }
        }

        if (!directory.delete()) {
            log.warn("Не удалось удалить директорию: {}", directory.getAbsolutePath());
        }
    }

    /**
     * Очищает временные файлы проекта.
     */
    private static void cleanupProjectTempFiles() {
        try {
            if (projectTempDir == null) {
                return;
            }

            Path dataDir = projectTempDir.resolve("embedded-postgres-data");
            if (Files.exists(dataDir)) {
                // Используем deleteDirectory для рекурсивного удаления
                deleteDirectory(dataDir.toFile());
                log.info("Очищено: {}", dataDir);
            }

        } catch (Exception e) {
            log.warn("Не удалось очистить временные файлы проекта: {}", e.getMessage());
        }
    }

    @BeforeAll
    void verifyDatabase() {
        if (!initialized.get()) {
            throw new IllegalStateException("База данных не инициализирована");
        }
        log.info("База данных готова: {}", databaseUrl);
    }

    /**
     * Останавливает embedded PostgreSQL после всех тестов.
     * Все соединения закрываются через try-with-resources.
     */
    @AfterAll
    static void stopDatabase() {
        lock.lock();
        try {
            if (embeddedPostgres != null) {
                log.info("Остановка встроенной PostgreSQL...");

                try (Connection conn = DriverManager.getConnection(databaseUrl, "postgres", "postgres");
                     Statement stmt = conn.createStatement()) {

                    stmt.execute("""
                            -- noinspection SqlNoDataSourceInspectionForFile
                            SELECT pg_terminate_backend(pid)
                                                        FROM pg_stat_activity
                                                        WHERE datname = 'postgres'
                                                        AND pid <> pg_backend_pid()
                                                        AND state = 'active'
                            """);
                    log.info("Активные соединения завершены");

                } catch (Exception e) {
                    log.warn("Ошибка при завершении соединений: {}", e.getMessage());
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                embeddedPostgres.close();
                log.info("PostgreSQL остановлена успешно");
            }
        } catch (Exception e) {
            log.warn("Ошибка при остановке PostgreSQL: {}", e.getMessage());
        } finally {
            embeddedPostgres = null;
            initialized.set(false);
            databaseUrl = null;
            databasePort = 0;
            lock.unlock();

            cleanupProjectTempFiles();
        }
    }

    /**
     * Регистрирует свойства для Spring DataSource.
     */
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        if (!initialized.get() || databaseUrl == null) {
            throw new IllegalStateException("База данных не инициализирована");
        }

        log.info("Настройка Spring DataSource с URL: {}", databaseUrl);

        registry.add("spring.datasource.url", () -> databaseUrl);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.datasource.hikari.autoCommit", () -> "true");
        registry.add("spring.datasource.hikari.connectionTimeout", () -> "60000");
        registry.add("spring.datasource.hikari.maximumPoolSize", () -> "10");
        registry.add("spring.datasource.hikari.idleTimeout", () -> "600000");
        registry.add("spring.datasource.hikari.maxLifetime", () -> "1800000");
        registry.add("spring.datasource.hikari.validationTimeout", () -> "10000");
        registry.add("spring.datasource.hikari.leakDetectionThreshold", () -> "30000");
        registry.add("spring.datasource.hikari.connectionTestQuery", () -> "SELECT 1");
        registry.add("spring.datasource.hikari.keepaliveTime", () -> "30000");

        registry.add("spring.jpa.properties.hibernate.jdbc.timeout", () -> "60");
        registry.add("spring.jpa.properties.hibernate.jdbc.batch_size", () -> "20");
        registry.add("spring.jpa.properties.hibernate.query.in_clause_parameter_padding", () -> "true");
        registry.add("spring.jpa.properties.jakarta.persistence.query.timeout", () -> "120000");
    }

    protected void waitForDatabase() {
        if (embeddedPostgres == null) {
            throw new IllegalStateException("EmbeddedPostgres не инициализирован");
        }

        Duration timeout = Duration.ofSeconds(WAIT_FOR_DATABASE_TIMEOUT_SECONDS);
        var startTime = java.time.Instant.now();

        while (Duration.between(startTime, java.time.Instant.now()).compareTo(timeout) < 0) {
            try (Connection conn = DriverManager.getConnection(databaseUrl, "postgres", "postgres")) {
                if (conn.isValid(CONNECTION_VALIDITY_TIMEOUT_SECONDS)) {
                    log.info("Подключение к базе данных валидно");
                    return;
                }
            } catch (SQLException e) {
                log.debug("Ожидание готовности базы данных: {}", e.getMessage());
            }

            try {
                TimeUnit.MILLISECONDS.sleep(WAIT_FOR_DATABASE_RETRY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Прервано ожидание готовности базы данных", ie);
            }
        }

        throw new RuntimeException("База данных не готова после " + WAIT_FOR_DATABASE_TIMEOUT_SECONDS + " секунд");
    }

    protected static String getDatabaseUrl() {
        return databaseUrl;
    }

    protected static int getDatabasePort() {
        return databasePort;
    }

    protected static boolean isDatabaseInitialized() {
        return initialized.get();
    }
}