package com.carsensor.platform.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Refresh токен")
public record RefreshTokenDto(
        @Schema(description = "ID токена",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Refresh token",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                accessMode = Schema.AccessMode.READ_ONLY)
        String token,

        @Schema(description = "ID пользователя",
                example = "10")
        Long userId,

        @Schema(description = "Имя пользователя",
                example = "admin")
        String username,

        @JsonProperty("expires_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "Время истечения",
                example = "2026-03-30 12:00:00")
        LocalDateTime expiresAt,

        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "Время создания",
                example = "2026-03-29 12:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Активен ли токен",
                example = "true")
        boolean active
) {
    // Фабричный метод для создания
    public static RefreshTokenDto of(
            Long id, String token, Long userId, String username,
            LocalDateTime expiresAt, LocalDateTime createdAt, boolean active
    ) {
        return new RefreshTokenDto(id, token, userId, username, expiresAt, createdAt, active);
    }
}