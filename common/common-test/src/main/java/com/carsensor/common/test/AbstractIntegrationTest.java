package com.carsensor.common.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
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
 * <h2>Управление временными файлами:</h2>
 * <p>Временные файлы создаются в {@code target/embedded-postgres-data/} и автоматически
 * очищаются после выполнения тестов. Глобальные кэши (Maven, ~/.embedded-postgres)
 * НЕ затрагиваются, так как это нарушает принцип изоляции тестов.</p>
 *
 * @see EmbeddedPostgres
 * @see DynamicPropertySource
 * @since 1.0
 */
@Slf4j
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Timeout(value = 120, unit = TimeUnit.SECONDS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

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

    /**
     * JUnit управляет очисткой этой директории автоматически.
     * Используется для временных файлов, которые не требуют ручного удаления.
     */
    @TempDir
    static Path tempDir;

    static {
        initializeProjectTempDirectory();

        try {
            log.info("Initializing embedded PostgreSQL in static block...");
            initializeDatabase();
        } catch (Exception e) {
            log.error("Failed to initialize database: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Инициализирует временную директорию проекта внутри target/.
     *
     * <p><b>Принцип:</b> Все временные файлы создаются внутри target/ текущего проекта,
     * что обеспечивает изоляцию от других проектов и упрощает очистку.
     *
     * <p><b>Почему это правильно:</b>
     * <ul>
     *   <li>Файлы удаляются при выполнении {@code mvn clean}</li>
     *   <li>Не затрагивает глобальные директории других проектов</li>
     *   <li>Позволяет параллельный запуск тестов в разных проектах</li>
     * </ul>
     */
    private static void initializeProjectTempDirectory() {
        try {
            String projectBaseDir = getProjectBaseDir();
            log.info("Project base directory: {}", projectBaseDir);

            projectTempDir = Paths.get(projectBaseDir, "target", "tmp");

            if (!Files.exists(projectTempDir)) {
                Files.createDirectories(projectTempDir);
                log.info("Created project temp directory: {}", projectTempDir);
            }

        } catch (Exception e) {
            log.error("Failed to initialize project temp directory: {}", e.getMessage());
            projectTempDir = Paths.get(System.getProperty("java.io.tmpdir", "/tmp"));
            log.info("Using system temp directory as fallback: {}", projectTempDir);
        }
    }

    /**
     * Определяет базовую директорию текущего модуля.
     *
     * <p><b>Алгоритм:</b>
     * <ol>
     *   <li>Проверяет системное свойство {@code basedir} (устанавливается Maven)</li>
     *   <li>Использует {@code user.dir} для IDE</li>
     *   <li>Нормализует путь, удаляя дублирования</li>
     *   <li>Гарантирует, что путь заканчивается на {@code auth-service}</li>
     * </ol>
     *
     * @return абсолютный путь к корню модуля
     */
    private static String getProjectBaseDir() {
        String basedir = System.getProperty("basedir");
        if (basedir != null && !basedir.isBlank()) {
            return Paths.get(basedir).normalize().toString();
        }

        String userDir = System.getProperty("user.dir");
        Path normalizedPath = Paths.get(userDir).normalize();

        // Удаляем дублирование пути (баг в некоторых IDE)
        String normalizedStr = normalizedPath.toString();
        if (normalizedStr.contains("services/auth-service/services/auth-service")) {
            normalizedStr = normalizedStr.replace(
                    "/services/auth-service/services/auth-service",
                    "/services/auth-service"
            );
        }

        Path finalPath = Paths.get(normalizedStr);

        // Убеждаемся, что путь указывает на корень модуля auth-service
        if (!finalPath.toString().endsWith("auth-service")) {
            Path authServicePath = findAuthServicePath(finalPath);
            if (authServicePath != null) {
                finalPath = authServicePath;
            }
        }

        return finalPath.toString();
    }

    /**
     * Рекурсивно ищет директорию auth-service вверх по дереву.
     *
     * @param startPath начальная точка поиска
     * @return путь к auth-service или null
     */
    private static Path findAuthServicePath(Path startPath) {
        Path current = startPath;
        for (int i = 0; i < 5; i++) {
            if (current.toString().endsWith("auth-service")) {
                return current;
            }
            Path parent = current.getParent();
            if (parent == null) {
                break;
            }
            current = parent;
        }
        return null;
    }

    /**
     * Возвращает базовый URL для HTTP запросов.
     *
     * @return URL в формате http://localhost:{port}
     */
    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Инициализирует экземпляр Embedded PostgreSQL.
     *
     * <p><b>Особенности:</b>
     * <ul>
     *   <li>Автоматический поиск свободного порта</li>
     *   <li>Данные сохраняются в {@code target/embedded-postgres-data/}</li>
     *   <li>autoCommit=true для корректной работы JPA</li>
     *   <li>Выполнение SQL скриптов по частям для детальной обработки ошибок</li>
     * </ul>
     *
     * @throws IOException  если не удалось запустить PostgreSQL
     * @throws SQLException если ошибка выполнения SQL
     */
    private static void initializeDatabase() throws IOException, SQLException {
        lock.lock();
        try {
            if (initialized.get()) {
                return;
            }

            // Проверяем projectTempDir
            log.info("projectTempDir = {}", projectTempDir);

            // Используем директорию внутри target/ для данных PostgreSQL
            Path dataPath = projectTempDir.resolve("embedded-postgres-data");
            log.info("Creating data directory: {}", dataPath);
            Files.createDirectories(dataPath);

            // Проверяем, что директория создана
            if (!Files.exists(dataPath)) {
                throw new IOException("Failed to create data directory: " + dataPath);
            }
            log.info("Data directory exists: {}", dataPath);

            int freePort = DatabasePropertyFactory.findFreePort();
            log.info("Found free port for PostgreSQL: {}", freePort);

            EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder()
                    .setPort(freePort)
                    .setDataDirectory(dataPath.toString())
                    .setServerConfig("max_connections", "10")
                    .setServerConfig("shared_buffers", "128MB")
                    .setServerConfig("log_statement", "none")
                    .setServerConfig("listen_addresses", "localhost");

            log.info("Starting embedded PostgreSQL...");
            embeddedPostgres = builder.start();
            databasePort = embeddedPostgres.getPort();
            databaseUrl = "jdbc:postgresql://localhost:%d/postgres".formatted(databasePort);
            log.info("PostgreSQL started on port: {}", databasePort);

            // ========== ДОБАВИТЬ ЭТОТ БЛОК ==========
            // Ждем, пока БД будет готова принимать подключения
            log.info("Waiting for database to accept connections...");
            int maxRetries = 30;
            for (int i = 0; i < maxRetries; i++) {
                try (Connection conn = DriverManager.getConnection(databaseUrl, "postgres", "postgres")) {
                    log.info("Database connection successful!");
                    break;
                } catch (SQLException e) {
                    log.info("Waiting for database... attempt {}/{}", i + 1, maxRetries);
                    Thread.sleep(1000);
                    if (i == maxRetries - 1) {
                        throw new SQLException("Database did not start after " + maxRetries + " seconds", e);
                    }
                }
            }
            // ========== КОНЕЦ ДОБАВЛЕННОГО БЛОКА ==========

            // Проверяем, что данные создались
            if (Files.exists(dataPath)) {
                log.info("Data directory contents:");
                try (var stream = Files.list(dataPath)) {
                    stream.forEach(p -> log.info("  {}", p.getFileName()));
                } catch (IOException e) {
                    log.warn("Failed to list data directory contents: {}", e.getMessage());
                }
            } else {
                log.error("Data directory does NOT exist after start!");
            }

            // Инициализация схемы
            try (Connection conn = DriverManager.getConnection(databaseUrl, "postgres", "postgres");
                 var stmt = conn.createStatement()) {

                conn.setAutoCommit(true);
                stmt.execute("CREATE SCHEMA IF NOT EXISTS public");

                log.info("Loading schema script...");
                String schemaSql = loadSchemaScript();

                stmt.execute(schemaSql);
                log.info("Database schema initialized successfully");
            }

            initialized.set(true);
            log.info("=== DATABASE INITIALIZATION COMPLETED ===");

        } catch (Exception e) {
            log.error("Failed to start embedded PostgreSQL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start embedded PostgreSQL", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Загружает SQL скрипт для инициализации схемы.
     *
     * <p>Ищет файл в classpath по пути {@code db/test/schema.sql}.
     * При неудаче проверяет файловую систему для отладки.
     *
     * @return содержимое SQL скрипта
     * @throws IOException если файл не найден
     */
    private static String loadSchemaScript() throws IOException {
        ClassLoader classLoader = AbstractIntegrationTest.class.getClassLoader();
        String[] possiblePaths = {"db/test/schema.sql", "/db/test/schema.sql"};

        for (String path : possiblePaths) {
            try (var inputStream = classLoader.getResourceAsStream(path)) {
                if (inputStream != null) {
                    log.info("Loaded schema from: {}", path);
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
                log.info("Found schema.sql in filesystem: {}", fsPath);
                return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
        }

        throw new IOException("""
                Schema script not found in classpath or filesystem.
                Expected location: src/test/resources/db/test/schema.sql
                """);
    }

    /**
     * Рекурсивно удаляет директорию и все её содержимое.
     *
     * <p><b>Важно:</b> Удаляются только файлы внутри target/ проекта.
     *
     * @param directory директория для удаления
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
                        log.warn("Failed to delete file: {}", file.getAbsolutePath());
                    }
                }
            }
        }

        if (!directory.delete()) {
            log.warn("Failed to delete directory: {}", directory.getAbsolutePath());
        }
    }

    /**
     * Очищает временные файлы проекта.
     *
     * <p><b>Что очищается:</b>
     * <ul>
     *   <li>{@code target/tmp/} - временные файлы тестов</li>
     *   <li>{@code target/embedded-postgres-data/} - данные PostgreSQL</li>
     * </ul>
     *
     * <p><b>Что НЕ очищается (сознательно):</b>
     * <ul>
     *   <li>Глобальный кэш Maven (~/.m2/)</li>
     *   <li>Глобальный кэш embedded-postgres (~/.embedded-postgres)</li>
     *   <li>Директории других проектов</li>
     * </ul>
     */
    private static void cleanupProjectTempFiles() {
        try {
            if (projectTempDir == null) {
                return;
            }

            Path dataDir = projectTempDir.resolve("embedded-postgres-data");
            if (Files.exists(dataDir)) {
                Files.walk(dataDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("Cleaned: {}", dataDir);
            }

        } catch (Exception e) {
            log.warn("Failed to cleanup project temp files: {}", e.getMessage());
        }
    }

    @BeforeAll
    void verifyDatabase() {
        if (!initialized.get()) {
            throw new IllegalStateException("Database not initialized");
        }
        log.info("Database is ready: {}", databaseUrl);
    }

    /**
     * Останавливает embedded PostgreSQL после всех тестов.
     *
     * <p><b>Процесс остановки:</b>
     * <ol>
     *   <li>Завершает все активные соединения</li>
     *   <li>Дает время на завершение транзакций (1 сек)</li>
     *   <li>Закрывает PostgreSQL</li>
     * </ol>
     */
    @AfterAll
    static void stopDatabase() {
        lock.lock();
        try {
            if (embeddedPostgres != null) {
                log.info("Stopping embedded PostgreSQL...");

                // Принудительное завершение активных соединений
                try (Connection conn = embeddedPostgres.getPostgresDatabase().getConnection();
                     var stmt = conn.createStatement()) {

                    stmt.execute("""
                                SELECT pg_terminate_backend(pid) 
                                FROM pg_stat_activity
                                WHERE datname = 'postgres'
                                AND pid <> pg_backend_pid()
                                AND state = 'active'
                            """);
                    log.info("Active connections terminated");

                } catch (Exception e) {
                    log.warn("Error terminating connections: {}", e.getMessage());
                }

                // Ожидание завершения транзакций
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                embeddedPostgres.close();
                log.info("PostgreSQL stopped successfully");
            }
        } catch (Exception e) {
            log.warn("Error stopping embedded PostgreSQL: {}", e.getMessage());
        } finally {
            embeddedPostgres = null;
            initialized.set(false);
            databaseUrl = null;
            databasePort = 0;
            lock.unlock();

            // Очистка временных файлов проекта
            cleanupProjectTempFiles();
        }
    }

    /**
     * Регистрирует свойства для Spring DataSource.
     *
     * <p><b>Важные настройки:</b>
     * <ul>
     *   <li>{@code autoCommit=true} - для корректной работы JPA</li>
     *   <li>{@code maximumPoolSize=5} - уменьшенный пул для тестов</li>
     *   <li>{@code connectionTimeout=30000} - 30 секунд на подключение</li>
     *   <li>{@code leakDetectionThreshold=30000} - обнаружение утечек через 30 сек</li>
     * </ul>
     */
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        if (!initialized.get() || databaseUrl == null) {
            throw new IllegalStateException("Database not initialized");
        }

        log.info("Configuring Spring DataSource with URL: {}", databaseUrl);

        registry.add("spring.datasource.url", () -> databaseUrl);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // HikariCP настройки для тестов
        registry.add("spring.datasource.hikari.autoCommit", () -> "true");
        registry.add("spring.datasource.hikari.connectionTimeout", () -> "30000");
        registry.add("spring.datasource.hikari.maximumPoolSize", () -> "5");
        registry.add("spring.datasource.hikari.idleTimeout", () -> "60000");
        registry.add("spring.datasource.hikari.maxLifetime", () -> "180000");
        registry.add("spring.datasource.hikari.validationTimeout", () -> "5000");
        registry.add("spring.datasource.hikari.leakDetectionThreshold", () -> "30000");
        registry.add("spring.datasource.hikari.connectionTestQuery", () -> "SELECT 1");

        // JPA настройки
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.properties.hibernate.jdbc.timeout", () -> "30");
        registry.add("spring.jpa.properties.javax.persistence.query.timeout", () -> "30000");
    }

    protected void waitForDatabase() {
        if (embeddedPostgres == null) {
            throw new IllegalStateException("EmbeddedPostgres is not initialized");
        }

        Duration timeout = Duration.ofSeconds(WAIT_FOR_DATABASE_TIMEOUT_SECONDS);
        var startTime = java.time.Instant.now();

        while (Duration.between(startTime, java.time.Instant.now()).compareTo(timeout) < 0) {
            try (Connection conn = embeddedPostgres.getPostgresDatabase().getConnection()) {
                if (conn.isValid(CONNECTION_VALIDITY_TIMEOUT_SECONDS)) {
                    log.info("Database connection is valid");
                    return;
                }
            } catch (SQLException e) {
                log.debug("Waiting for database to be ready: {}", e.getMessage());
            }

            try {
                TimeUnit.MILLISECONDS.sleep(WAIT_FOR_DATABASE_RETRY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for database", ie);
            }
        }

        throw new RuntimeException("Database not ready after " + WAIT_FOR_DATABASE_TIMEOUT_SECONDS + " seconds");
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