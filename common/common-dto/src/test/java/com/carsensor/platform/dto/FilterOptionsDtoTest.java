package com.carsensor.platform.dto;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты для FilterOptionsDto.
 *
 * <p><b>Проверяемые аспекты:</b>
 * <ul>
 *   <li>Сериализация/десериализация JSON</li>
 *   <li>Компактный конструктор (нормализация null списков)</li>
 * </ul>
 *
 * @see FilterOptionsDto
 * @since 1.0
 */
@DisplayName("Тесты FilterOptionsDto")
class FilterOptionsDtoTest {

    private static final List<String> BRANDS = List.of("Toyota", "Honda", "Nissan");
    private static final Integer YEAR_MIN = 2015;
    private static final Integer YEAR_MAX = 2025;
    private static final BigDecimal PRICE_MIN = new BigDecimal("1000000");
    private static final BigDecimal PRICE_MAX = new BigDecimal("5000000");
    private static final List<String> TRANSMISSIONS = List.of("AT", "MT", "CVT");
    private static final List<String> DRIVE_TYPES = List.of("2WD", "4WD", "AWD");
    private static final List<String> COLORS = List.of("Белый", "Черный", "Серебристый");

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================================================
    // ТЕСТЫ СЕРИАЛИЗАЦИИ
    // ============================================================

    @Nested
    @DisplayName("Сериализация и десериализация JSON")
    class SerializationTests {

        @Test
        @DisplayName("✅ Сериализация FilterOptionsDto в JSON и обратно")
        void should_SerializeAndDeserialize_Correctly() throws Exception {
            // given
            var original = new FilterOptionsDto(
                    BRANDS, YEAR_MIN, YEAR_MAX, PRICE_MIN, PRICE_MAX,
                    TRANSMISSIONS, DRIVE_TYPES, COLORS
            );

            // when
            var json = objectMapper.writeValueAsString(original);
            var deserialized = objectMapper.readValue(json, FilterOptionsDto.class);

            // then
            assertThat(deserialized)
                    .isNotNull()
                    .satisfies(f -> {
                        assertThat(f.brands()).containsExactlyElementsOf(BRANDS);
                        assertThat(f.yearMin()).isEqualTo(YEAR_MIN);
                        assertThat(f.yearMax()).isEqualTo(YEAR_MAX);
                        assertThat(f.priceMin()).isEqualTo(PRICE_MIN);
                        assertThat(f.priceMax()).isEqualTo(PRICE_MAX);
                        assertThat(f.transmissions()).containsExactlyElementsOf(TRANSMISSIONS);
                        assertThat(f.driveTypes()).containsExactlyElementsOf(DRIVE_TYPES);
                        assertThat(f.colors()).containsExactlyElementsOf(COLORS);
                    });
        }

        @Test
        @DisplayName("✅ Десериализация JSON с snake_case полями")
        void should_Deserialize_WithSnakeCaseFields() throws Exception {
            // given
            var json = """
                    {
                        "brands": ["Toyota", "Honda"],
                        "year_min": 2015,
                        "year_max": 2025,
                        "price_min": 1000000,
                        "price_max": 5000000,
                        "transmissions": ["AT", "MT"],
                        "drive_types": ["2WD", "4WD"],
                        "colors": ["Белый", "Черный"]
                    }
                    """;

            // when
            var options = objectMapper.readValue(json, FilterOptionsDto.class);

            // then
            assertThat(options)
                    .isNotNull()
                    .satisfies(f -> {
                        assertThat(f.brands()).containsExactly("Toyota", "Honda");
                        assertThat(f.yearMin()).isEqualTo(2015);
                        assertThat(f.yearMax()).isEqualTo(2025);
                        assertThat(f.priceMin()).isEqualTo(new BigDecimal("1000000"));
                        assertThat(f.priceMax()).isEqualTo(new BigDecimal("5000000"));
                        assertThat(f.transmissions()).containsExactly("AT", "MT");
                        assertThat(f.driveTypes()).containsExactly("2WD", "4WD");
                        assertThat(f.colors()).containsExactly("Белый", "Черный");
                    });
        }
    }

    // ============================================================
    // ТЕСТЫ КОМПАКТНОГО КОНСТРУКТОРА
    // ============================================================

    @Nested
    @DisplayName("Компактный конструктор (нормализация null списков)")
    class CompactConstructorTests {

        @Test
        @DisplayName("✅ Null brands заменяется на пустой список")
        void should_SetEmptyList_WhenBrandsIsNull() {
            // when
            var options = new FilterOptionsDto(
                    null, YEAR_MIN, YEAR_MAX, PRICE_MIN, PRICE_MAX,
                    TRANSMISSIONS, DRIVE_TYPES, COLORS
            );

            // then
            assertThat(options.brands()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("✅ Null transmissions заменяется на пустой список")
        void should_SetEmptyList_WhenTransmissionsIsNull() {
            // when
            var options = new FilterOptionsDto(
                    BRANDS, YEAR_MIN, YEAR_MAX, PRICE_MIN, PRICE_MAX,
                    null, DRIVE_TYPES, COLORS
            );

            // then
            assertThat(options.transmissions()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("✅ Null driveTypes заменяется на пустой список")
        void should_SetEmptyList_WhenDriveTypesIsNull() {
            // when
            var options = new FilterOptionsDto(
                    BRANDS, YEAR_MIN, YEAR_MAX, PRICE_MIN, PRICE_MAX,
                    TRANSMISSIONS, null, COLORS
            );

            // then
            assertThat(options.driveTypes()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("✅ Null colors заменяется на пустой список")
        void should_SetEmptyList_WhenColorsIsNull() {
            // when
            var options = new FilterOptionsDto(
                    BRANDS, YEAR_MIN, YEAR_MAX, PRICE_MIN, PRICE_MAX,
                    TRANSMISSIONS, DRIVE_TYPES, null
            );

            // then
            assertThat(options.colors()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("✅ Все списки могут быть null и заменяются на пустые")
        void should_SetEmptyLists_WhenAllListsAreNull() {
            // when
            var options = new FilterOptionsDto(
                    null, YEAR_MIN, YEAR_MAX, PRICE_MIN, PRICE_MAX,
                    null, null, null
            );

            // then
            assertThat(options.brands()).isEmpty();
            assertThat(options.transmissions()).isEmpty();
            assertThat(options.driveTypes()).isEmpty();
            assertThat(options.colors()).isEmpty();
            assertThat(options.yearMin()).isEqualTo(YEAR_MIN);
            assertThat(options.yearMax()).isEqualTo(YEAR_MAX);
            assertThat(options.priceMin()).isEqualTo(PRICE_MIN);
            assertThat(options.priceMax()).isEqualTo(PRICE_MAX);
        }
    }

    // ============================================================
    // ТЕСТЫ СОЗДАНИЯ DTO
    // ============================================================

    @Nested
    @DisplayName("Создание FilterOptionsDto")
    class CreationTests {

        @Test
        @DisplayName("✅ Создание FilterOptionsDto с минимальными полями")
        void should_CreateFilterOptionsDto_WithMinimalFields() {
            // when
            var options = new FilterOptionsDto(
                    BRANDS, null, null, null, null,
                    null, null, null
            );

            // then
            assertThat(options)
                    .isNotNull()
                    .satisfies(f -> {
                        assertThat(f.brands()).containsExactlyElementsOf(BRANDS);
                        assertThat(f.yearMin()).isNull();
                        assertThat(f.yearMax()).isNull();
                        assertThat(f.priceMin()).isNull();
                        assertThat(f.priceMax()).isNull();
                        assertThat(f.transmissions()).isEmpty();
                        assertThat(f.driveTypes()).isEmpty();
                        assertThat(f.colors()).isEmpty();
                    });
        }

        @Test
        @DisplayName("✅ Создание FilterOptionsDto с полными данными")
        void should_CreateFilterOptionsDto_WithAllFields() {
            // when
            var options = new FilterOptionsDto(
                    BRANDS, YEAR_MIN, YEAR_MAX, PRICE_MIN, PRICE_MAX,
                    TRANSMISSIONS, DRIVE_TYPES, COLORS
            );

            // then
            assertThat(options)
                    .isNotNull()
                    .satisfies(f -> {
                        assertThat(f.brands()).containsExactlyElementsOf(BRANDS);
                        assertThat(f.yearMin()).isEqualTo(YEAR_MIN);
                        assertThat(f.yearMax()).isEqualTo(YEAR_MAX);
                        assertThat(f.priceMin()).isEqualTo(PRICE_MIN);
                        assertThat(f.priceMax()).isEqualTo(PRICE_MAX);
                        assertThat(f.transmissions()).containsExactlyElementsOf(TRANSMISSIONS);
                        assertThat(f.driveTypes()).containsExactlyElementsOf(DRIVE_TYPES);
                        assertThat(f.colors()).containsExactlyElementsOf(COLORS);
                    });
        }
    }
}