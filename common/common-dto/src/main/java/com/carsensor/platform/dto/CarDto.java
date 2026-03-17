package com.carsensor.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Данные автомобиля")
public record CarDto(
        @Schema(description = "ID автомобиля",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @NotBlank(message = "Марка обязательна")
        @Size(min = 1, max = 100, message = "Марка должна содержать от 1 до 100 символов")
        @Schema(description = "Марка автомобиля",
                example = "Toyota",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = 100)
        String brand,

        @NotBlank(message = "Модель обязательна")
        @Size(min = 1, max = 100, message = "Модель должна содержать от 1 до 100 символов")
        @Schema(description = "Модель автомобиля",
                example = "Camry",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = 100)
        String model,

        @NotNull(message = "Год выпуска обязателен")
        @Min(value = 1900, message = "Год выпуска должен быть не менее 1900")
        @Max(value = 2026, message = "Год выпуска должен быть не более 2026")
        @Schema(description = "Год выпуска",
                example = "2020",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1900",
                maximum = "2026")
        Integer year,

        @NotNull(message = "Пробег обязателен")
        @Min(value = 0, message = "Пробег не может быть отрицательным")
        @Schema(description = "Пробег в км",
                example = "50000",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0")
        Integer mileage,

        @NotNull(message = "Цена обязательна")
        @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть положительной")
        @Schema(description = "Цена в рублях",
                example = "2500000",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0")
        BigDecimal price,

        @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
        @Schema(description = "Описание автомобиля",
                example = "Отличное состояние, полный привод, люк",
                maxLength = 1000)
        String description,

        @JsonProperty("original_brand")
        @Schema(description = "Оригинальное название марки (на японском)",
                example = "トヨタ")
        String originalBrand,

        @JsonProperty("original_model")
        @Schema(description = "Оригинальное название модели (на японском)",
                example = "カムリ")
        String originalModel,

        @JsonProperty("exterior_color")
        @Schema(description = "Цвет кузова",
                example = "Белый")
        String exteriorColor,

        @JsonProperty("interior_color")
        @Schema(description = "Цвет салона",
                example = "Черный")
        String interiorColor,

        @JsonProperty("engine_capacity")
        @Schema(description = "Объем двигателя",
                example = "2.5L")
        String engineCapacity,

        @Schema(description = "Тип трансмиссии",
                example = "AT",
                allowableValues = {"AT", "MT", "CVT"})
        String transmission,

        @JsonProperty("drive_type")
        @Schema(description = "Тип привода",
                example = "4WD",
                allowableValues = {"2WD", "4WD", "AWD"})
        String driveType,

        @JsonProperty("photo_urls")
        @Schema(description = "Список URL фотографий",
                example = "[\"/images/1.jpg\", \"/images/2.jpg\"]")
        List<String> photoUrls,

        @JsonProperty("main_photo_url")
        @Schema(description = "URL главного фото",
                example = "/images/1.jpg")
        String mainPhotoUrl,

        @JsonProperty("external_id")
        @Schema(description = "Внешний ID с сайта источника",
                example = "CS123456")
        String externalId,

        @JsonProperty("source_url")
        @Schema(description = "URL источника",
                example = "https://carsensor.net/...")
        String sourceUrl,

        @JsonProperty("parsed_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "Дата парсинга",
                example = "2026-03-16 14:30:00",
                pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime parsedAt,

        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "Дата создания записи",
                example = "2026-03-16 14:30:00",
                pattern = "yyyy-MM-dd HH:mm:ss",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime createdAt,

        @JsonProperty("updated_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "Дата обновления записи",
                example = "2026-03-16 14:30:00",
                pattern = "yyyy-MM-dd HH:mm:ss",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime updatedAt
) {
}