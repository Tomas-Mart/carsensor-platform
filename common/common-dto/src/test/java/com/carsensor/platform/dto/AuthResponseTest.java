package com.carsensor.platform.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Юнит-тесты для AuthResponse.
 *
 * <p><b>Проверяемые аспекты:</b>
 * <ul>
 *   <li>Сериализация/десериализация JSON</li>
 *   <li>Компактный конструктор (нормализация и валидация)</li>
 *   <li>Фабричный метод {@code of()}</li>
 * </ul>
 *
 * @see AuthResponse
 * @since 1.0
 */
@DisplayName("Тесты AuthResponse")
class AuthResponseTest {

    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIs...";
    private static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIs...";
    private static final String USERNAME = "admin";
    private static final long EXPIRES_IN = 900L;
    private static final String[] ROLES = {"ROLE_ADMIN", "ROLE_USER"};

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================================================
    // ТЕСТЫ СЕРИАЛИЗАЦИИ
    // ============================================================

    @Nested
    @DisplayName("Сериализация и десериализация JSON")
    class SerializationTests {

        @Test
        @DisplayName("✅ Сериализация AuthResponse в JSON и обратно")
        void should_SerializeAndDeserialize_Correctly() throws Exception {
            // given
            var original = new AuthResponse(
                    ACCESS_TOKEN,
                    REFRESH_TOKEN,
                    AuthResponse.DEFAULT_TOKEN_TYPE,
                    EXPIRES_IN,
                    USERNAME,
                    ROLES
            );

            // when
            var json = objectMapper.writeValueAsString(original);
            var deserialized = objectMapper.readValue(json, AuthResponse.class);

            // then
            assertThat(deserialized)
                    .isNotNull()
                    .satisfies(response -> {
                        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
                        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
                        assertThat(response.tokenType()).isEqualTo(AuthResponse.DEFAULT_TOKEN_TYPE);
                        assertThat(response.expiresIn()).isEqualTo(EXPIRES_IN);
                        assertThat(response.username()).isEqualTo(USERNAME);
                        assertThat(response.roles()).containsExactly(ROLES);
                    });
        }

        @Test
        @DisplayName("✅ Десериализация JSON с snake_case полями")
        void should_Deserialize_WithSnakeCaseFields() throws Exception {
            // given
            var json = """
                    {
                        "access_token": "token123",
                        "refresh_token": "refresh456",
                        "token_type": "Bearer",
                        "expires_in": 900,
                        "username": "admin",
                        "roles": ["ROLE_ADMIN"]
                    }
                    """;

            // when
            var response = objectMapper.readValue(json, AuthResponse.class);

            // then
            assertThat(response)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.accessToken()).isEqualTo("token123");
                        assertThat(r.refreshToken()).isEqualTo("refresh456");
                        assertThat(r.tokenType()).isEqualTo("Bearer");
                        assertThat(r.expiresIn()).isEqualTo(900L);
                        assertThat(r.username()).isEqualTo("admin");
                        assertThat(r.roles()).containsExactly("ROLE_ADMIN");
                    });
        }
    }

    // ============================================================
    // ТЕСТЫ КОМПАКТНОГО КОНСТРУКТОРА
    // ============================================================

    @Nested
    @DisplayName("Компактный конструктор (нормализация и валидация)")
    class CompactConstructorTests {

        @Test
        @DisplayName("✅ Null tokenType заменяется на DEFAULT_TOKEN_TYPE")
        void should_SetDefaultTokenType_WhenTokenTypeIsNull() {
            // when
            var response = new AuthResponse(
                    ACCESS_TOKEN,
                    REFRESH_TOKEN,
                    null,
                    EXPIRES_IN,
                    USERNAME,
                    ROLES
            );

            // then
            assertThat(response.tokenType()).isEqualTo(AuthResponse.DEFAULT_TOKEN_TYPE);
        }

        @Test
        @DisplayName("✅ Blank tokenType заменяется на DEFAULT_TOKEN_TYPE")
        void should_SetDefaultTokenType_WhenTokenTypeIsBlank() {
            // when
            var response = new AuthResponse(
                    ACCESS_TOKEN,
                    REFRESH_TOKEN,
                    "   ",
                    EXPIRES_IN,
                    USERNAME,
                    ROLES
            );

            // then
            assertThat(response.tokenType()).isEqualTo(AuthResponse.DEFAULT_TOKEN_TYPE);
        }

        @Test
        @DisplayName("✅ Корректный tokenType остается без изменений")
        void should_KeepTokenType_WhenTokenTypeIsValid() {
            // given
            var customTokenType = "Custom";

            // when
            var response = new AuthResponse(
                    ACCESS_TOKEN,
                    REFRESH_TOKEN,
                    customTokenType,
                    EXPIRES_IN,
                    USERNAME,
                    ROLES
            );

            // then
            assertThat(response.tokenType()).isEqualTo(customTokenType);
        }

        @Test
        @DisplayName("❌ Null accessToken выбрасывает IllegalArgumentException")
        void should_ThrowException_WhenAccessTokenIsNull() {
            assertThatThrownBy(() -> new AuthResponse(
                    null,
                    REFRESH_TOKEN,
                    AuthResponse.DEFAULT_TOKEN_TYPE,
                    EXPIRES_IN,
                    USERNAME,
                    ROLES
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("accessToken cannot be null or blank");
        }

        @Test
        @DisplayName("❌ Blank accessToken выбрасывает IllegalArgumentException")
        void should_ThrowException_WhenAccessTokenIsBlank() {
            assertThatThrownBy(() -> new AuthResponse(
                    "   ",
                    REFRESH_TOKEN,
                    AuthResponse.DEFAULT_TOKEN_TYPE,
                    EXPIRES_IN,
                    USERNAME,
                    ROLES
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("accessToken cannot be null or blank");
        }

        @Test
        @DisplayName("❌ expiresIn <= 0 выбрасывает IllegalArgumentException")
        void should_ThrowException_WhenExpiresInIsNotPositive() {
            assertThatThrownBy(() -> new AuthResponse(
                    ACCESS_TOKEN,
                    REFRESH_TOKEN,
                    AuthResponse.DEFAULT_TOKEN_TYPE,
                    0,
                    USERNAME,
                    ROLES
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expiresIn must be positive");
        }

        @Test
        @DisplayName("✅ Refresh token и roles могут быть null")
        void should_AllowNullRefreshTokenAndRoles() {
            // when
            var response = new AuthResponse(
                    ACCESS_TOKEN,
                    null,
                    null,
                    EXPIRES_IN,
                    USERNAME,
                    null
            );

            // then
            assertThat(response)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.accessToken()).isEqualTo(ACCESS_TOKEN);
                        assertThat(r.refreshToken()).isNull();
                        assertThat(r.tokenType()).isEqualTo(AuthResponse.DEFAULT_TOKEN_TYPE);
                        assertThat(r.expiresIn()).isEqualTo(EXPIRES_IN);
                        assertThat(r.username()).isEqualTo(USERNAME);
                        assertThat(r.roles()).isNull();
                    });
        }
    }

    // ============================================================
    // ТЕСТЫ ФАБРИЧНОГО МЕТОДА of()
    // ============================================================

    @Nested
    @DisplayName("Фабричный метод of()")
    class FactoryMethodTests {

        @Test
        @DisplayName("✅ of() создает AuthResponse с DEFAULT_TOKEN_TYPE")
        void should_CreateAuthResponse_WithDefaultTokenType() {
            // when
            var response = AuthResponse.of(
                    ACCESS_TOKEN,
                    REFRESH_TOKEN,
                    EXPIRES_IN,
                    USERNAME,
                    ROLES
            );

            // then
            assertThat(response)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.accessToken()).isEqualTo(ACCESS_TOKEN);
                        assertThat(r.refreshToken()).isEqualTo(REFRESH_TOKEN);
                        assertThat(r.tokenType()).isEqualTo(AuthResponse.DEFAULT_TOKEN_TYPE);
                        assertThat(r.expiresIn()).isEqualTo(EXPIRES_IN);
                        assertThat(r.username()).isEqualTo(USERNAME);
                        assertThat(r.roles()).containsExactly(ROLES);
                    });
        }

        @Test
        @DisplayName("✅ of() с null roles создает AuthResponse с null roles")
        void should_CreateAuthResponse_WithNullRoles() {
            // when
            var response = AuthResponse.of(
                    ACCESS_TOKEN,
                    REFRESH_TOKEN,
                    EXPIRES_IN,
                    USERNAME,
                    null
            );

            // then
            assertThat(response)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.accessToken()).isEqualTo(ACCESS_TOKEN);
                        assertThat(r.refreshToken()).isEqualTo(REFRESH_TOKEN);
                        assertThat(r.expiresIn()).isEqualTo(EXPIRES_IN);
                        assertThat(r.username()).isEqualTo(USERNAME);
                        assertThat(r.roles()).isNull();
                    });
        }
    }

    // ============================================================
    // ТЕСТЫ КОНСТАНТ
    // ============================================================

    @Nested
    @DisplayName("Константы")
    class ConstantsTests {

        @Test
        @DisplayName("✅ DEFAULT_TOKEN_TYPE = 'Bearer'")
        void should_HaveCorrectDefaultTokenType() {
            assertThat(AuthResponse.DEFAULT_TOKEN_TYPE).isEqualTo("Bearer");
        }
    }
}