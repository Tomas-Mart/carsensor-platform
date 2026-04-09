package com.carsensor.auth.integration.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import com.carsensor.auth.AuthServiceApplication;
import com.carsensor.auth.infrastructure.security.JwtTokenProvider;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.platform.dto.AuthResponse;
import com.carsensor.platform.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для AuthController.
 *
 * @see AbstractIntegrationTest
 * @since 1.0
 */
@Slf4j
@AutoConfigureMockMvc
@DisplayName("Интеграционные тесты AuthController")
@SpringBootTest(classes = AuthServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String ME_URL = "/api/v1/auth/me";
    private static final String VALIDATE_URL = "/api/v1/auth/validate";

    private static final String TEST_USERNAME = "admin";
    private static final String TEST_PASSWORD = "admin123";
    private static final String TEST_EMAIL = "admin@carsensor.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider tokenProvider;

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    private ResultActions performLogin(LoginRequest request) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private String obtainAccessToken() throws Exception {
        var loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        var loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        return loginResponse.accessToken();
    }

    private String obtainRefreshToken() throws Exception {
        var loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        var loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        return loginResponse.refreshToken();
    }

    // ============================================================
    // POST /api/v1/auth/login
    // ============================================================
    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @Commit
        @DisplayName("✅ Успешный вход с admin:admin123")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void should_ReturnTokens_When_CredentialsAreValid() throws Exception {
            var request = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

            var result = performLogin(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.roles").isArray())
                    .andReturn();

            var response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    AuthResponse.class
            );

            assertThat(tokenProvider.validateToken(response.accessToken())).isTrue();
            assertThat(tokenProvider.validateToken(response.refreshToken())).isTrue();
        }

        @Test
        @DisplayName("❌ Вход с неверным паролем возвращает 401")
        void should_ReturnUnauthorized_When_PasswordIsInvalid() throws Exception {
            var request = new LoginRequest(TEST_USERNAME, "wrongpassword");

            performLogin(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("❌ Вход с пустым логином возвращает 400")
        void should_ReturnBadRequest_When_UsernameIsEmpty() throws Exception {
            var request = new LoginRequest("", TEST_PASSWORD);

            performLogin(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("❌ Вход с несуществующим пользователем возвращает 401")
        void should_ReturnUnauthorized_When_UserDoesNotExist() throws Exception {
            var request = new LoginRequest("nonexistent", "password123");

            performLogin(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @Commit
        @DisplayName("✅ Проверка rate limiting - 10 запросов подряд")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void should_HandleMultipleRequests_When_ConsecutiveLoginAttempts() throws Exception {
            var request = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

            for (int i = 0; i < 10; i++) {
                performLogin(request)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.access_token").isNotEmpty());
            }
        }
    }

    // ============================================================
    // POST /api/v1/auth/register
    // ============================================================
    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @Commit
        @DisplayName("✅ Регистрация нового пользователя возвращает 201")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void should_ReturnCreated_When_RegistrationIsSuccessful() throws Exception {
            var request = """
                    {
                    "username": "newuser",
                    "email": "newuser@example.com",
                    "password": "password123",
                    "firstName": "New",
                    "lastName": "User"
                    }
                    """;

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.username").value("newuser"))
                    .andExpect(jsonPath("$.email").value("newuser@example.com"))
                    .andExpect(jsonPath("$.is_active").value(false))  // ← true → false
                    .andExpect(jsonPath("$.roles").isArray());
        }

        @Test
        @DisplayName("❌ Регистрация с существующим username возвращает 409")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void should_ReturnConflict_When_UsernameAlreadyExists() throws Exception {
            var request = """
                    {
                        "username": "admin",
                        "email": "admin2@example.com",
                        "password": "password123"
                    }
                    """;

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("DUPLICATE_RESOURCE"));
        }

        @Test
        @DisplayName("❌ Регистрация с некорректным email возвращает 400")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void should_ReturnBadRequest_When_EmailIsInvalid() throws Exception {
            var request = """
                    {
                        "username": "newuser",
                        "email": "invalid-email",
                        "password": "password123"
                    }
                    """;

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION_FAILED"));
        }
    }

    // ============================================================
    // POST /api/v1/auth/refresh
    // ============================================================
    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @Commit
        @DisplayName("✅ Обновление токена с валидным refresh token")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void should_ReturnNewAccessToken_When_RefreshTokenIsValid() throws Exception {
            var refreshToken = obtainRefreshToken();

            mockMvc.perform(post(REFRESH_URL)
                            .header("Authorization", "Bearer " + refreshToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").value(refreshToken))
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME));
        }

        @Test
        @DisplayName("❌ Выход с невалидным токеном возвращает 401")
        void should_ReturnUnauthorized_When_TokenIsInvalid() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer invalid.token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_TOKEN_FORMAT"));
        }

        @Test
        @DisplayName("❌ Обновление без Authorization header возвращает 401")
        void should_ReturnUnauthorized_When_NoTokenProvided() throws Exception {
            mockMvc.perform(post(REFRESH_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("MISSING_TOKEN"));
        }

        @Test
        @DisplayName("❌ Обновление с неправильным форматом header возвращает 401")
        void should_ReturnUnauthorized_When_HeaderFormatIsInvalid() throws Exception {
            mockMvc.perform(post(REFRESH_URL)
                            .header("Authorization", "NotBearer token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_TOKEN_FORMAT"));
        }
    }

    // ============================================================
    // POST /api/v1/auth/logout
    // ============================================================
    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @Commit
        @DisplayName("✅ Выход из системы с валидным токеном возвращает 200")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void should_ReturnOk_When_LogoutWithValidToken() throws Exception {
            var accessToken = obtainAccessToken();

            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("❌ Выход без токена возвращает 401")
        void should_ReturnUnauthorized_When_NoTokenProvided() throws Exception {
            mockMvc.perform(post(LOGOUT_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_TOKEN_FORMAT"));
        }

        @Test
        @DisplayName("❌ Выход с невалидным токеном возвращает 401")
        void should_ReturnUnauthorized_When_TokenIsInvalid() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer invalid.token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_TOKEN_FORMAT"));
        }
    }

    // ============================================================
    // GET /api/v1/auth/me
    // ============================================================
    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class GetCurrentUserTests {

        @Test
        @Commit
        @DisplayName("✅ Получение текущего пользователя с валидным токеном")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void should_ReturnCurrentUser_When_TokenIsValid() throws Exception {
            var accessToken = obtainAccessToken();

            mockMvc.perform(get(ME_URL)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL));
        }

        @Test
        @DisplayName("❌ Получение пользователя без токена возвращает 401")
        void should_ReturnUnauthorized_When_NoTokenProvided() throws Exception {
            mockMvc.perform(get(ME_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED"));
        }
    }

    // ============================================================
    // POST /api/v1/auth/validate
    // ============================================================
    @Nested
    @DisplayName("POST /api/v1/auth/validate")
    class ValidateTokenTests {

        @Test
        @Commit
        @DisplayName("✅ Проверка валидного токена возвращает true")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void should_ReturnTrue_When_TokenIsValid() throws Exception {
            var accessToken = obtainAccessToken();

            mockMvc.perform(post(VALIDATE_URL)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));
        }

        @Test
        @DisplayName("❌ Проверка невалидного токена возвращает false")
        @Sql(scripts = "/db/test/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void should_ReturnFalse_When_TokenIsInvalid() throws Exception {
            mockMvc.perform(post(VALIDATE_URL)
                            .header("Authorization", "Bearer invalid.token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(false));
        }
    }
}