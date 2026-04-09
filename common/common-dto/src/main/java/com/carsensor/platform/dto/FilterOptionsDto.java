package com.carsensor.platform.dto;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для опций фильтрации
 */
@Schema(description = "Доступные опции для фильтрации автомобилей")
public record FilterOptionsDto(
        @Schema(description = "Список доступных марок",
                example = "[\"Toyota\", \"Honda\", \"Nissan\"]")
        List<String> brands,

        @JsonProperty("year_min")
        @Schema(description = "Минимальный год выпуска",
                example = "2015")
        Integer yearMin,

        @JsonProperty("year_max")
        @Schema(description = "Максимальный год выпуска",
                example = "2025")
        Integer yearMax,

        @JsonProperty("price_min")
        @Schema(description = "Минимальная цена",
                example = "1000000")
        BigDecimal priceMin,

        @JsonProperty("price_max")
        @Schema(description = "Максимальная цена",
                example = "5000000")
        BigDecimal priceMax,

        @Schema(description = "Список доступных типов трансмиссии",
                example = "[\"AT\", \"MT\", \"CVT\"]")
        List<String> transmissions,

        @JsonProperty("drive_types")
        @Schema(description = "Список доступных типов привода",
                example = "[\"2WD\", \"4WD\", \"AWD\"]")
        List<String> driveTypes,

        @Schema(description = "Список доступных цветов",
                example = "[\"Белый\", \"Черный\", \"Серебристый\"]")
        List<String> colors
) {
    // Компактный конструктор для нормализации
    public FilterOptionsDto {
        // Обеспечиваем, что списки не null
        if (brands == null) {
            brands = List.of();
        }
        if (transmissions == null) {
            transmissions = List.of();
        }
        if (driveTypes == null) {
            driveTypes = List.of();
        }
        if (colors == null) {
            colors = List.of();
        }
    }
}