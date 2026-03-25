package com.carsensor.common.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.extern.slf4j.Slf4j;

/**
 * Абстрактный базовый класс для JPA тестов репозиториев.
 *
 * <p><b>Важно:</b> Этот класс НЕ содержит @DataJpaTest, @SpringBootTest, @AutoConfigureMockMvc.
 * Конфликтующие аннотации должны быть в дочерних классах.
 *
 * <p><b>Предоставляет:</b>
 * <ul>
 *   <li>Embedded PostgreSQL для тестов</li>
 *   <li>Динамические свойства DataSource</li>
 *   <li>Управление временными файлами</li>
 * </ul>
 */
@Slf4j
@ActiveProfiles("test")
@Timeout(value = 120, unit = TimeUnit.SECONDS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractJpaTest {

    private static final int CONNECTION_VALIDITY_TIMEOUT_SECONDS = 5;

    private static volatile EmbeddedPostgres embeddedPostgres;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final ReentrantLock lock = new ReentrantLock();
    private static volatile String databaseUrl;
    private static volatile int databasePort;
    private static volatile Path projectTempDir;

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * Возвращает базовую директорию текущего модуля.
     */
    protected static String getProjectBaseDir() {
        String basedir = System.getProperty("basedir");
        if (basedir != null && !basedir.isBlank()) {
            return Paths.get(basedir).normalize().toString();
        }

        String userDir = System.getProperty("user.dir");
        Path currentPath = Paths.get(userDir);
        while (currentPath != null && !Files.exists(currentPath.resolve("pom.xml"))) {
            currentPath = currentPath.getParent();
        }
        return currentPath != null ? currentPath.toString() : userDir;
    }

    static {
        try {
            log.info("Initializing embedded PostgreSQL for JPA tests...");
            initializeProjectTempDirectory();
            initializeDatabase();
        } catch (Exception e) {
            log.error("Failed to initialize database: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

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

    private static void initializeDatabase() throws IOException, SQLException {
        lock.lock();
        try {
            if (initialized.get()) {
                return;
            }

            log.info("projectTempDir = {}", projectTempDir);

            Path dataPath = projectTempDir.resolve("embedded-postgres-data-jpa");
            log.info("Creating data directory: {}", dataPath);
            Files.createDirectories(dataPath);

            if (!Files.exists(dataPath)) {
                throw new IOException("Failed to create data directory: " + dataPath);
            }
            log.info("Data directory exists: {}", dataPath);

            int freePort = DatabasePropertyFactory.findFreePort();
            log.info("Found free port for JPA PostgreSQL: {}", freePort);
            log.info("Starting embedded PostgreSQL for JPA tests...");

            EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder()
                    .setPort(freePort)
                    .setDataDirectory(dataPath.toString())
                    .setServerConfig("max_connections", "10")
                    .setServerConfig("shared_buffers", "128MB")
                    .setServerConfig("log_statement", "none")
                    .setServerConfig("listen_addresses", "localhost");

            embeddedPostgres = builder.start();
            databasePort = embeddedPostgres.getPort();
            databaseUrl = String.format("jdbc:postgresql://localhost:%d/postgres", databasePort);
            log.info("PostgreSQL started on port: {}", databasePort);

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

            initialized.set(true);
            log.info("=== DATABASE INITIALIZATION COMPLETED ===");

        } catch (Exception e) {
            log.error("Failed to start embedded PostgreSQL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start embedded PostgreSQL", e);
        } finally {
            lock.unlock();
        }
    }

    @BeforeAll
    void verifyDatabase() {
        if (!initialized.get()) {
            throw new IllegalStateException("Database not initialized");
        }
        log.info("Database ready for JPA tests: {}", databaseUrl);
    }

    @AfterAll
    static void stopDatabase() {
        lock.lock();
        try {
            if (embeddedPostgres != null) {
                log.info("Stopping embedded PostgreSQL for JPA tests...");

                try (Connection conn = DriverManager.getConnection(databaseUrl, "postgres", "postgres");
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
        }
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        if (!initialized.get() || databaseUrl == null) {
            throw new IllegalStateException("Database not initialized");
        }

        log.info("Configuring JPA test database: {}", databaseUrl);
        DatabasePropertyFactory.registerJpaProperties(registry, embeddedPostgres);
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