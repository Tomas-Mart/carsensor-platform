package com.carsensor.platform.dto.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.carsensor.platform.dto.CarDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Юнит-тесты для PageResponseFactory.
 *
 * <p><b>Проверяемые аспекты:</b>
 * <ul>
 *   <li>Создание страниц из Spring Page</li>
 *   <li>Создание пустых страниц</li>
 *   <li>Создание страниц с кастомными параметрами</li>
 *   <li>Расчет метаданных (totalPages, first, last, empty)</li>
 * </ul>
 *
 * @see PageResponseFactory
 * @since 1.0
 */
@DisplayName("Тесты PageResponseFactory")
class PageResponseFactoryTest {

    private static final List<CarDto> TEST_CARS = List.of(
            new CarDto(1L, "Toyota", "Camry", 2020, 50000, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
            new CarDto(2L, "Honda", "Accord", 2021, 30000, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
    );

    // ============================================================
    // ТЕСТЫ fromPage()
    // ============================================================

    @Nested
    @DisplayName("fromPage() - создание из Spring Page")
    class FromPageTests {

        @Test
        @DisplayName("✅ Создание PageResponse из Spring Page с данными")
        void should_CreatePageResponse_FromSpringPage() {
            // given
            var pageable = PageRequest.of(0, 10);
            var springPage = new PageImpl<>(TEST_CARS, pageable, 2L);

            // when
            var response = PageResponseFactory.fromPage(springPage);

            // then
            assertThat(response)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.content()).hasSize(2);
                        assertThat(r.totalElements()).isEqualTo(2L);
                        assertThat(r.totalPages()).isEqualTo(1);
                        assertThat(r.currentPage()).isEqualTo(0);
                        assertThat(r.pageSize()).isEqualTo(10);
                        assertThat(r.first()).isTrue();
                        assertThat(r.last()).isTrue();
                        assertThat(r.empty()).isFalse();
                    });
        }

        @Test
        @DisplayName("✅ Создание PageResponse из пустого Spring Page")
        void should_CreatePageResponse_FromEmptySpringPage() {
            // given
            var pageable = PageRequest.of(0, 10);
            var springPage = new PageImpl<>(List.of(), pageable, 0L);

            // when
            var response = PageResponseFactory.fromPage(springPage);

            // then
            assertThat(response)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.content()).isEmpty();
                        assertThat(r.totalElements()).isZero();
                        assertThat(r.totalPages()).isZero();
                        assertThat(r.currentPage()).isZero();
                        assertThat(r.first()).isTrue();
                        assertThat(r.last()).isTrue();
                        assertThat(r.empty()).isTrue();
                    });
        }
    }

    // ============================================================
    // ТЕСТЫ empty()
    // ============================================================

    @Nested
    @DisplayName("empty() - создание пустых страниц")
    class EmptyTests {

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
            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
            assertThat(response.totalPages()).isZero();
            assertThat(response.first()).isTrue();
            assertThat(response.last()).isTrue();
            assertThat(response.empty()).isTrue();
        }
    }

    // ============================================================
    // ТЕСТЫ of()
    // ============================================================

    @Nested
    @DisplayName("of() - создание страницы с кастомными параметрами")
    class OfTests {

        @Test
        @DisplayName("✅ Создание страницы с данными (первая страница)")
        void should_CreatePage_WithData_FirstPage() {
            // when
            var response = PageResponseFactory.of(TEST_CARS, 100L, 0, 10);

            // then
            assertThat(response.content()).hasSize(2);
            assertThat(response.totalElements()).isEqualTo(100L);
            assertThat(response.totalPages()).isEqualTo(10);
            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.pageSize()).isEqualTo(10);
            assertThat(response.first()).isTrue();
            assertThat(response.last()).isFalse();
            assertThat(response.empty()).isFalse();
        }

        @Test
        @DisplayName("✅ Создание страницы с данными (последняя страница)")
        void should_CreatePage_WithData_LastPage() {
            // when
            var response = PageResponseFactory.of(TEST_CARS, 100L, 9, 10);

            // then
            assertThat(response.currentPage()).isEqualTo(9);
            assertThat(response.first()).isFalse();
            assertThat(response.last()).isTrue();
        }

        @Test
        @DisplayName("✅ Создание страницы с данными (средняя страница)")
        void should_CreatePage_WithData_MiddlePage() {
            // when
            var response = PageResponseFactory.of(TEST_CARS, 100L, 5, 10);

            // then
            assertThat(response.currentPage()).isEqualTo(5);
            assertThat(response.first()).isFalse();
            assertThat(response.last()).isFalse();
        }

        @Test
        @DisplayName("✅ Создание страницы с пустым content")
        void should_CreatePage_WithEmptyContent() {
            // when
            var response = PageResponseFactory.of(List.of(), 0L, 0, 10);

            // then
            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
            assertThat(response.totalPages()).isZero();
            assertThat(response.first()).isTrue();
            assertThat(response.last()).isTrue();
            assertThat(response.empty()).isTrue();
        }

        @Test
        @DisplayName("✅ Создание страницы с null content")
        void should_CreatePage_WithNullContent() {
            // when
            var response = PageResponseFactory.of(null, 0L, 0, 10);

            // then
            assertThat(response.content()).isEmpty();
        }

        @Test
        @DisplayName("✅ Расчет totalPages при неполной странице")
        void should_CalculateTotalPages_Correctly() {
            // when
            var response = PageResponseFactory.of(TEST_CARS, 5L, 0, 10);

            // then
            assertThat(response.totalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("✅ Расчет totalPages при точном делении")
        void should_CalculateTotalPages_WhenExactDivision() {
            // when
            var response = PageResponseFactory.of(TEST_CARS, 100L, 0, 10);

            // then
            assertThat(response.totalPages()).isEqualTo(10);
        }
    }

    // ============================================================
    // ТЕСТЫ fromList()
    // ============================================================

    @Nested
    @DisplayName("fromList() - создание страницы из списка")
    class FromListTests {

        @Test
        @DisplayName("✅ fromList() создает страницу из списка")
        void should_CreatePage_FromList() {
            // when
            var response = PageResponseFactory.fromList(TEST_CARS, 100L, 2, 10);

            // then
            assertThat(response.content()).hasSize(2);
            assertThat(response.totalElements()).isEqualTo(100L);
            assertThat(response.currentPage()).isEqualTo(2);
            assertThat(response.pageSize()).isEqualTo(10);
        }
    }

    // ============================================================
    // ТЕСТЫ firstPage()
    // ============================================================

    @Nested
    @DisplayName("firstPage() - создание первой страницы")
    class FirstPageTests {

        @Test
        @DisplayName("✅ firstPage() создает первую страницу")
        void should_CreateFirstPage() {
            // when
            var response = PageResponseFactory.firstPage(TEST_CARS, 100L, 10);

            // then
            assertThat(response.currentPage()).isZero();
            assertThat(response.first()).isTrue();
            assertThat(response.content()).hasSize(2);
            assertThat(response.totalElements()).isEqualTo(100L);
            assertThat(response.pageSize()).isEqualTo(10);
        }
    }

    // ============================================================
// ТЕСТЫ КОНСТРУКТОРА
// ============================================================

    @Nested
    @DisplayName("Конструктор фабрики")
    class ConstructorTests {

        @Test
        @DisplayName("✅ Приватный конструктор выбрасывает UnsupportedOperationException")
        void should_ThrowException_WhenConstructorCalled() throws Exception {
            // given
            var constructor = PageResponseFactory.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            // when & then
            assertThatThrownBy(constructor::newInstance)
                    .isInstanceOf(InvocationTargetException.class)
                    .satisfies(e -> {
                        assertThat(e.getCause())
                                .isInstanceOf(UnsupportedOperationException.class);
                        assertThat(e.getCause().getMessage())
                                .isEqualTo("Utility class - do not instantiate");
                    });
        }
    }
}