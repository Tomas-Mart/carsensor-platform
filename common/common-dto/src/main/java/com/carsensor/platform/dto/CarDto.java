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

/**
 * DTO для автомобиля с использованием Java 21 Records
 * Immutable, с поддержкой Builder паттерна через отдельный класс
 */
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
        LocalDateTime updatedAt,

        @Schema(description = "Версия для оптимистичной блокировки",
                example = "0",
                accessMode = Schema.AccessMode.READ_ONLY)
        Long version
) {
    // Compact constructor для нормализации данных
    public CarDto {
        if (brand != null) {
            brand = brand.trim();
        }
        if (model != null) {
            model = model.trim();
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }

    // ===== ФАБРИЧНЫЕ МЕТОДЫ =====

    public static CarDto of(String brand, String model) {
        return new CarDto(null, brand, model, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null);
    }

    public static CarDto of(String brand, String model, Integer year, Integer mileage, BigDecimal price) {
        return new CarDto(null, brand, model, year, mileage, price,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null);
    }

    public static CarDto of(
            String brand, String model, Integer year, Integer mileage,
            BigDecimal price, String exteriorColor, String transmission, String driveType
    ) {
        return new CarDto(null, brand, model, year, mileage, price,
                null, null, null, exteriorColor, null, null,
                transmission, driveType, null, null, null, null,
                null, null, null, null);
    }

    // ===== BUILDER =====

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String brand;
        private String model;
        private Integer year;
        private Integer mileage;
        private BigDecimal price;
        private String description;
        private String originalBrand;
        private String originalModel;
        private String exteriorColor;
        private String interiorColor;
        private String engineCapacity;
        private String transmission;
        private String driveType;
        private List<String> photoUrls;
        private String mainPhotoUrl;
        private String externalId;
        private String sourceUrl;
        private LocalDateTime parsedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long version;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder year(Integer year) {
            this.year = year;
            return this;
        }

        public Builder mileage(Integer mileage) {
            this.mileage = mileage;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder originalBrand(String originalBrand) {
            this.originalBrand = originalBrand;
            return this;
        }

        public Builder originalModel(String originalModel) {
            this.originalModel = originalModel;
            return this;
        }

        public Builder exteriorColor(String exteriorColor) {
            this.exteriorColor = exteriorColor;
            return this;
        }

        public Builder interiorColor(String interiorColor) {
            this.interiorColor = interiorColor;
            return this;
        }

        public Builder engineCapacity(String engineCapacity) {
            this.engineCapacity = engineCapacity;
            return this;
        }

        public Builder transmission(String transmission) {
            this.transmission = transmission;
            return this;
        }

        public Builder driveType(String driveType) {
            this.driveType = driveType;
            return this;
        }

        public Builder photoUrls(List<String> photoUrls) {
            this.photoUrls = photoUrls;
            return this;
        }

        public Builder mainPhotoUrl(String mainPhotoUrl) {
            this.mainPhotoUrl = mainPhotoUrl;
            return this;
        }

        public Builder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder sourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        public Builder parsedAt(LocalDateTime parsedAt) {
            this.parsedAt = parsedAt;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public CarDto build() {
            return new CarDto(
                    id, brand, model, year, mileage, price,
                    description, originalBrand, originalModel, exteriorColor,
                    interiorColor, engineCapacity, transmission, driveType,
                    photoUrls, mainPhotoUrl, externalId, sourceUrl,
                    parsedAt, createdAt, updatedAt, version
            );
        }
    }
}