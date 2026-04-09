package com.carsensor.platform.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для роли
 */
@Schema(description = "Роль пользователя")
public record RoleDto(
        @Schema(description = "ID роли",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @NotBlank(message = "Название роли обязательно")
        @Size(min = 3, max = 50, message = "Название роли должно содержать от 3 до 50 символов")
        @Schema(description = "Название роли",
                example = "ROLE_ADMIN",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 3,
                maxLength = 50)
        String name,

        @Size(max = 200, message = "Описание не должно превышать 200 символов")
        @Schema(description = "Описание роли",
                example = "Администратор с полными правами",
                maxLength = 200)
        String description,

        @JsonProperty("permissions")
        @Schema(description = "Список разрешений, связанных с ролью",
                example = "[\"CAR_VIEW\", \"CAR_EDIT\", \"USER_MANAGE\"]")
        List<String> permissions,

        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "Дата создания",
                example = "2026-03-29 12:00:00",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime createdAt,

        @JsonProperty("updated_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "Дата обновления",
                example = "2026-03-29 12:00:00",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime updatedAt,

        @Schema(description = "Версия для оптимистичной блокировки",
                example = "0",
                accessMode = Schema.AccessMode.READ_ONLY)
        Long version
) {
    // Компактный конструктор для нормализации
    public RoleDto {
        if (name != null) {
            name = name.trim().toUpperCase();
            if (!name.startsWith("ROLE_")) {
                name = "ROLE_" + name;
            }
        }
        if (description != null) {
            description = description.trim();
        }
        if (permissions == null) {
            permissions = List.of();
        }
    }

    // Фабричный метод для создания простой роли
    public static RoleDto of(String name, String description) {
        return new RoleDto(null, name, description, List.of(), null, null, null);
    }

    // Фабричный метод для создания роли с разрешениями
    public static RoleDto of(String name, String description, List<String> permissions) {
        return new RoleDto(null, name, description, permissions, null, null, null);
    }
}