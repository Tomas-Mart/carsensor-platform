package com.carsensor.car.unit.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.carsensor.car.application.mapper.CarMapper;
import com.carsensor.car.application.service.command.CarCommandService;
import com.carsensor.car.application.service.command.impl.CarCommandServiceImpl;
import com.carsensor.car.application.service.export.CarExportImportService;
import com.carsensor.car.application.service.export.impl.CarExportImportServiceImpl;
import com.carsensor.car.application.service.filter.CarFilterOptionsService;
import com.carsensor.car.application.service.filter.impl.CarFilterOptionsServiceImpl;
import com.carsensor.car.application.service.query.CarQueryService;
import com.carsensor.car.application.service.query.impl.CarQueryServiceImpl;
import com.carsensor.car.application.service.statistics.CarStatisticsService;
import com.carsensor.car.application.service.statistics.impl.CarStatisticsServiceImpl;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.platform.dto.PageResponse;
import com.carsensor.platform.exception.PlatformException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты сервиса автомобилей")
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarMapper carMapper;

    private CarQueryService carQueryService;
    private CarCommandService carCommandService;
    private CarStatisticsService carStatisticsService;
    private CarFilterOptionsService carFilterOptionsService;
    private CarExportImportService carExportImportService;

    @Captor
    private ArgumentCaptor<Car> carCaptor;

    @Captor
    private ArgumentCaptor<Specification<Car>> specCaptor;

    private Car testCar;
    private CarDto testCarDto;
    private final Long CAR_ID = 1L;
    private final Long NON_EXISTENT_ID = 999L;

    @BeforeEach
    void setUp() {
        carQueryService = new CarQueryServiceImpl(carRepository, carMapper);
        carCommandService = new CarCommandServiceImpl(carRepository, carMapper);
        carStatisticsService = new CarStatisticsServiceImpl(carRepository);
        carFilterOptionsService = new CarFilterOptionsServiceImpl(carRepository);
        carExportImportService = new CarExportImportServiceImpl(carRepository, carCommandService);

        testCar = Car.builder()
                .id(CAR_ID)
                .brand("Toyota")
                .model("Camry")
                .year(2020)
                .mileage(50000)
                .price(new BigDecimal("2500000"))
                .exteriorColor("White")
                .transmission("AT")
                .driveType("2WD")
                .parsedAt(LocalDateTime.now())
                .build();

        testCarDto = new CarDto(
                CAR_ID, "Toyota", "Camry", 2020, 50000, new BigDecimal("2500000"),
                null, null, null, "White", null, null, "AT", "2WD",
                null, null, null, null, null, null, null, null
        );
    }

    private CarDto createCarDto(String brand, String model, int year, int mileage, BigDecimal price) {
        return new CarDto(null, brand, model, year, mileage, price,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    private CarDto createCarDtoWithExternalId(String brand, String model, String externalId) {
        return new CarDto(null, brand, model, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, externalId, null, null, null, null, null);
    }

    // ============================================================
    // ТЕСТЫ CAR QUERY SERVICE
    // ============================================================

    @Nested
    @DisplayName("CarQueryService - тесты чтения автомобилей")
    class CarQueryServiceTests {

        @Test
        @DisplayName("Получение автомобиля по ID - успешно")
        void getCarById_ExistingId_ReturnsCar() {
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));
            when(carMapper.toDto(testCar)).thenReturn(testCarDto);

            CarDto result = carQueryService.getCarById(CAR_ID);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(CAR_ID);
            assertThat(result.brand()).isEqualTo("Toyota");
            assertThat(result.model()).isEqualTo("Camry");

            verify(carRepository).findById(CAR_ID);
            verify(carMapper).toDto(testCar);
        }

        @Test
        @DisplayName("Получение автомобиля по несуществующему ID - исключение")
        void getCarById_NonExistingId_ThrowsException() {
            when(carRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carQueryService.getCarById(NON_EXISTENT_ID))
                    .isInstanceOf(PlatformException.CarNotFoundException.class);
        }

        @Test
        @DisplayName("Получение списка автомобилей с фильтрацией - успешно")
        void getCars_WithFilters_ReturnsPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Car> carPage = new PageImpl<>(List.of(testCar), pageable, 1);

            when(carRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(carPage);
            when(carMapper.toDto(any(Car.class))).thenReturn(testCarDto);

            PageResponse<CarDto> result = carQueryService.getCars(
                    "Toyota", null, 2020, 2021, null, null,
                    null, null, null, null, null, pageable
            );

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content().getFirst().brand()).isEqualTo("Toyota");
        }

        @Test
        @DisplayName("Получение пустого списка, когда автомобили не найдены")
        void getCars_NoCarsFound_ReturnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Car> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(carRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(emptyPage);

            PageResponse<CarDto> result = carQueryService.getCars(
                    "NonExistentBrand", null, null, null, null, null,
                    null, null, null, null, null, pageable
            );

            assertThat(result).isNotNull();
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("Получение автомобилей по марке - успешно")
        void getCarsByBrand_ReturnsList() {
            when(carRepository.findAll(any(Specification.class))).thenReturn(List.of(testCar));
            when(carMapper.toDto(testCar)).thenReturn(testCarDto);

            List<CarDto> result = carQueryService.getCarsByBrand("Toyota");

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().brand()).isEqualTo("Toyota");
        }

        @Test
        @DisplayName("Получение недавно спарсенных автомобилей")
        void getRecentlyParsedCars_ReturnsList() {
            when(carRepository.findRecentlyParsed(any(LocalDateTime.class))).thenReturn(List.of(testCar));
            when(carMapper.toDto(testCar)).thenReturn(testCarDto);

            List<CarDto> result = carQueryService.getRecentlyParsedCars(10);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Поиск похожих автомобилей")
        void findSimilarCars_ReturnsList() {
            // given
            Car similarCar = Car.builder()
                    .id(2L)
                    .brand("Toyota")
                    .model("Camry")
                    .year(2021)
                    .mileage(45000)
                    .price(new BigDecimal("2400000"))
                    .build();

            CarDto similarCarDto = new CarDto(
                    2L, "Toyota", "Camry", 2021, 45000, new BigDecimal("2400000"),
                    null, null, null, null, null, null, "AT", "2WD",
                    null, null, null, null, null, null, null, null
            );

            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));
            when(carRepository.findAll(any(Specification.class))).thenReturn(List.of(similarCar));
            when(carMapper.toDto(similarCar)).thenReturn(similarCarDto);

            // when
            List<CarDto> result = carQueryService.findSimilarCars(CAR_ID, 5);

            // then
            assertThat(result)
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(1);

            assertThat(result.getFirst().id()).isEqualTo(2L);
        }
    }
    // ============================================================
    // ТЕСТЫ CAR COMMAND SERVICE
    // ============================================================

    @Nested
    @DisplayName("CarCommandService - тесты записи автомобилей")
    class CarCommandServiceTests {

        @Test
        @DisplayName("Создание нового автомобиля - успешно")
        void createCar_ValidData_ReturnsCreatedCar() {
            CarDto newCarDto = createCarDto("Honda", "Civic", 2022, 10000, new BigDecimal("1800000"));
            Car newCar = Car.builder().brand("Honda").model("Civic").year(2022).mileage(10000).price(new BigDecimal("1800000")).build();
            Car savedCar = Car.builder().id(2L).brand("Honda").model("Civic").year(2022).mileage(10000).price(new BigDecimal("1800000")).build();
            CarDto expectedDto = createCarDto("Honda", "Civic", 2022, 10000, new BigDecimal("1800000"));

            when(carMapper.toEntity(newCarDto)).thenReturn(newCar);
            when(carRepository.save(any(Car.class))).thenReturn(savedCar);
            when(carMapper.toDto(savedCar)).thenReturn(expectedDto);

            CarDto result = carCommandService.createCar(newCarDto);

            assertThat(result).isNotNull();
            assertThat(result.brand()).isEqualTo("Honda");

            verify(carRepository).save(carCaptor.capture());
            assertThat(carCaptor.getValue().getParsedAt()).isNotNull();
        }

        @Test
        @DisplayName("Создание автомобиля с существующим externalId - исключение")
        void createCar_DuplicateExternalId_ThrowsException() {
            CarDto newCarDto = new CarDto(
                    null, "Honda", "Civic", 2022, 10000, new BigDecimal("1800000"),
                    null, null, null, null, null, null, null, null,
                    null, null, "ext123", null, null, null, null, null
            );

            when(carRepository.existsByExternalId("ext123")).thenReturn(true);

            assertThatThrownBy(() -> carCommandService.createCar(newCarDto))
                    .isInstanceOf(PlatformException.DuplicateResourceException.class);

            verify(carMapper, never()).toEntity(any());
        }

        @Test
        @DisplayName("Обновление существующего автомобиля - успешно")
        void updateCar_ExistingId_ReturnsUpdatedCar() {
            CarDto updateDto = createCarDto(null, "Camry Updated", 2021, 45000, new BigDecimal("2400000"));

            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));
            doAnswer(invocation -> {
                CarDto dto = invocation.getArgument(0);
                Car car = invocation.getArgument(1);
                if (dto.model() != null) car.setModel(dto.model());
                if (dto.year() != null) car.setYear(dto.year());
                if (dto.mileage() != null) car.setMileage(dto.mileage());
                if (dto.price() != null) car.setPrice(dto.price());
                return null;
            }).when(carMapper).updateEntity(eq(updateDto), any(Car.class));

            when(carRepository.save(any(Car.class))).thenReturn(testCar);
            when(carMapper.toDto(testCar)).thenReturn(updateDto);

            CarDto result = carCommandService.updateCar(CAR_ID, updateDto);

            assertThat(result).isNotNull();
            assertThat(result.model()).isEqualTo("Camry Updated");
        }

        @Test
        @DisplayName("Обновление несуществующего автомобиля - исключение")
        void updateCar_NonExistingId_ThrowsException() {
            when(carRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carCommandService.updateCar(NON_EXISTENT_ID, testCarDto))
                    .isInstanceOf(PlatformException.CarNotFoundException.class);

            verify(carRepository, never()).save(any());
        }

        @Test
        @DisplayName("Удаление существующего автомобиля - успешно")
        void deleteCar_ExistingId_Success() {
            when(carRepository.existsById(CAR_ID)).thenReturn(true);

            carCommandService.deleteCar(CAR_ID);

            verify(carRepository).deleteById(CAR_ID);
        }

        @Test
        @DisplayName("Удаление несуществующего автомобиля - исключение")
        void deleteCar_NonExistingId_ThrowsException() {
            when(carRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> carCommandService.deleteCar(NON_EXISTENT_ID))
                    .isInstanceOf(PlatformException.CarNotFoundException.class);

            verify(carRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Пакетное сохранение автомобилей - успешно")
        void saveAllCars_ValidList_SavesAll() {
            List<CarDto> carDtos = List.of(
                    createCarDto("Toyota", "Camry", 2020, 50000, new BigDecimal("2500000")),
                    createCarDto("Honda", "Civic", 2021, 30000, new BigDecimal("2000000"))
            );

            List<Car> savedCars = List.of(
                    Car.builder().id(1L).build(),
                    Car.builder().id(2L).build()
            );

            when(carMapper.toEntity(any(CarDto.class))).thenReturn(new Car());
            when(carRepository.saveAll(anyList())).thenReturn(savedCars);
            when(carMapper.toDtoList(anyList())).thenReturn(carDtos);

            List<CarDto> result = carCommandService.saveAllCars(carDtos);

            assertThat(result).hasSize(2);
            verify(carRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Пакетное сохранение пустого списка - возвращает пустой список")
        void saveAllCars_EmptyList_ReturnsEmptyList() {
            List<CarDto> result = carCommandService.saveAllCars(List.of());

            assertThat(result).isEmpty();
            verify(carRepository, never()).saveAll(any());
        }
    }

    // ============================================================
    // ТЕСТЫ CAR STATISTICS SERVICE
    // ============================================================

    @Nested
    @DisplayName("CarStatisticsService - тесты статистики")
    class CarStatisticsServiceTests {

        @Test
        @DisplayName("Получение общего количества автомобилей")
        void getTotalCarsCount_ReturnsCount() {
            when(carRepository.count()).thenReturn(5L);

            long result = carStatisticsService.getTotalCarsCount();

            assertThat(result).isEqualTo(5L);
            verify(carRepository).count();
        }

        @Test
        @DisplayName("Получение статистики по маркам")
        void getStatisticsByBrand_ReturnsMap() {
            Car car1 = Car.builder().brand("Toyota").build();
            Car car2 = Car.builder().brand("Toyota").build();
            Car car3 = Car.builder().brand("Honda").build();

            when(carRepository.findAll()).thenReturn(List.of(car1, car2, car3));

            Map<String, Long> result = carStatisticsService.getStatisticsByBrand();

            assertThat(result).containsEntry("Toyota", 2L).containsEntry("Honda", 1L);
        }

        @Test
        @DisplayName("Получение детальной статистики")
        void getCarStatistics_ReturnsCompleteStatistics() {
            when(carRepository.count()).thenReturn(10L);
            when(carRepository.countCarsWithPhotos()).thenReturn(6L);
            when(carRepository.getAveragePrice()).thenReturn(2500000.0);
            when(carRepository.getAverageMileage()).thenReturn(50000.0);

            List<Object[]> yearRange = new ArrayList<>();
            yearRange.add(new Object[]{2015, 2024});
            when(carRepository.findYearRange()).thenReturn(yearRange);
            when(carRepository.findMaxParsedAt()).thenReturn(Optional.of(LocalDateTime.now()));

            var result = carStatisticsService.getCarStatistics();

            assertThat(result.totalCars()).isEqualTo(10L);
            assertThat(result.carsWithPhotos()).isEqualTo(6L);
            assertThat(result.carsWithoutPhotos()).isEqualTo(4L);
            assertThat(result.averagePrice()).isEqualTo(2500000.0);
            assertThat(result.averageMileage()).isEqualTo(50000.0);
            assertThat(result.oldestYear()).isEqualTo(2015);
            assertThat(result.newestYear()).isEqualTo(2024);
            assertThat(result.lastParsedAt()).isNotNull();
        }
    }

    // ============================================================
    // ТЕСТЫ CAR FILTER OPTIONS SERVICE
    // ============================================================

    @Nested
    @DisplayName("CarFilterOptionsService - тесты опций фильтрации")
    class CarFilterOptionsServiceTests {

        @Test
        @DisplayName("Получение опций для фильтров - успешно")
        void getFilterOptions_ReturnsOptions() {
            List<String> mockBrands = List.of("Toyota", "Honda");
            when(carRepository.findAllBrands()).thenReturn(mockBrands);

            List<Object[]> mockYearRange = new ArrayList<>();
            mockYearRange.add(new Object[]{2010, 2023});
            when(carRepository.findYearRange()).thenReturn(mockYearRange);

            List<Object[]> mockPriceRange = new ArrayList<>();
            mockPriceRange.add(new Object[]{new BigDecimal("500000"), new BigDecimal("5000000")});
            when(carRepository.findPriceRange()).thenReturn(mockPriceRange);

            Map<String, Object> options = carFilterOptionsService.getFilterOptions();

            assertThat(options).containsKeys("brands", "yearMin", "yearMax", "priceMin", "priceMax",
                    "transmissions", "driveTypes");
            assertThat((List<String>) options.get("brands")).containsExactly("Toyota", "Honda");
            assertThat(options.get("yearMin")).isEqualTo(2010);
            assertThat(options.get("yearMax")).isEqualTo(2023);
        }
    }

    // ============================================================
    // ТЕСТЫ CAR EXPORT IMPORT SERVICE
    // ============================================================

    @Nested
    @DisplayName("CarExportImportService - тесты экспорта/импорта")
    class CarExportImportServiceTests {

        @Test
        @DisplayName("Экспорт автомобилей в CSV")
        void exportCarsToCsv_ReturnsCsvData() {
            List<Long> carIds = List.of(1L, 2L);
            List<Car> cars = List.of(testCar, testCar);

            when(carRepository.findAllById(carIds)).thenReturn(cars);

            byte[] result = carExportImportService.exportCarsToCsv(carIds);

            assertThat(result).isNotNull();
            assertThat(new String(result)).contains("ID,Brand,Model,Year,Mileage,Price");
        }

        @Test
        @DisplayName("Импорт автомобилей из CSV")
        void importCarsFromCsv_Success() {
            String csv = "ID,Brand,Model,Year,Mileage,Price\n1,Toyota,Camry,2020,50000,2500000";
            CarDto carDto = createCarDto("Toyota", "Camry", 2020, 50000, new BigDecimal("2500000"));
            Car car = Car.builder().brand("Toyota").model("Camry").build();

            when(carMapper.toEntity(any(CarDto.class))).thenReturn(car);
            when(carRepository.save(any(Car.class))).thenReturn(car);
            when(carMapper.toDto(any(Car.class))).thenReturn(carDto);

            carExportImportService.importCarsFromCsv(csv.getBytes());

            verify(carRepository, times(1)).save(any(Car.class));
        }
    }
}