package com.carsensor.platform.dto;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CarDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testCarDtoSerialization() throws Exception {
        // Arrange - создаем DTO с основными полями
        CarDto original = CarDto.builder()
                .id(1L)
                .brand("Toyota")
                .model("Camry")
                .year(2020)
                .mileage(50000)
                .price(new BigDecimal("2500000"))
                .build();

        // Act - сериализация и десериализация
        String json = mapper.writeValueAsString(original);
        CarDto deserialized = mapper.readValue(json, CarDto.class);

        // Assert - проверяем основные поля
        assertEquals(original.id(), deserialized.id());
        assertEquals(original.brand(), deserialized.brand());
        assertEquals(original.model(), deserialized.model());
        assertEquals(original.year(), deserialized.year());
        assertEquals(original.mileage(), deserialized.mileage());
        assertEquals(original.price(), deserialized.price());
    }

    @Test
    void testCarDtoBuilder() {
        // Act - создаем через билдер
        CarDto car = CarDto.builder()
                .brand("Toyota")
                .model("Camry")
                .year(2020)
                .mileage(50000)
                .price(new BigDecimal("2500000"))
                .build();

        // Assert - проверяем, что билдер работает
        assertNotNull(car);
        assertEquals("Toyota", car.brand());
        assertEquals("Camry", car.model());
        assertEquals(2020, car.year());
        assertEquals(50000, car.mileage());
        assertEquals(new BigDecimal("2500000"), car.price());
    }

    @Test
    void testCarDtoWithNullFields() {
        // Act - создаем DTO только с обязательными полями
        CarDto car = CarDto.builder()
                .brand("Toyota")
                .model("Camry")
                .year(2020)
                .mileage(50000)
                .price(new BigDecimal("2500000"))
                .build();

        // Assert - проверяем, что null поля остаются null
        assertNull(car.description());
        assertNull(car.exteriorColor());
        assertNull(car.photoUrls());
    }
}