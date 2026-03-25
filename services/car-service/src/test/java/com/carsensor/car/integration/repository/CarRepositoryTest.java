package com.carsensor.car.integration.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.common.test.AbstractJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для CarRepository.
 *
 * <p>Наследует AbstractJpaTest для получения embedded PostgreSQL,
 * но добавляет @DataJpaTest для загрузки JPA контекста.
 */
@DataJpaTest
@DisplayName("Интеграционные тесты CarRepository")
class CarRepositoryTest extends AbstractJpaTest {

    @Autowired
    private CarRepository carRepository;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();

        Car testCar1 = createCar(
                "Toyota", "Camry", 2020, 50000, new BigDecimal("2500000"),
                "White", "AT", "2WD", LocalDateTime.now(), "TOYOTA_CAMRY_001"
        );

        Car testCar2 = createCar(
                "Toyota", "RAV4", 2021, 30000, new BigDecimal("3500000"),
                "Black", "CVT", "4WD", LocalDateTime.now().minusDays(1), "TOYOTA_RAV4_001"
        );

        Car testCar3 = createCar(
                "Honda", "Civic", 2022, 10000, new BigDecimal("1800000"),
                "Red", "MT", "2WD", LocalDateTime.now().minusDays(2), "HONDA_CIVIC_001"
        );

        carRepository.save(testCar1);
        carRepository.save(testCar2);
        carRepository.save(testCar3);
    }

    private Car createCar(String brand, String model, int year, int mileage, BigDecimal price,
                          String exteriorColor, String transmission, String driveType,
                          LocalDateTime parsedAt, String externalId) {
        return Car.builder()
                .brand(brand)
                .model(model)
                .year(year)
                .mileage(mileage)
                .price(price)
                .exteriorColor(exteriorColor)
                .transmission(transmission)
                .driveType(driveType)
                .parsedAt(parsedAt)
                .externalId(externalId)
                .build();
    }

    @Nested
    @DisplayName("Базовые CRUD операции")
    class BasicCrudTests {

        @Test
        @DisplayName("Сохранение автомобиля")
        void saveCar_ShouldPersistCar() {
            Car newCar = Car.builder()
                    .brand("Mazda")
                    .model("CX-5")
                    .year(2023)
                    .mileage(5000)
                    .price(new BigDecimal("2800000"))
                    .build();

            Car savedCar = carRepository.save(newCar);

            assertThat(savedCar.getId()).isNotNull();
            assertThat(savedCar.getBrand()).isEqualTo("Mazda");
            assertThat(savedCar.getModel()).isEqualTo("CX-5");
        }

        @Test
        @DisplayName("Поиск автомобиля по ID")
        void findById_ShouldReturnCar() {
            List<Car> allCars = carRepository.findAll();
            assertThat(allCars).isNotEmpty();

            Long firstCarId = allCars.getFirst().getId();
            Optional<Car> found = carRepository.findById(firstCarId);

            assertThat(found).isPresent();
            assertThat(found.get().getBrand()).isEqualTo("Toyota");
            assertThat(found.get().getModel()).isEqualTo("Camry");
        }

        @Test
        @DisplayName("Удаление автомобиля")
        void deleteById_ShouldRemoveCar() {
            List<Car> allCars = carRepository.findAll();
            assertThat(allCars).isNotEmpty();

            Long firstCarId = allCars.getFirst().getId();
            carRepository.deleteById(firstCarId);

            Optional<Car> found = carRepository.findById(firstCarId);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Получение всех автомобилей")
        void findAll_ShouldReturnAllCars() {
            List<Car> cars = carRepository.findAll();

            assertThat(cars).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Поиск по externalId")
    class FindByExternalIdTests {

        @Test
        @DisplayName("Поиск по существующему externalId")
        void findByExternalId_ExistingId_ShouldReturnCar() {
            Optional<Car> found = carRepository.findByExternalId("TOYOTA_CAMRY_001");

            assertThat(found).isPresent();
            assertThat(found.get().getBrand()).isEqualTo("Toyota");
            assertThat(found.get().getModel()).isEqualTo("Camry");
        }

        @Test
        @DisplayName("Поиск по несуществующему externalId")
        void findByExternalId_NonExistingId_ShouldReturnEmpty() {
            Optional<Car> found = carRepository.findByExternalId("NON_EXISTENT");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Проверка существования externalId")
        void existsByExternalId_ShouldReturnCorrectBoolean() {
            assertThat(carRepository.existsByExternalId("TOYOTA_CAMRY_001")).isTrue();
            assertThat(carRepository.existsByExternalId("NON_EXISTENT")).isFalse();
        }
    }

    @Nested
    @DisplayName("Статистические запросы")
    class StatisticsQueriesTests {

        @Test
        @DisplayName("Получение всех марок")
        void findAllBrands_ShouldReturnDistinctBrands() {
            List<String> brands = carRepository.findAllBrands();

            assertThat(brands).containsExactlyInAnyOrder("Toyota", "Honda");
        }

        @Test
        @DisplayName("Получение моделей по марке")
        void findModelsByBrand_ShouldReturnModelsForBrand() {
            List<String> toyotaModels = carRepository.findModelsByBrand("Toyota");

            assertThat(toyotaModels).containsExactlyInAnyOrder("Camry", "RAV4");

            List<String> hondaModels = carRepository.findModelsByBrand("Honda");
            assertThat(hondaModels).containsExactly("Civic");
        }

        @Test
        @DisplayName("Получение диапазона годов")
        void findYearRange_ShouldReturnMinAndMaxYears() {
            List<Object[]> yearRange = carRepository.findYearRange();

            assertThat(yearRange).isNotEmpty();
            Object[] range = yearRange.getFirst();
            assertThat(range[0]).isEqualTo(2020);
            assertThat(range[1]).isEqualTo(2022);
        }

        @Test
        @DisplayName("Получение диапазона цен")
        void findPriceRange_ShouldReturnMinAndMaxPrices() {
            List<Object[]> priceRange = carRepository.findPriceRange();

            assertThat(priceRange).isNotEmpty();
            Object[] range = priceRange.getFirst();

            assertThat((BigDecimal) range[0])
                    .isEqualByComparingTo(new BigDecimal("1800000"));
            assertThat((BigDecimal) range[1])
                    .isEqualByComparingTo(new BigDecimal("3500000"));
        }
    }

    @Nested
    @DisplayName("Запросы по времени")
    class TimeBasedQueriesTests {

        @Test
        @DisplayName("Поиск недавно спарсенных автомобилей")
        void findRecentlyParsed_ShouldReturnCarsParsedAfterDate() {
            LocalDateTime since = LocalDateTime.now().minusHours(12);
            List<Car> recentlyParsed = carRepository.findRecentlyParsed(since);

            assertThat(recentlyParsed).hasSize(1);
            assertThat(recentlyParsed.getFirst().getModel()).isEqualTo("Camry");
        }

        @Test
        @DisplayName("Подсчет недавно спарсенных автомобилей")
        void countRecentlyParsed_ShouldReturnCorrectCount() {
            LocalDateTime since = LocalDateTime.now().minusHours(12);
            long count = carRepository.countRecentlyParsed(since);

            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Спецификации и фильтрация")
    class SpecificationTests {

        @Test
        @DisplayName("Фильтрация по марке")
        void findAll_WithBrandSpecification_ShouldReturnFilteredCars() {
            Specification<Car> spec = (root, query, cb) ->
                    cb.equal(root.get("brand"), "Toyota");

            List<Car> toyotas = carRepository.findAll(spec);

            assertThat(toyotas).hasSize(2);
            assertThat(toyotas).allMatch(car -> car.getBrand().equals("Toyota"));
        }

        @Test
        @DisplayName("Фильтрация по диапазону цен")
        void findAll_WithPriceRangeSpecification_ShouldReturnFilteredCars() {
            Specification<Car> spec = (root, query, cb) ->
                    cb.between(root.get("price"), new BigDecimal("2000000"), new BigDecimal("3000000"));

            List<Car> carsInRange = carRepository.findAll(spec);

            assertThat(carsInRange).hasSize(1);
            assertThat(carsInRange.getFirst().getModel()).isEqualTo("Camry");
        }

        @Test
        @DisplayName("Пагинация результатов")
        void findAll_WithPagination_ShouldReturnCorrectPage() {
            PageRequest pageable = PageRequest.of(0, 2);
            Page<Car> page = carRepository.findAll(pageable);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }
    }
}