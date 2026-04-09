package com.carsensor.platform.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Разрешение (permission)")
public record PermissionDto(
        @Schema(description = "ID разрешения",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @NotBlank(message = "Название разрешения обязательно")
        @Size(min = 3, max = 50, message = "Название разрешения должно содержать от 3 до 50 символов")
        @Schema(description = "Название разрешения",
                example = "VIEW_CARS",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Size(max = 200, message = "Описание не должно превышать 200 символов")
        @Schema(description = "Описание разрешения",
                example = "Просмотр автомобилей")
        String description,

        // ============================================================
        // ПОЛЯ АУДИТА
        // ============================================================

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
    public PermissionDto {
        if (name != null) {
            name = name.trim().toUpperCase();
        }
        if (description != null) {
            description = description.trim();
        }
    }

    // ============================================================
    // ФАБРИЧНЫЕ МЕТОДЫ
    // ============================================================

    // Фабричный метод для создания простого разрешения
    public static PermissionDto of(String name, String description) {
        return new PermissionDto(null, name, description, null, null, null);
    }

    // Фабричный метод для создания разрешения с ID
    public static PermissionDto of(Long id, String name, String description) {
        return new PermissionDto(id, name, description, null, null, null);
    }
}