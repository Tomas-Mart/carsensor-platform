package com.carsensor.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на логин - Record с валидацией
 */
@Schema(description = "Запрос на аутентификацию")
public record LoginRequest(
        @NotBlank(message = "Логин не может быть пустым")
        @Size(min = 3, max = 50, message = "Логин должен содержать от 3 до 50 символов")
        @Schema(description = "Имя пользователя",
                example = "admin",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 3,
                maxLength = 50)
        String username,

        @NotBlank(message = "Пароль не может быть пустым")
        @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
        @Schema(description = "Пароль",
                example = "admin123",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 6,
                format = "password")
        String password
) {
    // Компактный конструктор для нормализации
    public LoginRequest {
        if (username != null) {
            username = username.trim().toLowerCase();
        }
        if (password != null) {
            password = password.trim();
        }
    }
}