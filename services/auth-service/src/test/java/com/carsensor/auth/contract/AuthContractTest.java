package com.carsensor.auth.contract;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.platform.dto.AuthResponse;
import com.carsensor.platform.dto.LoginRequest;
import com.carsensor.platform.dto.UserDto;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Контрактные тесты для API auth-service.
 * Проверяют соответствие API спецификации с использованием Embedded PostgreSQL.
 *
 * @see AbstractIntegrationTest
 * @since 1.0
 */
@Slf4j
@Import(TestRestClientConfig.class)
@DisplayName("Контрактные тесты Auth API")
@Timeout(value = 180, unit = TimeUnit.SECONDS)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {com.carsensor.auth.AuthServiceApplication.class,
                com.carsensor.auth.infrastructure.security.SecurityConfig.class},
        properties = {"spring.jpa.open-in-view=false", "spring.transaction.default-timeout=90"})
class AuthContractTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RestClient restClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;

        var message = """
                === Test Configuration ===
                Server port: %d
                Base URL: %s
                ==========================
                """.formatted(port, baseUrl);
        System.out.println(message);

        // Проверка, что сервер доступен
        try {
            restClient.get()
                    .uri(baseUrl + "/actuator/health")
                    .retrieve()
                    .toBodilessEntity();
            System.out.println("✓ Server is reachable");
        } catch (Exception e) {
            System.err.println("✗ Server is NOT reachable: " + e.getMessage());
        }

        // ДИАГНОСТИКА: Проверяем, что тестовые данные созданы
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = 'admin'",
                    Integer.class
            );
            log.info("=== DIAGNOSTIC: Users in DB before test: {} ===", count);

            if (count == 0) {
                log.warn("!!! Test user 'admin' not found! @Sql may not be executing. !!!");
                // Принудительная вставка для диагностики
                jdbcTemplate.execute("""
                    INSERT INTO users (username, email, password, first_name, last_name, is_active, created_at, updated_at, version) 
                    VALUES ('admin', 'admin@test.com', '$2a$12$39aOe7AKc.qVz8zmHvfv9elb0n/h/AOuis6lvfuuMW1Fi6csFsAX.', 'Admin', 'User', true, NOW(), NOW(), 0)
                    ON CONFLICT (username) DO NOTHING
                    """);
                jdbcTemplate.execute("""
                    INSERT INTO user_roles (user_id, role_id) 
                    SELECT u.id, r.id FROM users u, roles r 
                    WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
                    ON CONFLICT DO NOTHING
                    """);
                log.info("Test user inserted via diagnostic fallback");
            }
        } catch (Exception e) {
            log.error("Diagnostic failed: {}", e.getMessage());
        }
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    @Sql(scripts = {"/db/test/insert-test-user.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("POST /api/v1/auth/login - успешная аутентификация")
    void login_WithValidCredentials_ShouldReturnTokens() {
        // ========== ПОЛНАЯ ДИАГНОСТИКА ==========
        System.out.println("=== DIAGNOSTICS START ===");

        // 1. Проверка пользователя
        var checkSql = """
                SELECT username, password, is_active
                FROM users
                WHERE username = 'admin'
                """;

        try {
            var user = jdbcTemplate.queryForMap(checkSql);

            System.out.println("1. User found: " + user.get("username"));
            System.out.println("2. Password hash: " + user.get("password"));
            System.out.println("3. Is active: " + user.get("is_active"));

            // ========== EXPLAIN ANALYZE ==========
            var explainSql = """
                    EXPLAIN (ANALYZE, BUFFERS)
                    SELECT * FROM users WHERE username = 'admin'
                    """;
            var explain = jdbcTemplate.queryForList(explainSql);
            System.out.println("=== EXPLAIN PLAN ===");
            for (var row : explain) {
                System.out.println(row);
            }
            System.out.println("=== END EXPLAIN ===");
            // ========== КОНЕЦ EXPLAIN ==========

        } catch (Exception e) {
            System.err.println("ERROR: User NOT found in DB!");
            var allUsers = jdbcTemplate.queryForList("SELECT username FROM users");
            System.out.println("All users in DB: " + allUsers);
        }

        // 2. Проверка пароля с BCryptEncoder
        var encoder = new BCryptPasswordEncoder(12);
        boolean matches = encoder.matches("admin123", "$2a$12$39aOe7AKc.qVz8zmHvfv9elb0n/h/AOuis6lvfuuMW1Fi6csFsAX.");
        System.out.println("4. Password 'admin123' matches directly: " + matches);

        // 3. Проверка, что ROLE_ADMIN существует в таблице roles
        var checkRoleExistsSql = """
                SELECT COUNT(*)
                FROM roles
                WHERE name = 'ROLE_ADMIN'
                """;
        Integer adminRoleCount = jdbcTemplate.queryForObject(checkRoleExistsSql, Integer.class);
        System.out.println("5. ROLE_ADMIN exists in roles table: " + (adminRoleCount != null && adminRoleCount > 0));

        // 4. Проверка связи user_roles
        var checkRoleSql = """
                SELECT COUNT(*)
                FROM user_roles ur
                JOIN users u ON ur.user_id = u.id
                WHERE u.username = 'admin'
                """;
        Integer roleCount = jdbcTemplate.queryForObject(checkRoleSql, Integer.class);
        System.out.println("6. User role assignments count: " + (roleCount != null ? roleCount : 0));

        // 5. Проверка ролей пользователя
        var roleSql = """
                SELECT r.name
                FROM roles r
                JOIN user_roles ur ON r.id = ur.role_id
                JOIN users u ON ur.user_id = u.id
                WHERE u.username = 'admin'
                """;
        var roles = jdbcTemplate.queryForList(roleSql, String.class);
        System.out.println("7. User roles: " + roles);

        System.out.println("=== DIAGNOSTICS END ===\n");
        // ========== КОНЕЦ ДИАГНОСТИКИ ==========

        // ========== ПРОВЕРКА USERDETAILSSERVICE ==========
        System.out.println("=== TESTING UserDetailsService DIRECTLY ===");
        try {
            // Используем внедренный бин, а не создаем новый
            var userDetails = userDetailsService.loadUserByUsername("admin");
            System.out.println("✓ UserDetails loaded: " + userDetails.getUsername());
            System.out.println("✓ Authorities: " + userDetails.getAuthorities());
            System.out.println("✓ Password hash length: " +
                    (userDetails.getPassword() != null ? userDetails.getPassword().length() : 0));
        } catch (Exception e) {
            System.err.println("✗ UserDetailsService failed: " + e.getMessage());
            log.error("UserDetailsService failed", e);
        }
        System.out.println("=== END UserDetailsService TEST ===\n");
        // ========== КОНЕЦ ПРОВЕРКИ ==========

        var sql = """
                SELECT COUNT(*)
                FROM users
                WHERE username = 'admin'
                """;

        var userCount = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(userCount)
                .as("Пользователь admin должен существовать в базе данных")
                .isEqualTo(1);

        var request = new LoginRequest("admin", "admin123");

        // Retry логика с получением ResponseEntity
        ResponseEntity<AuthResponse> responseEntity = null;
        Exception lastException = null;

        for (int i = 0; i < 3; i++) {
            try {
                System.out.println("=== Attempt " + (i + 1) + " to login ===");
                responseEntity = restClient.post()
                        .uri(baseUrl + "/api/v1/auth/login")
                        .body(request)
                        .retrieve()
                        .toEntity(AuthResponse.class);

                if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                    System.out.println("=== Login successful on attempt " + (i + 1) + " ===");
                    break;
                }
            } catch (HttpClientErrorException.Unauthorized e) {
                lastException = e;
                System.err.println("=== Attempt " + (i + 1) + " failed with 401 Unauthorized ===");
                if (i == 2) {
                    throw e;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            } catch (Exception e) {
                lastException = e;
                System.err.println("=== Attempt " + (i + 1) + " failed: " + e.getMessage() + " ===");
                if (i == 2) {
                    throw e;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        assertThat(responseEntity)
                .as("ResponseEntity не должен быть null, последняя ошибка: " +
                        (lastException != null ? lastException.getMessage() : "unknown"))
                .isNotNull();
        assertThat(responseEntity.getStatusCode())
                .as("Статус ответа должен быть 200 OK")
                .isEqualTo(HttpStatus.OK);

        var authResponse = responseEntity.getBody();
        assertThat(authResponse)
                .as("Тело ответа не должно быть null")
                .isNotNull();

        assertThat(authResponse.accessToken())
                .as("Access token не должен быть пустым")
                .isNotBlank();

        assertThat(authResponse.refreshToken())
                .as("Refresh token не должен быть пустым")
                .isNotBlank();

        assertThat(authResponse.tokenType())
                .as("Тип токена должен быть Bearer")
                .isEqualTo("Bearer");

        assertThat(authResponse.username())
                .as("Имя пользователя должно быть admin")
                .isEqualTo("admin");
    }

    @Test
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    @Sql(scripts = {"/db/test/insert-test-user.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("POST /api/v1/auth/login - неверный пароль")
    void login_WithInvalidPassword_ShouldReturnUnauthorized() {
        var request = new LoginRequest("admin", "wrongpassword");

        assertThatThrownBy(() -> restClient.post()
                .uri(baseUrl + "/api/v1/auth/login")
                .body(request)
                .retrieve()
                .toBodilessEntity())
                .as("Должно быть выброшено исключение Unauthorized")
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    @Sql(scripts = {"/db/test/insert-test-user.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("POST /api/v1/auth/login - несуществующий пользователь")
    void login_WithNonExistentUser_ShouldReturnUnauthorized() {
        var request = new LoginRequest("nonexistent", "password");

        assertThatThrownBy(() -> restClient.post()
                .uri(baseUrl + "/api/v1/auth/login")
                .body(request)
                .retrieve()
                .toBodilessEntity())
                .as("Должно быть выброшено исключение Unauthorized")
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("POST /api/v1/auth/login - пустой username")
    void login_WithEmptyUsername_ShouldReturnBadRequest() {
        var request = new LoginRequest("", "password123");

        assertThatThrownBy(() -> restClient.post()
                .uri(baseUrl + "/api/v1/auth/login")
                .body(request)
                .retrieve()
                .toBodilessEntity())
                .as("Должно быть выброшено исключение BadRequest")
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    @Sql(scripts = {"/db/test/insert-test-user.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("POST /api/v1/auth/refresh - обновление токена")
    void refresh_WithValidRefreshToken_ShouldReturnNewAccessToken() {
        var loginRequest = new LoginRequest("admin", "admin123");

        // Retry логика для логина
        ResponseEntity<AuthResponse> loginResponseEntity = null;
        for (int i = 0; i < 3; i++) {
            try {
                loginResponseEntity = restClient.post()
                        .uri(baseUrl + "/api/v1/auth/login")
                        .body(loginRequest)
                        .retrieve()
                        .toEntity(AuthResponse.class);

                if (loginResponseEntity.getStatusCode() == HttpStatus.OK && loginResponseEntity.getBody() != null) {
                    break;
                }
            } catch (Exception e) {
                if (i == 2) throw e;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        // Используем результат retry логики, а не новый запрос
        assertThat(loginResponseEntity).isNotNull();
        assertThat(loginResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponseEntity.getBody()).isNotNull();

        var refreshToken = loginResponseEntity.getBody().refreshToken();
        assertThat(refreshToken).isNotBlank();

        var refreshResponse = restClient.post()
                .uri(baseUrl + "/api/v1/auth/refresh")
                .header("Authorization", "Bearer " + refreshToken)
                .retrieve()
                .toEntity(AuthResponse.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        assertThat(refreshResponse.getBody().accessToken()).isNotBlank();
        assertThat(refreshResponse.getBody().refreshToken()).isEqualTo(refreshToken);
        assertThat(refreshResponse.getBody().username()).isEqualTo("admin");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("POST /api/v1/auth/refresh - без токена")
    void refresh_WithoutToken_ShouldReturnUnauthorized() {
        assertThatThrownBy(() -> restClient.post()
                .uri(baseUrl + "/api/v1/auth/refresh")
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("POST /api/v1/auth/refresh - невалидный токен")
    void refresh_WithInvalidToken_ShouldReturnUnauthorized() {
        assertThatThrownBy(() -> restClient.post()
                .uri(baseUrl + "/api/v1/auth/refresh")
                .header("Authorization", "Bearer invalid.token.here")
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    @Sql(scripts = {"/db/test/insert-test-user.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("GET /api/v1/auth/me - получение информации о пользователе")
    void getCurrentUser_WithValidToken_ShouldReturnUserInfo() {
        long startTime = System.currentTimeMillis();
        System.out.println("=== Starting getCurrentUser test at: " + new java.util.Date() + " ===");

        var loginRequest = new LoginRequest("admin", "admin123");

        // Retry логика для логина
        ResponseEntity<AuthResponse> loginResponseEntity = null;
        for (int i = 0; i < 3; i++) {
            try {
                loginResponseEntity = restClient.post()
                        .uri(baseUrl + "/api/v1/auth/login")
                        .body(loginRequest)
                        .retrieve()
                        .toEntity(AuthResponse.class);

                if (loginResponseEntity.getStatusCode() == HttpStatus.OK && loginResponseEntity.getBody() != null) {
                    break;
                }
            } catch (Exception e) {
                if (i == 2) throw e;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        assertThat(loginResponseEntity).isNotNull();
        assertThat(loginResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        var loginAuthResponse = loginResponseEntity.getBody();
        assertThat(loginAuthResponse).isNotNull();

        var accessToken = loginAuthResponse.accessToken();
        assertThat(accessToken).isNotBlank();

        // Получаем информацию о пользователе
        var response = restClient.get()
                .uri(baseUrl + "/api/v1/auth/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toEntity(UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        var userDto = response.getBody();
        assertThat(userDto.username()).isEqualTo("admin");
        assertThat(userDto.email()).isEqualTo("admin@test.com");
        assertThat(userDto.firstName()).isEqualTo("Admin");
        assertThat(userDto.lastName()).isEqualTo("User");
        assertThat(userDto.roles()).contains("ROLE_ADMIN");
        assertThat(userDto.isActive()).isTrue();

        long endTime = System.currentTimeMillis();
        System.out.println("=== Test completed in: " + (endTime - startTime) + " ms ===");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("GET /api/v1/auth/me - без токена")
    void getCurrentUser_WithoutToken_ShouldReturnUnauthorized() {
        assertThatThrownBy(() -> restClient.get()
                .uri(baseUrl + "/api/v1/auth/me")
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    @Sql(scripts = {"/db/test/insert-test-user.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("POST /api/v1/auth/logout - выход из системы")
    void logout_WithValidToken_ShouldReturnOk() {
        var loginRequest = new LoginRequest("admin", "admin123");

        // Retry логика для логина
        ResponseEntity<AuthResponse> loginResponseEntity = null;
        for (int i = 0; i < 3; i++) {
            try {
                loginResponseEntity = restClient.post()
                        .uri(baseUrl + "/api/v1/auth/login")
                        .body(loginRequest)
                        .retrieve()
                        .toEntity(AuthResponse.class);

                if (loginResponseEntity.getStatusCode() == HttpStatus.OK && loginResponseEntity.getBody() != null) {
                    break;
                }
            } catch (Exception e) {
                if (i == 2) throw e;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        // Используем результат retry логики, а не новый запрос
        assertThat(loginResponseEntity).isNotNull();
        assertThat(loginResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponseEntity.getBody()).isNotNull();

        var accessToken = loginResponseEntity.getBody().accessToken();
        assertThat(accessToken).isNotBlank();

        var response = restClient.post()
                .uri(baseUrl + "/api/v1/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("POST /api/v1/auth/logout - без токена")
    void logout_WithoutToken_ShouldReturnUnauthorized() {
        assertThatThrownBy(() -> restClient.post()
                .uri(baseUrl + "/api/v1/auth/logout")
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }
}