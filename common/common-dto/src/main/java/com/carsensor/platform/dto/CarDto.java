package com.carsensor.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.With;

/**
 * DTO для автомобиля с использованием Java 21 Records
 * Immutable, без boilerplate кода
 */
@Builder
@With
public record CarDto(
        Long id,

        @NotBlank(message = "Марка обязательна")
        @Size(min = 1, max = 100, message = "Марка должна содержать от 1 до 100 символов")
        String brand,

        @NotBlank(message = "Модель обязательна")
        @Size(min = 1, max = 100, message = "Модель должна содержать от 1 до 100 символов")
        String model,

        @NotNull(message = "Год выпуска обязателен")
        @Min(value = 1900, message = "Год выпуска должен быть не менее 1900")
        @Max(value = 2026, message = "Год выпуска должен быть не более 2026")
        Integer year,

        @NotNull(message = "Пробег обязателен")
        @Min(value = 0, message = "Пробег не может быть отрицательным")
        Integer mileage,

        @NotNull(message = "Цена обязательна")
        @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть положительной")
        BigDecimal price,

        @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
        String description,

        String originalBrand,
        String originalModel,
        String exteriorColor,
        String interiorColor,
        String engineCapacity,
        String transmission,
        String driveType,

        List<String> photoUrls,
        String mainPhotoUrl,

        String externalId,

        String sourceUrl,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime parsedAt,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {
}