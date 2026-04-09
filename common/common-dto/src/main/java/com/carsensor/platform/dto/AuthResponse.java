package com.carsensor.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ответ с JWT токенами
 */
@Schema(description = "Ответ с JWT токенами после успешной аутентификации")
public record AuthResponse(
        @JsonProperty("access_token")
        @Schema(description = "JWT токен доступа",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String accessToken,

        @JsonProperty("refresh_token")
        @Schema(description = "JWT токен для обновления",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken,

        @JsonProperty("token_type")
        @Schema(description = "Тип токена",
                example = "Bearer",
                defaultValue = "Bearer",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String tokenType,

        @JsonProperty("expires_in")
        @Schema(description = "Время жизни токена в секундах",
                example = "900",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long expiresIn,

        @JsonProperty("username")
        @Schema(description = "Имя пользователя",
                example = "admin",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @JsonProperty("roles")
        @Schema(description = "Роли пользователя",
                example = "[\"ROLE_ADMIN\", \"ROLE_USER\"]")
        String[] roles
) {
    public static final String DEFAULT_TOKEN_TYPE = "Bearer";

    // Компактный конструктор для нормализации
    public AuthResponse {
        if (tokenType == null || tokenType.isBlank()) {
            tokenType = DEFAULT_TOKEN_TYPE;
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken cannot be null or blank");
        }
        if (expiresIn <= 0) {
            throw new IllegalArgumentException("expiresIn must be positive");
        }
    }

    // Фабричный метод для удобного создания
    public static AuthResponse of(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String username,
            String[] roles) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                DEFAULT_TOKEN_TYPE,
                expiresIn,
                username,
                roles);
    }
}