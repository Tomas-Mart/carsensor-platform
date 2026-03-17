package com.carsensor.platform.dto;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FilterOptionsDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testFilterOptionsSerialization() throws Exception {
        // Arrange
        FilterOptionsDto original = FilterOptionsDto.builder()
                .brands(List.of("Toyota", "Honda"))
                .yearMin(2015)
                .yearMax(2025)
                .priceMin(new BigDecimal("1000000"))
                .priceMax(new BigDecimal("5000000"))
                .transmissions(List.of("AT", "MT"))
                .driveTypes(List.of("2WD", "4WD"))
                .colors(List.of("Белый", "Черный"))
                .build();

        // Act
        String json = mapper.writeValueAsString(original);
        FilterOptionsDto deserialized = mapper.readValue(json, FilterOptionsDto.class);

        // Assert
        assertEquals(original.brands(), deserialized.brands());
        assertEquals(original.yearMin(), deserialized.yearMin());
        assertEquals(original.yearMax(), deserialized.yearMax());
        assertEquals(original.priceMin(), deserialized.priceMin());
        assertEquals(original.priceMax(), deserialized.priceMax());
        assertEquals(original.transmissions(), deserialized.transmissions());
        assertEquals(original.driveTypes(), deserialized.driveTypes());
        assertEquals(original.colors(), deserialized.colors());
    }

    @Test
    void testFilterOptionsWithMinimalFields() {
        // Act
        FilterOptionsDto options = FilterOptionsDto.builder()
                .brands(List.of("Toyota"))
                .build();

        // Assert
        assertNotNull(options);
        assertEquals(List.of("Toyota"), options.brands());
        assertNull(options.yearMin());
        assertNull(options.priceMin());
        assertNull(options.transmissions());
    }
}