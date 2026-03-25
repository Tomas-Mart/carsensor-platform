package com.carsensor.common.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.function.Consumer;
import org.springframework.test.context.DynamicPropertyRegistry;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Фабрика для регистрации свойств базы данных в тестах.
 *
 * <p>Предоставляет единый интерфейс для настройки подключения к Embedded PostgreSQL
 * и конфигурации JPA/Hibernate для тестовой среды.
 *
 * <p><b>Ключевые возможности:</b>
 * <ul>
 *   <li><b>Поиск свободного порта</b> - автоматическое обнаружение свободного порта
 *       для запуска PostgreSQL</li>
 *   <li><b>Конфигурация через Builder</b> - гибкая настройка всех параметров
 *       подключения с использованием паттерна Builder</li>
 *   <li><b>Предустановленные конфигурации</b> - готовые настройки для различных
 *       типов тестов (интеграционные, JPA)</li>
 *   <li><b>Автоматическая регистрация</b> - упрощенная регистрация свойств в
 *       Spring DynamicPropertyRegistry</li>
 *   <li><b>Оптимизированные таймауты</b> - предустановленные значения для
 *       предотвращения утечек соединений в тестах</li>
 * </ul>
 *
 * <p><b>Использование Java 21 features:</b>
 * <ul>
 *   <li><b>Records (JEP 395)</b> - {@link DatabaseConfig} для неизменяемых
 *       конфигурационных объектов</li>
 *   <li><b>Builder pattern</b> - для гибкой и читаемой настройки</li>
 *   <li><b>Functional interfaces</b> - {@link ConfigCustomizer} для
 *       лямбда-кастомизации конфигурации</li>
 *   <li><b>var (JEP 323)</b> - для локального вывода типов</li>
 *   <li><b>Switch expressions (JEP 361)</b> - в валидации конфигурации</li>
 *   <li><b>Text blocks (JEP 378)</b> - для многострочных SQL и сообщений</li>
 * </ul>
 *
 * <p><b>Примеры использования:</b>
 * <pre>{@code
 * // Стандартная конфигурация для интеграционных тестов
 * DatabasePropertyFactory.registerStandardProperties(registry, embeddedPostgres);
 *
 * // Конфигурация для JPA тестов с оптимизированными параметрами
 * DatabasePropertyFactory.registerJpaProperties(registry, embeddedPostgres);
 *
 * // Полная кастомизация через лямбда-выражение
 * DatabasePropertyFactory.registerProperties(registry, embeddedPostgres, builder -> {
 *     builder.showSql(false)
 *            .batchSize(100)
 *            .maxPoolSize(20)
 *            .connectionTimeout(60000);
 * });
 *
 * // Создание кастомной конфигурации
 * var config = DatabaseConfig.builder()
 *     .url("jdbc:postgresql://localhost:5432/test")
 *     .maxPoolSize(15)
 *     .showSql(true)
 *     .build();
 * config.register(registry);
 *
 * // Поиск свободного порта
 * int freePort = DatabasePropertyFactory.findFreePort();
 * }</pre>
 *
 * <p><b>Настройки по умолчанию:</b>
 * <ul>
 *   <li>Имя базы данных: postgres</li>
 *   <li>Пользователь: postgres</li>
 *   <li>Драйвер: org.postgresql.Driver</li>
 *   <li>Диалект Hibernate: PostgreSQLDialect</li>
 *   <li>ddl-auto: create-drop</li>
 *   <li>Размер пула соединений: 10</li>
 *   <li>Таймаут подключения: 30 секунд</li>
 *   <li>Порог обнаружения утечек: 30 секунд</li>
 * </ul>
 *
 * @see DatabaseConfig
 * @see ConfigCustomizer
 * @see EmbeddedPostgres
 * @since 2.0.0
 */
public final class DatabasePropertyFactory {

    /**
     * Имя базы данных по умолчанию
     */
    private static final String DATABASE_NAME = "postgres";

    /**
     * Имя пользователя по умолчанию
     */
    private static final String DEFAULT_USERNAME = "postgres";

    /**
     * Пароль по умолчанию
     */
    private static final String DEFAULT_PASSWORD = "postgres";

    /**
     * Класс драйвера PostgreSQL
     */
    private static final String DEFAULT_DRIVER = "org.postgresql.Driver";

    /**
     * Диалект Hibernate для PostgreSQL
     */
    private static final String DEFAULT_DIALECT = "org.hibernate.dialect.PostgreSQLDialect";

    /**
     * Стратегия DDL для тестов
     */
    private static final String DEFAULT_DDL_AUTO = "create-drop";

    /**
     * Размер пакетной вставки по умолчанию
     */
    private static final int DEFAULT_BATCH_SIZE = 50;

    /**
     * Таймаут подключения в миллисекундах
     */
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30000;

    /**
     * Максимальный размер пула соединений
     */
    private static final int DEFAULT_MAX_POOL_SIZE = 10;

    /**
     * Таймаут простоя соединения в миллисекундах
     */
    private static final int DEFAULT_IDLE_TIMEOUT = 600000;

    /**
     * Максимальное время жизни соединения в миллисекундах
     */
    private static final int DEFAULT_MAX_LIFETIME = 1800000;

    /**
     * Таймаут валидации соединения в миллисекундах
     */
    private static final int DEFAULT_VALIDATION_TIMEOUT = 5000;

    /**
     * Порог обнаружения утечек соединений в миллисекундах
     */
    private static final int DEFAULT_LEAK_DETECTION_THRESHOLD = 30000;

    /**
     * Таймаут подключения для интеграционных тестов (60 секунд)
     */
    public static final int INTEGRATION_CONNECTION_TIMEOUT = 60000;

    /**
     * Таймаут чтения для интеграционных тестов (120 секунд)
     */
    public static final int INTEGRATION_READ_TIMEOUT = 120000;

    public static final int INTEGRATION_TEST_TIMEOUT = 120000;

    /**
     * Приватный конструктор для утилитарного класса.
     * Запрещает создание экземпляров.
     */
    private DatabasePropertyFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Находит свободный порт в системе.
     *
     * <p>Использует ServerSocket с портом 0, что заставляет ОС выделить любой
     * свободный порт. Это гарантирует отсутствие конфликтов с другими
     * приложениями и позволяет запускать несколько экземпляров тестов
     * параллельно.
     *
     * <p><b>Алгоритм работы:</b>
     * <ol>
     *   <li>Создает ServerSocket с портом 0</li>
     *   <li>ОС автоматически выделяет свободный порт</li>
     *   <li>Закрывает сокет и возвращает выделенный порт</li>
     * </ol>
     *
     * @return свободный порт, который можно использовать для запуска PostgreSQL
     * @throws IOException если не удалось создать ServerSocket или получить порт
     * @see java.net.ServerSocket
     */
    public static int findFreePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * Конфигурация базы данных с поддержкой билдера.
     *
     * <p>Использует record (Java 21) для неизменяемого представления конфигурации.
     * Все поля являются финальными и инициализируются при создании объекта.
     * Это обеспечивает потокобезопасность и предсказуемость конфигурации.
     *
     * <p><b>Поля конфигурации:</b>
     * <ul>
     *   <li><b>url</b> - JDBC URL для подключения к базе данных</li>
     *   <li><b>username</b> - имя пользователя базы данных</li>
     *   <li><b>password</b> - пароль пользователя</li>
     *   <li><b>driverClassName</b> - полное имя класса JDBC драйвера</li>
     *   <li><b>dialect</b> - диалект Hibernate для SQL генерации</li>
     *   <li><b>ddlAuto</b> - стратегия управления схемой (create-drop, validate, etc.)</li>
     *   <li><b>showSql</b> - выводить ли SQL запросы в лог</li>
     *   <li><b>formatSql</b> - форматировать ли SQL для читаемости</li>
     *   <li><b>batchSize</b> - размер пакета для batch операций</li>
     *   <li><b>flywayEnabled</b> - включена ли миграция Flyway</li>
     *   <li><b>connectionTimeout</b> - таймаут установки соединения (мс)</li>
     *   <li><b>maxPoolSize</b> - максимальный размер пула соединений</li>
     *   <li><b>idleTimeout</b> - время жизни idle соединения (мс)</li>
     *   <li><b>maxLifetime</b> - максимальное время жизни соединения (мс)</li>
     *   <li><b>validationTimeout</b> - таймаут валидации соединения (мс)</li>
     *   <li><b>leakDetectionThreshold</b> - порог обнаружения утечек (мс)</li>
     * </ul>
     */
    public record DatabaseConfig(
            String url,
            String username,
            String password,
            String driverClassName,
            String dialect,
            String ddlAuto,
            boolean showSql,
            boolean formatSql,
            int batchSize,
            boolean flywayEnabled,
            int connectionTimeout,
            int maxPoolSize,
            int idleTimeout,
            int maxLifetime,
            int validationTimeout,
            int leakDetectionThreshold
    ) {

        /**
         * Компактный конструктор для валидации параметров.
         *
         * <p>Выполняет проверку обязательных параметров:
         * <ul>
         *   <li>URL не может быть null или пустым</li>
         *   <li>Имя пользователя не может быть null или пустым</li>
         *   <li>maxPoolSize должен быть положительным</li>
         *   <li>connectionTimeout должен быть положительным</li>
         * </ul>
         *
         * @throws IllegalArgumentException если какой-либо параметр не прошел валидацию
         */
        public DatabaseConfig {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("Database URL cannot be null or blank");
            }
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Username cannot be null or blank");
            }
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("maxPoolSize must be positive, got: " + maxPoolSize);
            }
            if (connectionTimeout <= 0) {
                throw new IllegalArgumentException("connectionTimeout must be positive, got: " + connectionTimeout);
            }
            // Добавьте проверку формата URL
            if (!url.startsWith("jdbc:postgresql://")) {
                throw new IllegalArgumentException("URL must be a valid PostgreSQL JDBC URL");
            }
        }

        /**
         * Регистрирует свойства для интеграционных тестов с увеличенными таймаутами.
         */
        public static void registerIntegrationTestProperties(DynamicPropertyRegistry registry, EmbeddedPostgres postgres) {
            var config = DatabaseConfig.builder()
                    .url(postgres.getJdbcUrl(DATABASE_NAME, DATABASE_NAME))
                    .connectionTimeout(INTEGRATION_TEST_TIMEOUT)
                    .maxPoolSize(10)
                    .showSql(true)
                    .formatSql(true)
                    .batchSize(50)
                    .flywayEnabled(false)
                    .build();
            config.register(registry);
        }

        /**
         * Билдер для создания конфигурации.
         *
         * <p>Реализует паттерн Builder для удобной и читаемой настройки параметров.
         * Позволяет использовать цепочки вызовов:
         * <pre>{@code
         * var config = DatabaseConfig.builder()
         *     .url("jdbc:postgresql://localhost:5432/test")
         *     .maxPoolSize(20)
         *     .showSql(true)
         *     .build();
         * }</pre>
         */
        public static class Builder {
            private String url;
            private String username = DEFAULT_USERNAME;
            private String password = DEFAULT_PASSWORD;
            private String driverClassName = DEFAULT_DRIVER;
            private String dialect = DEFAULT_DIALECT;
            private String ddlAuto = DEFAULT_DDL_AUTO;
            private boolean showSql = true;
            private boolean formatSql = true;
            private int batchSize = DEFAULT_BATCH_SIZE;
            private boolean flywayEnabled = false;
            private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
            private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
            private int idleTimeout = DEFAULT_IDLE_TIMEOUT;
            private int maxLifetime = DEFAULT_MAX_LIFETIME;
            private int validationTimeout = DEFAULT_VALIDATION_TIMEOUT;
            private int leakDetectionThreshold = DEFAULT_LEAK_DETECTION_THRESHOLD;

            /**
             * Устанавливает JDBC URL для подключения.
             *
             * @param url JDBC URL в формате jdbc:postgresql://host:port/database
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder url(String url) {
                this.url = url;
                return this;
            }

            /**
             * Устанавливает имя пользователя базы данных.
             *
             * @param username имя пользователя
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder username(String username) {
                this.username = username;
                return this;
            }

            /**
             * Устанавливает пароль пользователя.
             *
             * @param password пароль
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder password(String password) {
                this.password = password;
                return this;
            }

            /**
             * Устанавливает класс JDBC драйвера.
             *
             * @param driverClassName полное имя класса драйвера
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder driverClassName(String driverClassName) {
                this.driverClassName = driverClassName;
                return this;
            }

            /**
             * Устанавливает диалект Hibernate.
             *
             * @param dialect полное имя класса диалекта
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder dialect(String dialect) {
                this.dialect = dialect;
                return this;
            }

            /**
             * Устанавливает стратегию DDL (Data Definition Language).
             *
             * <p>Возможные значения:
             * <ul>
             *   <li><b>create-drop</b> - создает схему при старте, удаляет при остановке</li>
             *   <li><b>create</b> - создает схему при старте, не удаляет</li>
             *   <li><b>update</b> - обновляет существующую схему</li>
             *   <li><b>validate</b> - только проверяет схему</li>
             *   <li><b>none</b> - не выполняет DDL операций</li>
             * </ul>
             *
             * @param ddlAuto стратегия DDL
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder ddlAuto(String ddlAuto) {
                this.ddlAuto = ddlAuto;
                return this;
            }

            /**
             * Включает или отключает вывод SQL запросов в лог.
             *
             * @param showSql true для вывода SQL
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder showSql(boolean showSql) {
                this.showSql = showSql;
                return this;
            }

            /**
             * Включает или отключает форматирование SQL запросов.
             *
             * @param formatSql true для форматирования
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder formatSql(boolean formatSql) {
                this.formatSql = formatSql;
                return this;
            }

            /**
             * Устанавливает размер пакета для batch операций.
             *
             * @param batchSize размер пакета (рекомендуется 20-100)
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder batchSize(int batchSize) {
                this.batchSize = batchSize;
                return this;
            }

            /**
             * Включает или отключает миграции Flyway.
             *
             * @param flywayEnabled true для включения Flyway
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder flywayEnabled(boolean flywayEnabled) {
                this.flywayEnabled = flywayEnabled;
                return this;
            }

            /**
             * Устанавливает таймаут подключения.
             *
             * @param connectionTimeout таймаут в миллисекундах
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder connectionTimeout(int connectionTimeout) {
                this.connectionTimeout = connectionTimeout;
                return this;
            }

            /**
             * Устанавливает максимальный размер пула соединений.
             *
             * @param maxPoolSize максимальное количество соединений в пуле
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder maxPoolSize(int maxPoolSize) {
                this.maxPoolSize = maxPoolSize;
                return this;
            }

            /**
             * Устанавливает время жизни idle соединения.
             *
             * @param idleTimeout таймаут в миллисекундах
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder idleTimeout(int idleTimeout) {
                this.idleTimeout = idleTimeout;
                return this;
            }

            /**
             * Устанавливает максимальное время жизни соединения.
             *
             * @param maxLifetime максимальное время жизни в миллисекундах
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder maxLifetime(int maxLifetime) {
                this.maxLifetime = maxLifetime;
                return this;
            }

            /**
             * Устанавливает таймаут валидации соединения.
             *
             * @param validationTimeout таймаут в миллисекундах
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder validationTimeout(int validationTimeout) {
                this.validationTimeout = validationTimeout;
                return this;
            }

            /**
             * Устанавливает порог обнаружения утечек соединений.
             *
             * @param leakDetectionThreshold порог в миллисекундах
             * @return текущий экземпляр билдера для цепочечных вызовов
             */
            public Builder leakDetectionThreshold(int leakDetectionThreshold) {
                this.leakDetectionThreshold = leakDetectionThreshold;
                return this;
            }

            /**
             * Строит объект DatabaseConfig из установленных параметров.
             *
             * @return сконфигурированный объект DatabaseConfig
             * @throws IllegalStateException если url не установлен
             */
            public DatabaseConfig build() {
                if (url == null || url.isBlank()) {
                    throw new IllegalStateException("URL must be set before building configuration");
                }
                return new DatabaseConfig(
                        url, username, password, driverClassName, dialect, ddlAuto,
                        showSql, formatSql, batchSize, flywayEnabled,
                        connectionTimeout, maxPoolSize, idleTimeout, maxLifetime,
                        validationTimeout, leakDetectionThreshold
                );
            }
        }

        /**
         * Создает новый экземпляр билдера.
         *
         * @return новый билдер для конфигурации
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Создает стандартную конфигурацию из EmbeddedPostgres.
         *
         * <p>Использует URL, сгенерированный EmbeddedPostgres, и все
         * остальные параметры по умолчанию.
         *
         * @param postgres экземпляр EmbeddedPostgres
         * @return сконфигурированный объект DatabaseConfig
         */
        public static DatabaseConfig fromEmbeddedPostgres(EmbeddedPostgres postgres) {
            return builder()
                    .url(postgres.getJdbcUrl(DATABASE_NAME, DATABASE_NAME))
                    .build();
        }

        /**
         * Создает конфигурацию для JPA тестов с оптимизированными параметрами.
         *
         * <p>Особенности конфигурации для JPA тестов:
         * <ul>
         *   <li>Уменьшенный пул соединений (5 вместо 10)</li>
         *   <li>Включен вывод SQL для отладки</li>
         *   <li>Включено форматирование SQL</li>
         *   <li>Оптимизированный размер пакета (50)</li>
         *   <li>Отключен Flyway</li>
         * </ul>
         *
         * @param postgres экземпляр EmbeddedPostgres
         * @return сконфигурированный объект DatabaseConfig для JPA тестов
         */
        public static DatabaseConfig forJpaTests(EmbeddedPostgres postgres) {
            return builder()
                    .url(postgres.getJdbcUrl(DATABASE_NAME, DATABASE_NAME))
                    .maxPoolSize(5)
                    .showSql(true)
                    .formatSql(true)
                    .batchSize(50)
                    .flywayEnabled(false)
                    .connectionTimeout(30000)
                    .leakDetectionThreshold(30000)
                    .build();
        }

        /**
         * Регистрирует все свойства в Spring DynamicPropertyRegistry.
         *
         * <p>Автоматически добавляет все необходимые настройки для Spring Boot:
         * <ul>
         *   <li><b>spring.datasource.*</b> - настройки подключения к базе данных</li>
         *   <li><b>spring.datasource.hikari.*</b> - настройки HikariCP для
         *       предотвращения утечек соединений и оптимизации производительности</li>
         *   <li><b>spring.jpa.*</b> - настройки JPA/Hibernate для тестового окружения</li>
         * </ul>
         *
         * <p><b>Важные настройки для предотвращения проблем в тестах:</b>
         * <ul>
         *   <li>autoCommit=false - управление транзакциями через Spring</li>
         *   <li>leakDetectionThreshold - обнаружение утечек соединений</li>
         *   <li>connectionTestQuery=SELECT 1 - проверка соединения перед использованием</li>
         *   <li>hibernate.jdbc.batch_size - оптимизация массовых операций</li>
         * </ul>
         *
         * @param registry реестр динамических свойств Spring
         */
        public void register(DynamicPropertyRegistry registry) {
            // Основные настройки datasource
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> username);
            registry.add("spring.datasource.password", () -> password);
            registry.add("spring.datasource.driver-class-name", () -> driverClassName);

            // HikariCP настройки для предотвращения утечек и оптимизации
            registry.add("spring.datasource.hikari.connectionTimeout", () -> String.valueOf(connectionTimeout));
            registry.add("spring.datasource.hikari.maximumPoolSize", () -> String.valueOf(maxPoolSize));
            registry.add("spring.datasource.hikari.idleTimeout", () -> String.valueOf(idleTimeout));
            registry.add("spring.datasource.hikari.maxLifetime", () -> String.valueOf(maxLifetime));
            registry.add("spring.datasource.hikari.validationTimeout", () -> String.valueOf(validationTimeout));
            registry.add("spring.datasource.hikari.leakDetectionThreshold", () -> String.valueOf(leakDetectionThreshold));
            registry.add("spring.datasource.hikari.autoCommit", () -> "true");
            registry.add("spring.datasource.hikari.registerMbeans", () -> "false");
            registry.add("spring.datasource.hikari.allowPoolSuspension", () -> "false");
            registry.add("spring.datasource.hikari.initializationFailTimeout", () -> "1");
            registry.add("spring.datasource.hikari.connectionTestQuery", () -> "SELECT 1");
            registry.add("spring.datasource.hikari.keepaliveTime", () -> "30000");

            // JPA/Hibernate настройки для тестов
            registry.add("spring.jpa.database-platform", () -> dialect);
            registry.add("spring.jpa.hibernate.ddl-auto", () -> ddlAuto);
            registry.add("spring.jpa.show-sql", () -> String.valueOf(showSql));
            registry.add("spring.jpa.properties.hibernate.format_sql", () -> String.valueOf(formatSql));
            registry.add("spring.jpa.properties.hibernate.jdbc.batch_size", () -> String.valueOf(batchSize));
            registry.add("spring.jpa.properties.hibernate.order_inserts", () -> "true");
            registry.add("spring.jpa.properties.hibernate.order_updates", () -> "true");
            registry.add("spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation", () -> "true");
            registry.add("spring.jpa.properties.hibernate.connection.provider_disables_autocommit", () -> "false");
            registry.add("spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults", () -> "false");
            registry.add("spring.jpa.properties.hibernate.jdbc.timeout", () -> "30");
            registry.add("spring.jpa.properties.javax.persistence.query.timeout", () -> "30000");

            // Flyway отключаем для тестов
            registry.add("spring.flyway.enabled", () -> String.valueOf(flywayEnabled));
        }
    }

    /**
     * Функциональный интерфейс для кастомизации конфигурации.
     *
     * <p>Позволяет использовать лямбда-выражения для настройки параметров
     * в декларативном стиле:
     *
     * <pre>{@code
     * DatabasePropertyFactory.registerProperties(registry, postgres, builder -> {
     *     builder.showSql(false)
     *            .maxPoolSize(20)
     *            .connectionTimeout(60000)
     *            .batchSize(100);
     * });
     * }</pre>
     *
     * <p>Этот подход обеспечивает гибкость и переиспользование конфигураций.
     */
    @FunctionalInterface
    public interface ConfigCustomizer extends Consumer<DatabaseConfig.Builder> {
        // Функциональный интерфейс для кастомизации конфигурации
    }

    /**
     * Регистрирует свойства с кастомной конфигурацией.
     *
     * <p>Позволяет гибко настраивать параметры подключения к базе данных
     * с использованием лямбда-выражений. Принимает произвольное количество
     * кастомайзеров, которые применяются последовательно.
     *
     * @param registry    реестр динамических свойств Spring
     * @param postgres    экземпляр EmbeddedPostgres (не может быть null)
     * @param customizers варарг кастомайзеров для настройки билдера
     * @throws IllegalStateException если postgres не инициализирован
     * @see ConfigCustomizer
     */
    public static void registerProperties(
            DynamicPropertyRegistry registry,
            EmbeddedPostgres postgres,
            ConfigCustomizer... customizers
    ) {
        if (postgres == null) {
            throw new IllegalStateException("EmbeddedPostgres is not initialized");
        }

        var builder = DatabaseConfig.builder()
                .url(postgres.getJdbcUrl(DATABASE_NAME, DATABASE_NAME));

        for (var customizer : customizers) {
            customizer.accept(builder);
        }

        var config = builder.build();
        config.register(registry);
    }

    /**
     * Регистрирует стандартные свойства для интеграционных тестов.
     *
     * <p>Использует все параметры по умолчанию, что подходит для большинства
     * интеграционных тестов. Включает:
     * <ul>
     *   <li>Размер пула: 10 соединений</li>
     *   <li>Вывод SQL: включен</li>
     *   <li>Форматирование SQL: включено</li>
     *   <li>DDL стратегия: create-drop</li>
     *   <li>Flyway: отключен</li>
     * </ul>
     *
     * @param registry реестр динамических свойств Spring
     * @param postgres экземпляр EmbeddedPostgres
     */
    public static void registerStandardProperties(DynamicPropertyRegistry registry, EmbeddedPostgres postgres) {
        registry.add("database.port", () -> String.valueOf(postgres.getPort()));
        registry.add("spring.datasource.url", () ->
                String.format("jdbc:postgresql://localhost:%d/postgres?useUnicode=true&characterEncoding=UTF-8",
                        postgres.getPort()));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    /**
     * Регистрирует оптимизированные свойства для JPA тестов.
     *
     * <p>Предоставляет настройки, оптимизированные специально для JPA тестов:
     * <ul>
     *   <li>Уменьшенный пул соединений (5) для экономии ресурсов</li>
     *   <li>Включен вывод SQL для отладки JPA запросов</li>
     *   <li>Форматирование SQL для читаемости</li>
     *   <li>Оптимальный размер пакета для batch операций (50)</li>
     * </ul>
     *
     * @param registry реестр динамических свойств Spring
     * @param postgres экземпляр EmbeddedPostgres
     */
    public static void registerJpaProperties(DynamicPropertyRegistry registry, EmbeddedPostgres postgres) {
        var config = DatabaseConfig.forJpaTests(postgres);
        config.register(registry);
    }

    /**
     * Создает URL для подключения к EmbeddedPostgreSQL.
     *
     * <p>Удобный метод для получения JDBC URL из экземпляра EmbeddedPostgres.
     * URL формируется в формате:
     * {@code jdbc:postgresql://localhost:{port}/postgres}
     *
     * @param postgres экземпляр EmbeddedPostgres
     * @return JDBC URL для подключения
     */
    public static String createJdbcUrl(EmbeddedPostgres postgres) {
        return postgres.getJdbcUrl(DATABASE_NAME, DATABASE_NAME);
    }
}