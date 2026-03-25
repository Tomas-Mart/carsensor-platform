package com.carsensor.car.unit.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import com.carsensor.car.application.mapper.CarMapper;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.platform.dto.CarDto;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты CarMapper")
class CarMapperTest {

    private CarMapper carMapper;

    @BeforeEach
    void setUp() {
        carMapper = Mappers.getMapper(CarMapper.class);
    }

    @Test
    @DisplayName("Маппинг Car -> CarDto")
    void toDto_ShouldMapAllFields() {
        // Arrange
        Car car = Car.builder()
                .id(1L)
                .brand("Toyota")
                .model("Camry")
                .year(2022)
                .mileage(15000)
                .price(new BigDecimal("2500000"))
                .transmission("AT")
                .driveType("2WD")
                .createdAt(LocalDateTime.now())
                .build();

        // Act
        CarDto dto = carMapper.toDto(car);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(car.getId());
        assertThat(dto.brand()).isEqualTo(car.getBrand());
        assertThat(dto.model()).isEqualTo(car.getModel());
        assertThat(dto.year()).isEqualTo(car.getYear());
        assertThat(dto.mileage()).isEqualTo(car.getMileage());
        assertThat(dto.price()).isEqualTo(car.getPrice());
        assertThat(dto.transmission()).isEqualTo(car.getTransmission());
        assertThat(dto.driveType()).isEqualTo(car.getDriveType());
    }

    @Test
    @DisplayName("Маппинг CarDto -> Car (игнорируемые поля не маппятся)")
    void toEntity_ShouldIgnoreIdAndTimestamps() {
        // Arrange
        CarDto dto = CarDto.builder()
                .id(999L)
                .brand("Honda")
                .model("Accord")
                .year(2021)
                .mileage(20000)
                .price(new BigDecimal("1800000"))
                .build();

        // Act
        Car car = carMapper.toEntity(dto);

        // Assert
        assertThat(car).isNotNull();
        assertThat(car.getId()).isNull(); // игнорируется
        assertThat(car.getBrand()).isEqualTo(dto.brand());
        assertThat(car.getModel()).isEqualTo(dto.model());
        assertThat(car.getYear()).isEqualTo(dto.year());
        assertThat(car.getCreatedAt()).isNull(); // игнорируется
    }

    @Test
    @DisplayName("Обновление существующего Car из DTO")
    void updateEntityFromDto_ShouldUpdateOnlyNonNullFields() {
        // Arrange
        Car car = Car.builder()
                .id(1L)
                .brand("Toyota")
                .model("Corolla")
                .year(2020)
                .mileage(30000)
                .price(new BigDecimal("1500000"))
                .build();

        CarDto dto = CarDto.builder()
                .brand("Toyota") // то же значение
                .model("Corolla") // то же значение
                .year(2021) // обновлено
                .mileage(31000) // обновлено
                .build();

        // Act
        carMapper.updateEntityFromDto(dto, car);

        // Assert
        assertThat(car.getYear()).isEqualTo(2021);
        assertThat(car.getMileage()).isEqualTo(31000);
        assertThat(car.getBrand()).isEqualTo("Toyota"); // не изменилось
        assertThat(car.getPrice()).isEqualTo(new BigDecimal("1500000")); // не изменилось
    }
}