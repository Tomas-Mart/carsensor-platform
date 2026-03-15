package com.carsensor.platform.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

/**
 * DTO для опций фильтрации
 */
@Builder
public record FilterOptionsDto(
        List<String> brands,
        Integer yearMin,
        Integer yearMax,
        BigDecimal priceMin,
        BigDecimal priceMax,
        List<String> transmissions,
        List<String> driveTypes,
        List<String> colors
) {
}