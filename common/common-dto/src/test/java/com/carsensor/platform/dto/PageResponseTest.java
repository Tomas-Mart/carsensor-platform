package com.carsensor.platform.dto;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.carsensor.platform.dto.factory.PageResponseFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Юнит-тесты для PageResponse.
 *
 * <p><b>Проверяемые аспекты:</b>
 * <ul>
 *   <li>Сериализация/десериализация JSON</li>
 *   <li>Компактный конструктор (нормализация и валидация)</li>
 *   <li>Фабричные методы PageResponseFactory</li>
 * </ul>
 *
 * @see PageResponse
 * @see PageResponseFactory
 * @since 1.0
 */
@DisplayName("Тесты PageResponse")
class PageResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<CarDto> TEST_CARS = List.of(
            new CarDto(1L, "Toyota", "Camry", 2020, 50000, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
            new CarDto(2L, "Honda", "Accord", 2021, 30000, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
    );

    // ============================================================
    // ТЕСТЫ СЕРИАЛИЗАЦИИ
    // ============================================================

    @Nested
    @DisplayName("Сериализация и десериализация JSON")
    class SerializationTests {

        @Test
        @DisplayName("✅ Сериализация PageResponse в JSON и обратно")
        void should_SerializeAndDeserialize_Correctly() throws Exception {
            // given
            var original = new PageResponse<>(
                    TEST_CARS,
                    2L,
                    1,
                    0,
                    10,
                    true,
                    false,
                    false
            );

            // when
            var json = objectMapper.writeValueAsString(original);
            var type = TypeFactory.defaultInstance()
                    .constructParametricType(PageResponse.class, CarDto.class);
            PageResponse<CarDto> deserialized = objectMapper.readValue(json, type);

            // then
            assertThat(deserialized)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.totalElements()).isEqualTo(2L);
                        assertThat(r.totalPages()).isEqualTo(1);
                        assertThat(r.currentPage()).isEqualTo(0);
                        assertThat(r.pageSize()).isEqualTo(10);
                        assertThat(r.first()).isTrue();
                        assertThat(r.last()).isFalse();
                        assertThat(r.empty()).isFalse();
                        assertThat(r.content()).hasSize(2);
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
        @DisplayName("✅ Null content заменяется на пустой список")
        void should_SetEmptyList_WhenContentIsNull() {
            // when
            var response = new PageResponse<>(
                    null,
                    0,
                    0,
                    0,
                    10,
                    true,
                    true,
                    true
            );

            // then
            assertThat(response.content()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("❌ pageSize <= 0 выбрасывает IllegalArgumentException")
        void should_ThrowException_WhenPageSizeIsNotPositive() {
            assertThatThrownBy(() -> new PageResponse<>(
                    List.of(),
                    0,
                    0,
                    0,
                    0,
                    true,
                    true,
                    true
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pageSize must be positive");
        }

        @Test
        @DisplayName("❌ totalPages < 0 выбрасывает IllegalArgumentException")
        void should_ThrowException_WhenTotalPagesIsNegative() {
            assertThatThrownBy(() -> new PageResponse<>(
                    List.of(),
                    0,
                    -1,
                    0,
                    10,
                    true,
                    true,
                    true
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("totalPages cannot be negative");
        }

        @Test
        @DisplayName("❌ totalElements < 0 выбрасывает IllegalArgumentException")
        void should_ThrowException_WhenTotalElementsIsNegative() {
            assertThatThrownBy(() -> new PageResponse<>(
                    List.of(),
                    -1,
                    0,
                    0,
                    10,
                    true,
                    true,
                    true
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("totalElements cannot be negative");
        }
    }

    // ============================================================
    // ТЕСТЫ ФАБРИЧНЫХ МЕТОДОВ
    // ============================================================

    @Nested
    @DisplayName("Фабричные методы PageResponseFactory")
    class FactoryMethodTests {

        @Test
        @DisplayName("✅ empty() создает пустую страницу с размером 10")
        void should_CreateEmptyPage_WithDefaultPageSize() {
            // when
            var response = PageResponseFactory.empty();

            // then
            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
            assertThat(response.totalPages()).isZero();
            assertThat(response.currentPage()).isZero();
            assertThat(response.pageSize()).isEqualTo(10);
            assertThat(response.first()).isTrue();
            assertThat(response.last()).isTrue();
            assertThat(response.empty()).isTrue();
        }

        @Test
        @DisplayName("✅ empty(pageSize) создает пустую страницу с указанным размером")
        void should_CreateEmptyPage_WithCustomPageSize() {
            // when
            var response = PageResponseFactory.empty(25);

            // then
            assertThat(response.pageSize()).isEqualTo(25);
        }

        @Test
        @DisplayName("✅ of() создает страницу с кастомными параметрами")
        void should_CreatePage_WithCustomParameters() {
            // when
            var response = PageResponseFactory.of(TEST_CARS, 100L, 2, 10);

            // then
            assertThat(response.content()).hasSize(2);
            assertThat(response.totalElements()).isEqualTo(100L);
            assertThat(response.totalPages()).isEqualTo(10);
            assertThat(response.currentPage()).isEqualTo(2);
            assertThat(response.pageSize()).isEqualTo(10);
            assertThat(response.first()).isFalse();
            assertThat(response.last()).isFalse();
        }

        @Test
        @DisplayName("✅ firstPage() создает первую страницу")
        void should_CreateFirstPage() {
            // when
            var response = PageResponseFactory.firstPage(TEST_CARS, 100L, 10);

            // then
            assertThat(response.currentPage()).isZero();
            assertThat(response.first()).isTrue();
        }

        @Test
        @DisplayName("✅ fromList() создает страницу из списка")
        void should_CreatePageFromList() {
            // when
            var response = PageResponseFactory.fromList(TEST_CARS, 100L, 2, 10);

            // then
            assertThat(response.content()).hasSize(2);
            assertThat(response.totalElements()).isEqualTo(100L);
        }
    }
}