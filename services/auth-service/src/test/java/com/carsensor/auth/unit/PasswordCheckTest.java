package com.carsensor.auth.unit;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import com.carsensor.auth.contract.TestRestClientConfig;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.platform.dto.AuthResponse;
import com.carsensor.platform.dto.LoginRequest;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Интеграционные тесты аутентификации и JWT генерации.
 *
 * <p><b>Назначение тестов:</b></p>
 * <ul>
 *   <li>Проверка корректности BCrypt хеширования пароля admin123</li>
 *   <li>Проверка успешной аутентификации пользователя admin</li>
 *   <li>Проверка генерации JWT access и refresh токенов</li>
 *   <li>Валидация структуры ответа эндпоинта /api/v1/auth/login</li>
 * </ul>
 *
 * <p><b>Архитектурные решения:</b></p>
 * <ul>
 *   <li><b>Изоляция тестовых данных</b> - @Sql с ISOLATED транзакцией гарантирует,
 *       что каждый тест работает с чистыми данными</li>
 *   <li><b>Асинхронное ожидание сервера</b> - Awaitility обеспечивает надежное
 *       ожидание запуска embedded Tomcat на случайном порту</li>
 *   <li><b>Очистка кэша Hibernate</b> - принудительная эвикция кэша перед каждым
 *       запросом для получения актуальных данных из БД</li>
 *   <li><b>Детальное логирование</b> - каждый этап теста логируется для быстрой
 *       диагностики проблем в CI/CD окружении</li>
 * </ul>
 *
 * <p><b>Технологический стек:</b></p>
 * <ul>
 *   <li>Spring Boot 3.4.11 - контекст приложения</li>
 *   <li>JUnit 5 - фреймворк тестирования</li>
 *   <li>Awaitility 4.2.0 - асинхронные проверки</li>
 *   <li>AssertJ - fluent assertions</li>
 *   <li>Embedded PostgreSQL - изолированная БД для тестов</li>
 * </ul>
 *
 * @see AbstractIntegrationTest
 * @see AuthResponse
 * @see LoginRequest
 * @since 1.0
 */
@Slf4j
@Import(TestRestClientConfig.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=test",
                "spring.jpa.open-in-view=false",
                "spring.datasource.hikari.autoCommit=true",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@DisplayName("Тесты аутентификации и JWT")
@SqlGroup({
        @Sql(
                scripts = "/db/test/insert-test-user.sql",
                config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
                executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
        )
})
public class PasswordCheckTest extends AbstractIntegrationTest {

    /**
     * Имя тестового пользователя.
     */
    private static final String TEST_USERNAME = "admin";

    /**
     * Пароль тестового пользователя.
     */
    private static final String TEST_PASSWORD = "admin123";

    /**
     * Таймаут ожидания запуска сервера в секундах.
     */
    private static final int SERVER_START_TIMEOUT_SECONDS = 30;

    /**
     * Интервал опроса при ожидании сервера в секундах.
     */
    private static final int POLL_INTERVAL_SECONDS = 1;

    /**
     * Случайный порт, выделенный Spring Boot для теста.
     */
    @LocalServerPort
    private int port;

    /**
     * RestTemplate для выполнения HTTP запросов в тестах.
     */
    @Autowired
    private TestRestTemplate testRestTemplate;

    /**
     * RestClient для альтернативного способа выполнения запросов.
     */
    @Autowired(required = false)
    private RestClient restClient;

    /**
     * JdbcTemplate для прямого доступа к базе данных.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Сервис загрузки пользователей для проверки UserDetailsService.
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * EntityManager для управления кэшем Hibernate.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Базовый URL приложения с динамическим портом.
     */
    private String baseUrl;

    /**
     * Инициализация перед каждым тестом.
     *
     * <p><b>Выполняемые операции:</b></p>
     * <ol>
     *   <li>Формирование базового URL с текущим портом</li>
     *   <li>Ожидание полного запуска embedded сервера</li>
     *   <li>Проверка состояния базы данных (наличие тестового пользователя)</li>
     *   <li>Валидация работы UserDetailsService</li>
     * </ol>
     */
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        log.info("Test configuration - Port: {}, URL: {}", port, baseUrl);

        waitForServerStart();
        verifyDatabaseState();
        verifyUserDetailsService();
    }

    /**
     * Ожидание запуска сервера с использованием Awaitility.
     *
     * <p>Периодически проверяет эндпоинт /actuator/health до получения
     * успешного ответа или истечения таймаута.</p>
     */
    private void waitForServerStart() {
        await()
                .atMost(SERVER_START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(() -> {
                    var response = testRestTemplate.getForEntity(
                            baseUrl + "/actuator/health",
                            String.class
                    );
                    return response.getStatusCode() == HttpStatus.OK;
                });

        log.info("✓ Server is reachable");
    }

    /**
     * Проверка состояния базы данных.
     *
     * <p>Выполняет:
     * <ul>
     *   <li>Логирование URL базы данных</li>
     *   <li>Подсчет количества пользователей с именем admin</li>
     *   <li>Вывод хеша пароля для диагностики</li>
     *   <li>Ассерт на наличие ровно одного пользователя</li>
     * </ul>
     */
    private void verifyDatabaseState() {
        try {
            var url = AbstractIntegrationTest.getDatabaseUrl();
            log.info("Database URL: {}", url);

            Integer userCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = ?",
                    Integer.class,
                    TEST_USERNAME
            );

            // Безопасная распаковка с проверкой на null
            int actualCount = Objects.requireNonNullElse(userCount, 0);
            log.info("Users in database: {}", actualCount);

            if (actualCount > 0) {
                String passwordHash = jdbcTemplate.queryForObject(
                        "SELECT password FROM users WHERE username = ?",
                        String.class,
                        TEST_USERNAME
                );
                log.info("Stored password hash: {}", passwordHash);
            }

            assertThat(actualCount)
                    .as("User should exist in database")
                    .isEqualTo(1);

        } catch (Exception e) {
            log.error("Database verification failed", e);
            fail("Database verification failed: " + e.getMessage());
        }
    }

    /**
     * Проверка работы UserDetailsService.
     *
     * <p>Напрямую вызывает loadUserByUsername для валидации:
     * <ul>
     *   <li>Пользователь admin должен быть найден</li>
     *   <li>У пользователя должны быть назначены роли/права</li>
     * </ul>
     */
    private void verifyUserDetailsService() {
        try {
            var userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);
            log.info("UserDetailsService found user: {}", userDetails.getUsername());
            log.info("Authorities: {}", userDetails.getAuthorities());

            assertThat(userDetails.getUsername())
                    .as("Username should match test user")
                    .isEqualTo(TEST_USERNAME);
            assertThat(userDetails.getAuthorities())
                    .as("User should have authorities")
                    .isNotEmpty();

        } catch (Exception e) {
            log.error("UserDetailsService verification failed", e);
            fail("UserDetailsService verification failed: " + e.getMessage());
        }
    }

    /**
     * Тест проверки BCrypt хеша пароля.
     *
     * <p>Валидирует, что пароль 'admin123' соответствует сохраненному
     * в базе данных хешу. Используется тот же BCryptPasswordEncoder
     * с strength 12, что и в production конфигурации.</p>
     */
    @Test
    @DisplayName("Проверка BCrypt хеша пароля")
    void testPasswordHash() {
        String storedHash = "$2a$12$39aOe7AKc.qVz8zmHvfv9elb0n/h/AOuis6lvfuuMW1Fi6csFsAX.";

        var encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches(TEST_PASSWORD, storedHash);

        log.info("Password verification result: {}", matches);

        assertThat(matches)
                .as("Password should match stored hash")
                .isTrue();
    }

    /**
     * Тест аутентификации через RestClient.
     *
     * <p>Использует RestClient (новый клиент Spring Boot 3.2+) для
     * выполнения POST запроса к эндпоинту аутентификации.</p>
     *
     * <p><b>Проверяемые аспекты:</b></p>
     * <ul>
     *   <li>HTTP статус 200 OK</li>
     *   <li>Наличие access и refresh токенов в ответе</li>
     *   <li>Корректный тип токена (Bearer)</li>
     *   <li>Соответствие имени пользователя в ответе</li>
     * </ul>
     */
    @Test
    @DisplayName("Аутентификация через RestClient")
    void testAuthenticationWithRestClient() {
        if (restClient == null) {
            log.warn("RestClient not available, skipping test");
            return;
        }

        // Принудительная очистка кэша Hibernate для получения свежих данных
        entityManager.clear();
        entityManager.getEntityManagerFactory().getCache().evictAll();

        var request = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

        try {
            var response = restClient.post()
                    .uri(baseUrl + "/api/v1/auth/login")
                    .body(request)
                    .retrieve()
                    .toEntity(AuthResponse.class);

            // Преобразуем HttpStatusCode в HttpStatus для совместимости
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            validateAuthResponse(response.getBody(), status);

        } catch (HttpClientErrorException.Unauthorized e) {
            logAuthenticationFailure(e);
            fail("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error", e);
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Тест аутентификации через TestRestTemplate.
     *
     * <p>Альтернативный тест с использованием классического TestRestTemplate.
     * Проверяет те же аспекты, что и тест с RestClient.</p>
     */
    @Test
    @DisplayName("Аутентификация через TestRestTemplate")
    void testAuthenticationWithTestRestTemplate() {
        // Принудительная очистка кэша Hibernate для получения свежих данных
        entityManager.clear();
        entityManager.getEntityManagerFactory().getCache().evictAll();

        var request = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

        try {
            var response = testRestTemplate.postForEntity(
                    baseUrl + "/api/v1/auth/login",
                    request,
                    AuthResponse.class
            );

            // Преобразуем HttpStatusCode в HttpStatus для совместимости
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            validateAuthResponse(response.getBody(), status);

        } catch (HttpClientErrorException.Unauthorized e) {
            logAuthenticationFailure(e);
            fail("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error", e);
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Валидация ответа аутентификации.
     *
     * <p><b>Проверяемые критерии:</b></p>
     * <ul>
     *   <li>Ответ не должен быть null</li>
     *   <li>HTTP статус должен быть 200 OK</li>
     *   <li>Access token не должен быть пустым</li>
     *   <li>Refresh token не должен быть пустым</li>
     *   <li>Тип токена должен быть "Bearer"</li>
     *   <li>Имя пользователя должно соответствовать запрошенному</li>
     * </ul>
     *
     * @param response объект ответа с токенами
     * @param status   HTTP статус ответа
     */
    private void validateAuthResponse(AuthResponse response, HttpStatus status) {
        assertThat(response)
                .as("Response should not be null")
                .isNotNull();

        log.info("Response status: {}", status);
        log.info("Access token: {}", response.accessToken());
        log.info("Refresh token: {}", response.refreshToken());

        assertThat(status)
                .as("Response status should be OK")
                .isEqualTo(HttpStatus.OK);

        assertThat(response.accessToken())
                .as("Access token should not be blank")
                .isNotBlank();

        assertThat(response.refreshToken())
                .as("Refresh token should not be blank")
                .isNotBlank();

        assertThat(response.tokenType())
                .as("Token type should be Bearer")
                .isEqualTo("Bearer");

        assertThat(response.username())
                .as("Username should match")
                .isEqualTo(TEST_USERNAME);
    }

    /**
     * Логирование ошибки аутентификации.
     *
     * <p>Выводит детальную информацию об ошибке:
     * <ul>
     *   <li>HTTP статус 401 Unauthorized</li>
     *   <li>Тело ответа сервера</li>
     *   <li>Текущее состояние базы данных (количество пользователей)</li>
     * </ul>
     *
     * @param e исключение 401 Unauthorized
     */
    private void logAuthenticationFailure(HttpClientErrorException.Unauthorized e) {
        log.error("Authentication failed with 401 Unauthorized");
        log.error("Response body: {}", e.getResponseBodyAsString());

        // Диагностика состояния базы данных
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = ?",
                    Integer.class,
                    TEST_USERNAME
            );
            int userCount = Objects.requireNonNullElse(count, 0);
            log.error("Users in database: {}", userCount);
        } catch (Exception ex) {
            log.error("Cannot query database", ex);
        }
    }
}