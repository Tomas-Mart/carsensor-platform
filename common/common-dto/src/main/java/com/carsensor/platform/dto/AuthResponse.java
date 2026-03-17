package com.carsensor.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Ответ с JWT токенами
 */
@Builder
@Schema(description = "Ответ с JWT токенами после успешной аутентификации")
public record AuthResponse(
        @JsonProperty("access_token")
        @Schema(description = "JWT токен доступа",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String accessToken,

        @JsonProperty("refresh_token")
        @Schema(description = "JWT токен для обновления",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ")
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
    public static final String TOKEN_TYPE = "Bearer";

    public AuthResponse {
        tokenType = TOKEN_TYPE;
    }
}