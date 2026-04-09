package com.carsensor.auth.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO для пользователя
 */
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

        @JsonProperty("is_locked")
        @Schema(description = "Заблокирован ли пользователь",
                example = "false",
                accessMode = Schema.AccessMode.READ_ONLY)
        boolean isLocked,

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
        LocalDateTime updatedAt,

        @Schema(description = "Версия для оптимистичной блокировки",
                example = "0",
                accessMode = Schema.AccessMode.READ_ONLY)
        Long version
) {
    // Compact constructor для нормализации
    public UserDto {
        if (username != null) {
            username = username.trim().toLowerCase();
        }
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    // ===== Фабричные методы =====

    /**
     * Создает DTO из Entity
     */
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                null,
                user.getFirstName(),
                user.getLastName(),
                user.isActive(),
                user.isLocked(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toList(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getVersion()
        );
    }

    /**
     * Создает Entity из DTO
     */
    public User toEntity() {
        return User.builder()
                .username(username)
                .email(email)
                .password(password)
                .firstName(firstName)
                .lastName(lastName)
                .isActive(isActive)
                .isLocked(isLocked)
                .version(version)
                .build();
    }

    /**
     * Обновляет существующую Entity
     */
    public User updateEntity(User user) {
        if (username != null && !username.equals(user.getUsername())) {
            user.setUsername(username);
        }
        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
        }
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        if (version != null) {
            user.setVersion(version);
        }
        return user;
    }
}