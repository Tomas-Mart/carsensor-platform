package com.carsensor.auth.integration.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.web.servlet.MockMvc;
import com.carsensor.auth.AuthServiceApplication;
import com.carsensor.auth.infrastructure.security.JwtTokenProvider;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.platform.dto.AuthResponse;
import com.carsensor.platform.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для AuthController.
 * Проверяют работу контроллера с Embedded PostgreSQL.
 *
 * @see AbstractIntegrationTest
 * @since 1.0
 */
@SpringBootTest(
        classes = AuthServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@DisplayName("Интеграционные тесты AuthController")
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @SqlGroup({@Sql(scripts = "/db/test/cleanup.sql",
                config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
                executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/test/insert-test-user.sql",
                        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
                        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)})
        @DisplayName("Успешный вход с admin:admin123")
        void login_ValidCredentials_ReturnsTokens() throws Exception {
            var request = new LoginRequest("admin", "admin123");

            var result = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.roles").isArray())
                    .andReturn();

            var responseJson = result.getResponse().getContentAsString();
            var response = objectMapper.readValue(responseJson, AuthResponse.class);

            assertThat(tokenProvider.validateToken(response.accessToken())).isTrue();
            assertThat(tokenProvider.validateToken(response.refreshToken())).isTrue();
        }

        @Test
        @Sql(scripts = {"/db/test/cleanup.sql", "/db/test/insert-test-user.sql"},
                executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @DisplayName("Вход с неверным паролем возвращает 401")
        void login_InvalidPassword_ReturnsUnauthorized() throws Exception {
            var request = new LoginRequest("admin", "wrongpassword");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Вход с пустым логином возвращает 400")
        void login_EmptyUsername_ReturnsBadRequest() throws Exception {
            var request = new LoginRequest("", "password123");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("Вход с несуществующим пользователем возвращает 401")
        void login_NonExistentUser_ReturnsUnauthorized() throws Exception {
            var request = new LoginRequest("nonexistent", "password123");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @SqlGroup({@Sql(scripts = "/db/test/cleanup.sql",
                config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
                executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/test/insert-test-user.sql",
                        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
                        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)})
        @DisplayName("Проверка rate limiting - много запросов подряд")
        void login_MultipleAttempts_HandlesCorrectly() throws Exception {
            var request = new LoginRequest("admin", "admin123");

            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @SqlGroup({@Sql(scripts = "/db/test/cleanup.sql",
                config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
                executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/test/insert-test-user.sql",
                        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
                        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)})
        @DisplayName("Обновление токена с валидным refresh token")
        void refresh_ValidToken_ReturnsNewAccessToken() throws Exception {
            var loginRequest = new LoginRequest("admin", "admin123");
            var loginResult = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            var loginResponse = objectMapper.readValue(
                    loginResult.getResponse().getContentAsString(),
                    AuthResponse.class
            );

            mockMvc.perform(post(REFRESH_URL)
                            .header("Authorization", "Bearer " + loginResponse.refreshToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").value(loginResponse.refreshToken()))
                    .andExpect(jsonPath("$.username").value("admin"));
        }

        @Test
        @DisplayName("Обновление с невалидным токеном возвращает 401")
        void refresh_InvalidToken_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(post(REFRESH_URL)
                            .header("Authorization", "Bearer invalid.token.here"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_TOKEN_FORMAT"));
        }

        @Test
        @DisplayName("Обновление без Authorization header возвращает 401")
        void refresh_NoToken_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(post(REFRESH_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("MISSING_TOKEN"));
        }

        @Test
        @DisplayName("Обновление с неправильным форматом header возвращает 401")
        void refresh_WrongHeaderFormat_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(post(REFRESH_URL)
                            .header("Authorization", "NotBearer token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_TOKEN_FORMAT"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @SqlGroup({@Sql(scripts = "/db/test/cleanup.sql",
                config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
                executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/test/insert-test-user.sql",
                        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
                        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)})
        @DisplayName("Выход из системы с валидным токеном")
        void logout_ValidToken_ReturnsOk() throws Exception {
            var loginRequest = new LoginRequest("admin", "admin123");
            var loginResult = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            var loginResponse = objectMapper.readValue(
                    loginResult.getResponse().getContentAsString(),
                    AuthResponse.class
            );

            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer " + loginResponse.accessToken()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Выход без токена возвращает 401")
        void logout_NoToken_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(post(LOGOUT_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("Выход с невалидным токеном возвращает 401")
        void logout_WithInvalidToken_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer invalid.token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_TOKEN_FORMAT"));
        }
    }
}