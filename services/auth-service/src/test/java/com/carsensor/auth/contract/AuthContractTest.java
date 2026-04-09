package com.carsensor.auth.contract;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import com.carsensor.auth.application.dto.UserDto;
import com.carsensor.auth.infrastructure.config.SecurityConfig;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.platform.dto.AuthResponse;
import com.carsensor.platform.dto.LoginRequest;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Контрактные тесты для Auth API.
 *
 * @see AbstractIntegrationTest
 * @since 1.0
 */
@Slf4j
@Import(TestRestClientConfig.class)
@DisplayName("Контрактные тесты Auth API")
@Timeout(value = 180, unit = TimeUnit.SECONDS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {com.carsensor.auth.AuthServiceApplication.class, SecurityConfig.class},
        properties = {"spring.jpa.open-in-view=false", "spring.transaction.default-timeout=90"})
class AuthContractTest extends AbstractIntegrationTest {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String TEST_USERNAME = "admin";
    private static final String TEST_PASSWORD = "admin123";
    private static final String TEST_EMAIL = "admin@carsensor.local";
    private static final String TEST_FIRST_NAME = "Admin";
    private static final String TEST_LAST_NAME = "User";

    private static final int SERVER_START_TIMEOUT_SECONDS = 60;
    private static final int RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MILLIS = 500;

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String ME_URL = "/api/v1/auth/me";
    private static final String HEALTH_URL = "/actuator/health";

    // ============================================================
    // ПОЛЯ КЛАССА
    // ============================================================

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RestClient restClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        baseUrl = super.baseUrl();  // ✅ используем метод родителя

        log.info("=== Test Configuration ===");
        log.info("Server port: {}", super.getDatabasePort());
        log.info("Base URL: {}", baseUrl);

        await()
                .atMost(Duration.ofSeconds(SERVER_START_TIMEOUT_SECONDS))
                .pollInterval(Duration.ofSeconds(1))
                .ignoreExceptions()
                .until(() -> {
                    var response = restClient.get()
                            .uri(baseUrl + HEALTH_URL)
                            .retrieve()
                            .toBodilessEntity();
                    return response.getStatusCode() == HttpStatus.OK;
                });

        log.info("✓ Server is reachable");
    }

    @SuppressWarnings("SqlNoDataSourceInspection")
    private void verifyDatabaseState() {
        try {
            var userCount = jdbcTemplate.queryForObject(
                    """
                            SELECT COUNT(*)
                            FROM users
                            WHERE username = ?
                            """,
                    Integer.class,
                    TEST_USERNAME
            );

            var actualCount = userCount != null ? userCount : 0;
            log.info("Users in database: {}", actualCount);

            if (actualCount == 0) {
                log.warn("Test user '{}' not found!", TEST_USERNAME);
            }

        } catch (Exception e) {
            log.error("Database verification failed", e);
        }
    }

    private void verifyUserDetailsService() {
        try {
            var userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);
            log.info("✓ UserDetails loaded: {}", userDetails.getUsername());
            log.info("✓ Authorities: {}", userDetails.getAuthorities());
        } catch (Exception e) {
            log.error("UserDetailsService verification failed", e);
        }
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    private ResponseEntity<AuthResponse> loginWithRetry() {
        var request = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        Exception lastException = null;

        for (int attempt = 1; attempt <= RETRY_ATTEMPTS; attempt++) {
            try {
                var response = restClient.post()
                        .uri(baseUrl + LOGIN_URL)
                        .body(request)
                        .retrieve()
                        .toEntity(AuthResponse.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    return response;
                }
            } catch (HttpClientErrorException.Unauthorized e) {
                lastException = e;
                log.debug("Login attempt {} failed with 401", attempt);
                if (attempt < RETRY_ATTEMPTS) {
                    sleep();
                }
            } catch (Exception e) {
                lastException = e;
                log.debug("Login attempt {} failed: {}", attempt, e.getMessage());
                if (attempt < RETRY_ATTEMPTS) {
                    sleep();
                }
            }
        }

        throw new RuntimeException("Failed to login after " + RETRY_ATTEMPTS + " attempts", lastException);
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", e);
        }
    }

    // ============================================================
    // POST /api/v1/auth/login
    // ============================================================

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("✅ Успешная аутентификация с валидными credentials")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void login_WithValidCredentials_ShouldReturnTokens() {
            var request = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

            var response = restClient.post()
                    .uri(baseUrl + LOGIN_URL)
                    .body(request)
                    .retrieve()
                    .toEntity(AuthResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            var authResponse = response.getBody();
            assertThat(authResponse.accessToken()).isNotBlank();
            assertThat(authResponse.refreshToken()).isNotBlank();
            assertThat(authResponse.tokenType()).isEqualTo("Bearer");
            assertThat(authResponse.username()).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("❌ Неверный пароль возвращает 401")
        void login_WithInvalidPassword_ShouldReturnUnauthorized() {
            var request = new LoginRequest(TEST_USERNAME, "wrongpassword");

            assertThatThrownBy(() -> restClient.post()
                    .uri(baseUrl + LOGIN_URL)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        }

        @Test
        @DisplayName("❌ Несуществующий пользователь возвращает 401")
        void login_WithNonExistentUser_ShouldReturnUnauthorized() {
            var request = new LoginRequest("nonexistent", "password");

            assertThatThrownBy(() -> restClient.post()
                    .uri(baseUrl + LOGIN_URL)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        }

        @Test
        @DisplayName("❌ Пустой username возвращает 400")
        void login_WithEmptyUsername_ShouldReturnBadRequest() {
            var request = new LoginRequest("", "password123");

            assertThatThrownBy(() -> restClient.post()
                    .uri(baseUrl + LOGIN_URL)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.BadRequest.class);
        }

        @Test
        @DisplayName("❌ Пустой password возвращает 400")
        void login_WithEmptyPassword_ShouldReturnBadRequest() {
            var request = new LoginRequest(TEST_USERNAME, "");

            assertThatThrownBy(() -> restClient.post()
                    .uri(baseUrl + LOGIN_URL)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.BadRequest.class);
        }
    }

    // ============================================================
    // POST /api/v1/auth/refresh
    // ============================================================

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("✅ Обновление токена с валидным refresh token")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void refresh_WithValidRefreshToken_ShouldReturnNewAccessToken() {
            var loginResponse = loginWithRetry();
            assertThat(loginResponse.getBody()).isNotNull();

            var refreshToken = loginResponse.getBody().refreshToken();
            assertThat(refreshToken).isNotBlank();

            var refreshResponse = restClient.post()
                    .uri(baseUrl + REFRESH_URL)
                    .header("Authorization", "Bearer " + refreshToken)
                    .retrieve()
                    .toEntity(AuthResponse.class);

            assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(refreshResponse.getBody()).isNotNull();
            assertThat(refreshResponse.getBody().accessToken()).isNotBlank();
            assertThat(refreshResponse.getBody().refreshToken()).isEqualTo(refreshToken);
            assertThat(refreshResponse.getBody().username()).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("❌ Обновление без токена возвращает 401")
        void refresh_WithoutToken_ShouldReturnUnauthorized() {
            assertThatThrownBy(() -> restClient.post()
                    .uri(baseUrl + REFRESH_URL)
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        }

        @Test
        @DisplayName("❌ Обновление с невалидным токеном возвращает 401")
        void refresh_WithInvalidToken_ShouldReturnUnauthorized() {
            assertThatThrownBy(() -> restClient.post()
                    .uri(baseUrl + REFRESH_URL)
                    .header("Authorization", "Bearer invalid.token.here")
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        }

        @Test
        @DisplayName("❌ Обновление с неправильным форматом header возвращает 401")
        void refresh_WithInvalidHeaderFormat_ShouldReturnUnauthorized() {
            assertThatThrownBy(() -> restClient.post()
                    .uri(baseUrl + REFRESH_URL)
                    .header("Authorization", "InvalidFormat token")
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        }
    }

    // ============================================================
    // POST /api/v1/auth/logout
    // ============================================================

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("✅ Выход из системы с валидным токеном возвращает 200")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void logout_WithValidToken_ShouldReturnOk() {
            var loginResponse = loginWithRetry();
            assertThat(loginResponse.getBody()).isNotNull();

            var accessToken = loginResponse.getBody().accessToken();
            assertThat(accessToken).isNotBlank();

            var response = restClient.post()
                    .uri(baseUrl + LOGOUT_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("❌ Выход без токена возвращает 401")
        void logout_WithoutToken_ShouldReturnUnauthorized() {
            assertThatThrownBy(() -> restClient.post()
                    .uri(baseUrl + LOGOUT_URL)
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        }

        @Test
        @DisplayName("❌ Выход с невалидным токеном возвращает 401")
        void logout_WithInvalidToken_ShouldReturnUnauthorized() {
            assertThatThrownBy(() -> restClient.post()
                    .uri(baseUrl + LOGOUT_URL)
                    .header("Authorization", "Bearer invalid.token")
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        }
    }

    // ============================================================
    // GET /api/v1/auth/me
    // ============================================================

    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("✅ Получение информации о пользователе с валидным токеном")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void getCurrentUser_WithValidToken_ShouldReturnUserInfo() {
            var loginResponse = loginWithRetry();
            assertThat(loginResponse.getBody()).isNotNull();

            var accessToken = loginResponse.getBody().accessToken();
            assertThat(accessToken).isNotBlank();

            var response = restClient.get()
                    .uri(baseUrl + ME_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .toEntity(UserDto.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            var userDto = response.getBody();
            assertThat(userDto.username()).isEqualTo(TEST_USERNAME);
            assertThat(userDto.email()).isEqualTo(TEST_EMAIL);
            assertThat(userDto.firstName()).isEqualTo(TEST_FIRST_NAME);
            assertThat(userDto.lastName()).isEqualTo(TEST_LAST_NAME);
            assertThat(userDto.isActive()).isTrue();
            assertThat(userDto.roles()).contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("❌ Получение пользователя без токена возвращает 401")
        void getCurrentUser_WithoutToken_ShouldReturnUnauthorized() {
            assertThatThrownBy(() -> restClient.get()
                    .uri(baseUrl + ME_URL)
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        }

        @Test
        @DisplayName("❌ Получение пользователя с невалидным токеном возвращает 401")
        void getCurrentUser_WithInvalidToken_ShouldReturnUnauthorized() {
            assertThatThrownBy(() -> restClient.get()
                    .uri(baseUrl + ME_URL)
                    .header("Authorization", "Bearer invalid.token")
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        }
    }

    // ============================================================
    // POST /api/v1/auth/validate
    // ============================================================

    @Nested
    @DisplayName("POST /api/v1/auth/validate")
    class ValidateTokenTests {

        private static final String VALIDATE_URL = "/api/v1/auth/validate";

        @Test
        @DisplayName("✅ Проверка валидного токена возвращает true")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void validate_WithValidToken_ShouldReturnTrue() {
            var loginResponse = loginWithRetry();
            assertThat(loginResponse.getBody()).isNotNull();

            var accessToken = loginResponse.getBody().accessToken();
            assertThat(accessToken).isNotBlank();

            var response = restClient.post()
                    .uri(baseUrl + VALIDATE_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .toEntity(Boolean.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isTrue();
        }

        @Test
        @DisplayName("❌ Проверка невалидного токена возвращает false")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void validate_WithInvalidToken_ShouldReturnFalse() {
            var response = restClient.post()
                    .uri(baseUrl + VALIDATE_URL)
                    .header("Authorization", "Bearer invalid.token")
                    .retrieve()
                    .toEntity(Boolean.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isFalse();
        }

        @Test
        @DisplayName("❌ Проверка без токена возвращает false")
        void validate_WithoutToken_ShouldReturnFalse() {
            var response = restClient.post()
                    .uri(baseUrl + VALIDATE_URL)
                    .retrieve()
                    .toEntity(Boolean.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isFalse();
        }
    }
}