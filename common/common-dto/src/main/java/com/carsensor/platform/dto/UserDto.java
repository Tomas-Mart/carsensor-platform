package com.carsensor.platform.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.With;

/**
 * DTO для пользователя
 */
@Builder
@With
@Schema(description = "Данные пользователя")
public record UserDto(
        @Schema(description = "ID пользователя",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @NotBlank(message = "Имя пользователя обязательно")
        @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Имя пользователя может содержать только буквы, цифры и подчеркивание")
        @Schema(description = "Имя пользователя для входа",
                example = "admin",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 3,
                maxLength = 50,
                pattern = "^[a-zA-Z0-9_]+$")
        String username,

        @NotBlank(message = "Email обязателен")
        @Email(message = "Неверный формат email")
        @Size(max = 100, message = "Email не должен превышать 100 символов")
        @Schema(description = "Email пользователя",
                example = "admin@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 100,
                format = "email")
        String email,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
        @Schema(description = "Пароль (только для записи)",
                example = "admin123",
                minLength = 6,
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY)
        String password,

        @Size(max = 50, message = "Имя не должно превышать 50 символов")
        @JsonProperty("first_name")
        @Schema(description = "Имя",
                example = "Admin",
                maxLength = 50)
        String firstName,

        @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
        @JsonProperty("last_name")
        @Schema(description = "Фамилия",
                example = "User",
                maxLength = 50)
        String lastName,

        @JsonProperty("is_active")
        @Schema(description = "Активен ли пользователь",
                example = "true")
        boolean isActive,

        @Schema(description = "Список ролей пользователя")
        List<String> roles,

        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "Дата создания",
                example = "2026-03-16 14:30:00",
                pattern = "yyyy-MM-dd HH:mm:ss",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime createdAt,

        @JsonProperty("updated_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "Дата обновления",
                example = "2026-03-16 14:30:00",
                pattern = "yyyy-MM-dd HH:mm:ss",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime updatedAt
) {
}