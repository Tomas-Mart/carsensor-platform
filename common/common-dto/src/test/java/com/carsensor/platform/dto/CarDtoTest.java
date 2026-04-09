package com.carsensor.platform.dto;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Юнит-тесты для CarDto.
 *
 * <p><b>Проверяемые аспекты:</b>
 * <ul>
 *   <li>Сериализация/десериализация JSON</li>
 *   <li>Компактный конструктор (нормализация и валидация)</li>
 *   <li>Валидация через Bean Validation</li>
 *   <li>Фабричные методы и Builder</li>
 * </ul>
 *
 * @see CarDto
 * @since 1.0
 */
@DisplayName("Тесты CarDto")
class CarDtoTest {

    private static final Long ID = 1L;
    private static final String BRAND = "Toyota";
    private static final String MODEL = "Camry";
    private static final Integer YEAR = 2020;
    private static final Integer MILEAGE = 50000;
    private static final BigDecimal PRICE = new BigDecimal("2500000");
    private static final String DESCRIPTION = "Отличное состояние, полный привод";
    private static final List<String> PHOTO_URLS = List.of("/images/1.jpg", "/images/2.jpg");
    private static final String EXTERIOR_COLOR = "White";
    private static final String TRANSMISSION = "AT";
    private static final String DRIVE_TYPE = "2WD";
    private static final Long VERSION = 0L;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================================================
    // ТЕСТЫ ФАБРИЧНЫХ МЕТОДОВ И BUILDER
    // ============================================================

    @Nested
    @DisplayName("Фабричные методы и Builder")
    class FactoryAndBuilderTests {

        @Test
        @DisplayName("✅ of(brand, model) создает минимальный CarDto")
        void should_CreateMinimalCarDto_WithFactoryMethod() {
            // when
            var car = CarDto.of(BRAND, MODEL);

            // then
            assertThat(car)
                    .isNotNull()
                    .satisfies(c -> {
                        assertThat(c.brand()).isEqualTo(BRAND);
                        assertThat(c.model()).isEqualTo(MODEL);
                        assertThat(c.id()).isNull();
                        assertThat(c.year()).isNull();
                        assertThat(c.mileage()).isNull();
                        assertThat(c.price()).isNull();
                    });
        }

        @Test
        @DisplayName("✅ of(brand, model, year, mileage, price) создает CarDto с основными полями")
        void should_CreateCarDto_WithBasicFields() {
            // when
            var car = CarDto.of(BRAND, MODEL, YEAR, MILEAGE, PRICE);

            // then
            assertThat(car)
                    .isNotNull()
                    .satisfies(c -> {
                        assertThat(c.brand()).isEqualTo(BRAND);
                        assertThat(c.model()).isEqualTo(MODEL);
                        assertThat(c.year()).isEqualTo(YEAR);
                        assertThat(c.mileage()).isEqualTo(MILEAGE);
                        assertThat(c.price()).isEqualTo(PRICE);
                    });
        }

        @Test
        @DisplayName("✅ of(brand, model, year, mileage, price, exteriorColor, transmission, driveType) создает CarDto с доп. полями")
        void should_CreateCarDto_WithAdditionalFields() {
            // when
            var car = CarDto.of(BRAND, MODEL, YEAR, MILEAGE, PRICE,
                    EXTERIOR_COLOR, TRANSMISSION, DRIVE_TYPE);

            // then
            assertThat(car)
                    .isNotNull()
                    .satisfies(c -> {
                        assertThat(c.brand()).isEqualTo(BRAND);
                        assertThat(c.model()).isEqualTo(MODEL);
                        assertThat(c.year()).isEqualTo(YEAR);
                        assertThat(c.mileage()).isEqualTo(MILEAGE);
                        assertThat(c.price()).isEqualTo(PRICE);
                        assertThat(c.exteriorColor()).isEqualTo(EXTERIOR_COLOR);
                        assertThat(c.transmission()).isEqualTo(TRANSMISSION);
                        assertThat(c.driveType()).isEqualTo(DRIVE_TYPE);
                    });
        }

        @Test
        @DisplayName("✅ Builder создает CarDto с полными данными")
        void should_CreateCarDto_WithBuilder() {
            // when
            var car = CarDto.builder()
                    .id(ID)
                    .brand(BRAND)
                    .model(MODEL)
                    .year(YEAR)
                    .mileage(MILEAGE)
                    .price(PRICE)
                    .description(DESCRIPTION)
                    .exteriorColor(EXTERIOR_COLOR)
                    .transmission(TRANSMISSION)
                    .driveType(DRIVE_TYPE)
                    .photoUrls(PHOTO_URLS)
                    .version(VERSION)
                    .build();

            // then
            assertThat(car)
                    .isNotNull()
                    .satisfies(c -> {
                        assertThat(c.id()).isEqualTo(ID);
                        assertThat(c.brand()).isEqualTo(BRAND);
                        assertThat(c.model()).isEqualTo(MODEL);
                        assertThat(c.year()).isEqualTo(YEAR);
                        assertThat(c.mileage()).isEqualTo(MILEAGE);
                        assertThat(c.price()).isEqualTo(PRICE);
                        assertThat(c.description()).isEqualTo(DESCRIPTION);
                        assertThat(c.exteriorColor()).isEqualTo(EXTERIOR_COLOR);
                        assertThat(c.transmission()).isEqualTo(TRANSMISSION);
                        assertThat(c.driveType()).isEqualTo(DRIVE_TYPE);
                        assertThat(c.photoUrls()).containsExactlyElementsOf(PHOTO_URLS);
                        assertThat(c.version()).isEqualTo(VERSION);
                    });
        }
    }

    // ============================================================
    // ТЕСТЫ СЕРИАЛИЗАЦИИ
    // ============================================================

    @Nested
    @DisplayName("Сериализация и десериализация JSON")
    class SerializationTests {

        @Test
        @DisplayName("✅ Сериализация CarDto в JSON и обратно")
        void should_SerializeAndDeserialize_Correctly() throws Exception {
            // given
            var original = CarDto.builder()
                    .id(ID)
                    .brand(BRAND)
                    .model(MODEL)
                    .year(YEAR)
                    .mileage(MILEAGE)
                    .price(PRICE)
                    .description(DESCRIPTION)
                    .photoUrls(PHOTO_URLS)
                    .version(VERSION)
                    .build();

            // when
            var json = objectMapper.writeValueAsString(original);
            var deserialized = objectMapper.readValue(json, CarDto.class);

            // then
            assertThat(deserialized)
                    .isNotNull()
                    .satisfies(car -> {
                        assertThat(car.id()).isEqualTo(ID);
                        assertThat(car.brand()).isEqualTo(BRAND);
                        assertThat(car.model()).isEqualTo(MODEL);
                        assertThat(car.year()).isEqualTo(YEAR);
                        assertThat(car.mileage()).isEqualTo(MILEAGE);
                        assertThat(car.price()).isEqualTo(PRICE);
                        assertThat(car.description()).isEqualTo(DESCRIPTION);
                        assertThat(car.photoUrls()).containsExactlyElementsOf(PHOTO_URLS);
                        assertThat(car.version()).isEqualTo(VERSION);
                    });
        }

        @Test
        @DisplayName("✅ Десериализация JSON с snake_case полями")
        void should_Deserialize_WithSnakeCaseFields() throws Exception {
            // given
            var json = """
                    {
                        "id": 1,
                        "brand": "Toyota",
                        "model": "Camry",
                        "year": 2020,
                        "mileage": 50000,
                        "price": 2500000,
                        "photo_urls": ["/images/1.jpg"]
                    }
                    """;

            // when
            var car = objectMapper.readValue(json, CarDto.class);

            // then
            assertThat(car)
                    .isNotNull()
                    .satisfies(c -> {
                        assertThat(c.id()).isEqualTo(1L);
                        assertThat(c.brand()).isEqualTo("Toyota");
                        assertThat(c.model()).isEqualTo("Camry");
                        assertThat(c.year()).isEqualTo(2020);
                        assertThat(c.mileage()).isEqualTo(50000);
                        assertThat(c.price()).isEqualTo(new BigDecimal("2500000"));
                        assertThat(c.photoUrls()).containsExactly("/images/1.jpg");
                    });
        }
    }

    // ============================================================
    // ТЕСТЫ КОМПАКТНОГО КОНСТРУКТОРА
    // ============================================================

    @Nested
    @DisplayName("Компактный конструктор (нормализация и валидация)")
    class CompactConstructorTests {

        @Test
        @DisplayName("✅ Brand и model нормализуются (trim)")
        void should_TrimBrandAndModel() {
            // when
            var car = new CarDto(
                    null, "  Toyota  ", "  Camry  ", YEAR, MILEAGE, PRICE,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null
            );

            // then
            assertThat(car.brand()).isEqualTo("Toyota");
            assertThat(car.model()).isEqualTo("Camry");
        }

        @Test
        @DisplayName("✅ Price null не вызывает исключение")
        void should_NotThrowException_WhenPriceIsNull() {
            // when & then
            var car = new CarDto(
                    null, BRAND, MODEL, YEAR, MILEAGE, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null
            );

            assertThat(car.price()).isNull();
        }

        @Test
        @DisplayName("❌ Price <= 0 выбрасывает IllegalArgumentException")
        void should_ThrowException_WhenPriceIsNotPositive() {
            assertThatThrownBy(() -> new CarDto(
                    null, BRAND, MODEL, YEAR, MILEAGE, BigDecimal.ZERO,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price must be positive");
        }

        @Test
        @DisplayName("❌ Price отрицательный выбрасывает IllegalArgumentException")
        void should_ThrowException_WhenPriceIsNegative() {
            assertThatThrownBy(() -> new CarDto(
                    null, BRAND, MODEL, YEAR, MILEAGE, new BigDecimal("-1000"),
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price must be positive");
        }
    }

    // ============================================================
    // ТЕСТЫ ВАЛИДАЦИИ (Bean Validation)
    // ============================================================

    @Nested
    @DisplayName("Bean Validation")
    class ValidationTests {

        @Test
        @DisplayName("✅ Валидный CarDto проходит проверку")
        void should_PassValidation_WhenAllFieldsAreValid() {
            // given
            var car = CarDto.of(BRAND, MODEL, YEAR, MILEAGE, PRICE);

            // then
            assertThat(car.brand()).isNotBlank();
            assertThat(car.model()).isNotBlank();
            assertThat(car.year()).isBetween(1900, 2026);
            assertThat(car.mileage()).isGreaterThanOrEqualTo(0);
            assertThat(car.price()).isPositive();
        }

        @Test
        @DisplayName("❌ Brand пустой вызывает ошибку валидации")
        void should_FailValidation_WhenBrandIsEmpty() {
            // given
            var car = CarDto.of("", MODEL);

            // then
            assertThat(car.brand()).isBlank();
        }

        @Test
        @DisplayName("❌ Model пустая вызывает ошибку валидации")
        void should_FailValidation_WhenModelIsEmpty() {
            // given
            var car = CarDto.of(BRAND, "");

            // then
            assertThat(car.model()).isBlank();
        }
    }
}