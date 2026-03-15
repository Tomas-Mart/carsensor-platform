package com.carsensor.auth.integration.controller;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
 * Интеграционные тесты для AuthController
 * Использует Testcontainers с реальной PostgreSQL
 */
@DisplayName("Интеграционные тесты AuthController")
@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
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
        @Sql(scripts = {"/db/test/cleanup.sql", "/db/test/insert-test-user.sql"})
        @DisplayName("Успешный вход с admin:admin123")
        void login_ValidCredentials_ReturnsTokens() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin", "admin123");

            // Act & Assert
            MvcResult result = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.roles").isArray())
                    .andReturn();

            // Проверяем, что токены валидны
            String responseJson = result.getResponse().getContentAsString();
            AuthResponse response = objectMapper.readValue(responseJson, AuthResponse.class);

            assertThat(tokenProvider.validateToken(response.accessToken())).isTrue();
            assertThat(tokenProvider.validateToken(response.refreshToken())).isTrue();
        }

        @Test
        @Sql(scripts = {"/db/test/cleanup.sql", "/db/test/insert-test-user.sql"})
        @DisplayName("Вход с неверным паролем возвращает 401")
        void login_InvalidPassword_ReturnsUnauthorized() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin", "wrongpassword");

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Вход с пустым логином возвращает 400")
        void login_EmptyUsername_ReturnsBadRequest() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("", "password123");

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("Вход с несуществующим пользователем возвращает 401")
        void login_NonExistentUser_ReturnsUnauthorized() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("nonexistent", "password123");

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @Sql(scripts = {"/db/test/cleanup.sql", "/db/test/insert-test-user.sql"})
        @DisplayName("Проверка rate limiting - много запросов подряд")
        void login_MultipleAttempts_HandlesCorrectly() throws Exception {
            LoginRequest request = new LoginRequest("admin", "admin123");

            // 10 успешных запросов подряд
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
        @Sql(scripts = {"/db/test/cleanup.sql", "/db/test/insert-test-user.sql"})
        @DisplayName("Обновление токена с валидным refresh token")
        void refresh_ValidToken_ReturnsNewAccessToken() throws Exception {
            // Сначала логинимся, получаем refresh token
            LoginRequest loginRequest = new LoginRequest("admin", "admin123");
            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            AuthResponse loginResponse = objectMapper.readValue(
                    loginResult.getResponse().getContentAsString(),
                    AuthResponse.class
            );

            // Теперь обновляем токен
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
            // Arrange
            Map<String, String> request = new HashMap<>();
            request.put("refreshToken", "invalid.token.string");

            // Act & Assert
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Необходима аутентификация для доступа к ресурсу"))
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Обновление с токеном в заголовке возвращает 401")
        void refresh_WithTokenInHeader_ReturnsUnauthorized() throws Exception {
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
        @Sql(scripts = {"/db/test/cleanup.sql", "/db/test/insert-test-user.sql"})
        @DisplayName("Выход из системы с валидным токеном")
        void logout_ValidToken_ReturnsOk() throws Exception {
            // Получаем токен
            LoginRequest loginRequest = new LoginRequest("admin", "admin123");
            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            AuthResponse loginResponse = objectMapper.readValue(
                    loginResult.getResponse().getContentAsString(),
                    AuthResponse.class
            );

            // Выходим
            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer " + loginResponse.accessToken()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Выход без токена возвращает 401")
        void logout_NoToken_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(post(LOGOUT_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("MISSING_TOKEN"));
        }
    }
}