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
import com.carsensor.car.application.service.CarService;
import com.carsensor.car.application.service.impl.CarServiceImpl;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.platform.dto.PageResponse;
import com.carsensor.platform.exception.PlatformException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты сервиса автомобилей")
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarMapper carMapper;

    private CarService carService;

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
        carService = new CarServiceImpl(carRepository, carMapper);

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

        testCarDto = CarDto.builder()
                .id(CAR_ID)
                .brand("Toyota")
                .model("Camry")
                .year(2020)
                .mileage(50000)
                .price(new BigDecimal("2500000"))
                .exteriorColor("White")
                .transmission("AT")
                .driveType("2WD")
                .build();
    }

    @Nested
    @DisplayName("8. Тесты получения автомобилей")
    class GetCarTests {

        @Test
        @DisplayName("22. Получение автомобиля по ID - успешно")
        void getCarById_ExistingId_ReturnsCar() {
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));
            when(carMapper.toDto(testCar)).thenReturn(testCarDto);

            CarDto result = carService.getCarById(CAR_ID);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(CAR_ID);
            assertThat(result.brand()).isEqualTo("Toyota");
            assertThat(result.model()).isEqualTo("Camry");

            verify(carRepository).findById(CAR_ID);
            verify(carMapper).toDto(testCar);
        }

        @Test
        @DisplayName("23. Получение автомобиля по несуществующему ID - исключение")
        void getCarById_NonExistingId_ThrowsException() {
            when(carRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carService.getCarById(NON_EXISTENT_ID))
                    .isInstanceOf(PlatformException.CarNotFoundException.class)
                    .satisfies(exception -> {
                        PlatformException.CarNotFoundException ex =
                                (PlatformException.CarNotFoundException) exception;
                        assertThat(ex.getErrorCode()).isEqualTo("CAR_NOT_FOUND");
                    });
        }

        @Test
        @DisplayName("25. Получение списка автомобилей с фильтрацией - успешно")
        void getCars_WithFilters_ReturnsPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Car> carPage = new PageImpl<>(List.of(testCar), pageable, 1);

            when(carRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(carPage);

            CarDto expectedDto = CarDto.builder()
                    .id(CAR_ID)
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .exteriorColor("White")
                    .transmission("AT")
                    .driveType("2WD")
                    .build();

            when(carMapper.toDto(any(Car.class))).thenReturn(expectedDto);

            PageResponse<CarDto> result = carService.getCars(
                    "Toyota", null, 2020, 2021, null, null,
                    null, null, null, null, null, pageable
            );

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content().getFirst().brand()).isEqualTo("Toyota");
        }

        @Test
        @DisplayName("26. Получение пустого списка, когда автомобили не найдены")
        void getCars_NoCarsFound_ReturnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Car> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(carRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(emptyPage);

            PageResponse<CarDto> result = carService.getCars(
                    "NonExistentBrand", null, null, null, null, null,
                    null, null, null, null, null, pageable
            );

            assertThat(result).isNotNull();
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("24. Получение опций для фильтров - успешно")
        void getFilterOptions_ReturnsOptions() {
            List<String> mockBrands = List.of("Toyota", "Honda");
            when(carRepository.findAllBrands()).thenReturn(mockBrands);

            List<Object[]> mockYearRange = new ArrayList<>();
            mockYearRange.add(new Object[]{2010, 2023});
            when(carRepository.findYearRange()).thenReturn(mockYearRange);

            List<Object[]> mockPriceRange = new ArrayList<>();
            mockPriceRange.add(new Object[]{new BigDecimal("500000"), new BigDecimal("5000000")});
            when(carRepository.findPriceRange()).thenReturn(mockPriceRange);

            Map<String, Object> options = carService.getFilterOptions();

            assertThat(options)
                    .containsKeys("brands", "yearMin", "yearMax", "priceMin", "priceMax",
                            "transmissions", "driveTypes");

            assertThat((List<String>) options.get("brands"))
                    .containsExactly("Toyota", "Honda");

            assertThat(options.get("yearMin")).isEqualTo(2010);
            assertThat(options.get("yearMax")).isEqualTo(2023);
        }
    }

    @Nested
    @DisplayName("7. Тесты создания автомобилей")
    class CreateCarTests {

        @Test
        @DisplayName("19. Создание нового автомобиля - успешно")
        void createCar_ValidData_ReturnsCreatedCar() {
            CarDto newCarDto = CarDto.builder()
                    .brand("Honda")
                    .model("Civic")
                    .year(2022)
                    .mileage(10000)
                    .price(new BigDecimal("1800000"))
                    .build();

            Car newCar = Car.builder()
                    .brand("Honda")
                    .model("Civic")
                    .year(2022)
                    .mileage(10000)
                    .price(new BigDecimal("1800000"))
                    .build();

            Car savedCar = Car.builder()
                    .id(2L)
                    .brand("Honda")
                    .model("Civic")
                    .year(2022)
                    .mileage(10000)
                    .price(new BigDecimal("1800000"))
                    .build();

            CarDto expectedDto = CarDto.builder()
                    .id(2L)
                    .brand("Honda")
                    .model("Civic")
                    .year(2022)
                    .mileage(10000)
                    .price(new BigDecimal("1800000"))
                    .build();

            when(carMapper.toEntity(newCarDto)).thenReturn(newCar);
            when(carRepository.save(any(Car.class))).thenReturn(savedCar);
            when(carMapper.toDto(savedCar)).thenReturn(expectedDto);

            CarDto result = carService.createCar(newCarDto);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(2L);
            assertThat(result.brand()).isEqualTo("Honda");

            verify(carRepository).save(carCaptor.capture());
            Car capturedCar = carCaptor.getValue();
            assertThat(capturedCar.getBrand()).isEqualTo("Honda");
            assertThat(capturedCar.getParsedAt()).isNotNull();
        }

        @Test
        @DisplayName("20. Создание автомобиля с существующим externalId - исключение")
        void createCar_DuplicateExternalId_ThrowsException() {
            CarDto newCarDto = CarDto.builder()
                    .brand("Honda")
                    .model("Civic")
                    .externalId("ext123")
                    .build();

            when(carRepository.existsByExternalId("ext123")).thenReturn(true);

            assertThatThrownBy(() -> carService.createCar(newCarDto))
                    .isInstanceOf(PlatformException.DuplicateResourceException.class)
                    .satisfies(exception -> {
                        PlatformException.DuplicateResourceException ex =
                                (PlatformException.DuplicateResourceException) exception;
                        assertThat(ex.getErrorCode()).isEqualTo("DUPLICATE_RESOURCE");
                    });
        }

        @Test
        @DisplayName("18. Создание автомобиля с null externalId - успешно")
        void createCar_NullExternalId_Success() {
            CarDto newCarDto = CarDto.builder()
                    .brand("Honda")
                    .model("Civic")
                    .externalId(null)
                    .build();

            when(carMapper.toEntity(newCarDto)).thenReturn(new Car());
            when(carRepository.save(any())).thenReturn(testCar);
            when(carMapper.toDto(any())).thenReturn(testCarDto);

            CarDto result = carService.createCar(newCarDto);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("21. Создание автомобиля с пустым externalId - успешно")
        void createCar_EmptyExternalId_Success() {
            CarDto newCarDto = CarDto.builder()
                    .brand("Honda")
                    .model("Civic")
                    .externalId("")
                    .build();

            when(carRepository.existsByExternalId(any())).thenReturn(false);
            when(carMapper.toEntity(newCarDto)).thenReturn(new Car());
            when(carRepository.save(any())).thenReturn(testCar);
            when(carMapper.toDto(any())).thenReturn(testCarDto);

            CarDto result = carService.createCar(newCarDto);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("6. Тесты обновления автомобилей")
    class UpdateCarTests {

        @Test
        @DisplayName("17. Обновление существующего автомобиля - успешно")
        void updateCar_ExistingId_ReturnsUpdatedCar() {
            CarDto updateDto = CarDto.builder()
                    .model("Camry Updated")
                    .year(2021)
                    .mileage(45000)
                    .price(new BigDecimal("2400000"))
                    .build();

            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));
            doAnswer(invocation -> {
                CarDto dto = invocation.getArgument(0);
                Car car = invocation.getArgument(1);
                car.setModel(dto.model());
                car.setYear(dto.year());
                car.setMileage(dto.mileage());
                car.setPrice(dto.price());
                return null;
            }).when(carMapper).updateEntityFromDto(eq(updateDto), any(Car.class));

            when(carRepository.save(any(Car.class))).thenReturn(testCar);
            when(carMapper.toDto(testCar)).thenReturn(
                    CarDto.builder()
                            .id(CAR_ID)
                            .brand("Toyota")
                            .model("Camry Updated")
                            .year(2021)
                            .mileage(45000)
                            .price(new BigDecimal("2400000"))
                            .build()
            );

            CarDto result = carService.updateCar(CAR_ID, updateDto);

            assertThat(result).isNotNull();
            assertThat(result.model()).isEqualTo("Camry Updated");
            assertThat(result.year()).isEqualTo(2021);
            assertThat(result.mileage()).isEqualTo(45000);

            verify(carRepository).save(any(Car.class));
        }

        @Test
        @DisplayName("15. Обновление несуществующего автомобиля - исключение")
        void updateCar_NonExistingId_ThrowsException() {
            when(carRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carService.updateCar(NON_EXISTENT_ID, testCarDto))
                    .isInstanceOf(PlatformException.CarNotFoundException.class)
                    .satisfies(exception -> {
                        PlatformException.CarNotFoundException ex =
                                (PlatformException.CarNotFoundException) exception;
                        assertThat(ex.getErrorCode()).isEqualTo("CAR_NOT_FOUND");
                    });

            verify(carRepository, never()).save(any());
        }

        @Test
        @DisplayName("14. Частичное обновление - только цена")
        void updateCar_PartialUpdate_PriceOnly() {
            CarDto partialUpdate = CarDto.builder()
                    .price(new BigDecimal("2300000"))
                    .build();

            Car existingCar = Car.builder()
                    .id(CAR_ID)
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();

            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(existingCar));

            doAnswer(invocation -> {
                Car car = invocation.getArgument(1);
                car.setPrice(partialUpdate.price());
                return null;
            }).when(carMapper).updateEntityFromDto(eq(partialUpdate), any(Car.class));

            when(carRepository.save(any(Car.class))).thenAnswer(i -> i.getArgument(0));
            when(carMapper.toDto(any())).thenReturn(testCarDto);

            carService.updateCar(CAR_ID, partialUpdate);

            verify(carRepository).save(carCaptor.capture());
            Car capturedCar = carCaptor.getValue();
            assertThat(capturedCar.getPrice()).isEqualTo(new BigDecimal("2300000"));
        }

        @Test
        @DisplayName("16. Частичное обновление - только пробег")
        void updateCar_PartialUpdate_MileageOnly() {
            CarDto partialUpdate = CarDto.builder()
                    .mileage(45000)
                    .build();

            Car existingCar = Car.builder()
                    .id(CAR_ID)
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();

            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(existingCar));

            doAnswer(invocation -> {
                CarDto dto = invocation.getArgument(0);
                Car car = invocation.getArgument(1);
                car.setMileage(dto.mileage());
                return null;
            }).when(carMapper).updateEntityFromDto(eq(partialUpdate), any(Car.class));

            when(carRepository.save(any(Car.class))).thenAnswer(i -> i.getArgument(0));
            when(carMapper.toDto(any())).thenReturn(testCarDto);

            carService.updateCar(CAR_ID, partialUpdate);

            verify(carRepository).save(carCaptor.capture());
            Car capturedCar = carCaptor.getValue();
            assertThat(capturedCar.getMileage()).isEqualTo(45000);
            assertThat(capturedCar.getPrice()).isEqualTo(new BigDecimal("2500000"));
        }
    }

    @Nested
    @DisplayName("5. Тесты удаления автомобилей")
    class DeleteCarTests {

        @Test
        @DisplayName("12. Удаление существующего автомобиля - успешно")
        void deleteCar_ExistingId_Success() {
            when(carRepository.existsById(CAR_ID)).thenReturn(true);

            carService.deleteCar(CAR_ID);

            verify(carRepository).deleteById(CAR_ID);
        }

        @Test
        @DisplayName("13. Удаление несуществующего автомобиля - исключение")
        void deleteCar_NonExistingId_ThrowsException() {
            when(carRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> carService.deleteCar(NON_EXISTENT_ID))
                    .isInstanceOf(PlatformException.CarNotFoundException.class)
                    .satisfies(exception -> {
                        PlatformException.CarNotFoundException ex =
                                (PlatformException.CarNotFoundException) exception;
                        assertThat(ex.getErrorCode()).isEqualTo("CAR_NOT_FOUND");
                    });

            verify(carRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("4. Тесты пакетной обработки")
    class BatchProcessingTests {

        @Test
        @DisplayName("11. Сохранение нескольких автомобилей - успешно")
        void saveAllCars_ValidList_SavesAll() {
            List<CarDto> carDtos = List.of(
                    CarDto.builder().brand("Toyota").model("Camry").build(),
                    CarDto.builder().brand("Honda").model("Civic").build(),
                    CarDto.builder().brand("Nissan").model("X-Trail").build()
            );

            List<Car> savedCars = List.of(
                    Car.builder().id(1L).build(),
                    Car.builder().id(2L).build(),
                    Car.builder().id(3L).build()
            );

            when(carMapper.toEntity(any(CarDto.class))).thenReturn(new Car());
            when(carRepository.saveAll(anyList())).thenReturn(savedCars);
            when(carMapper.toDtoList(anyList())).thenReturn(carDtos);

            List<CarDto> result = carService.saveAllCars(carDtos);

            assertThat(result).hasSize(3);
            verify(carRepository).saveAll(anyList());
            verify(carMapper, times(3)).toEntity(any(CarDto.class));
        }

        @Test
        @DisplayName("9. Сохранение пустого списка - возвращает пустой список")
        void saveAllCars_EmptyList_ReturnsEmptyList() {
            List<CarDto> emptyList = List.of();

            List<CarDto> result = carService.saveAllCars(emptyList);

            assertThat(result).isEmpty();
            verify(carRepository, never()).saveAll(any());
            verifyNoInteractions(carMapper);
        }

        @Test
        @DisplayName("8. Сохранение списка с одним автомобилем - успешно")
        void saveAllCars_SingleCar_SavesOne() {
            List<CarDto> carDtos = List.of(
                    CarDto.builder().brand("Toyota").model("Camry").build()
            );

            List<Car> savedCars = List.of(
                    Car.builder().id(1L).build()
            );

            when(carMapper.toEntity(any(CarDto.class))).thenReturn(new Car());
            when(carRepository.saveAll(anyList())).thenReturn(savedCars);
            when(carMapper.toDtoList(anyList())).thenReturn(carDtos);

            List<CarDto> result = carService.saveAllCars(carDtos);

            assertThat(result).hasSize(1);
            verify(carRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("10. Транзакция откатывается при ошибке в пакетной обработке")
        void saveAllCars_ErrorDuringSave_RollsBack() {
            List<CarDto> carDtos = List.of(
                    CarDto.builder().brand("Toyota").model("Camry").build(),
                    CarDto.builder().brand("Honda").model("Civic").build()
            );

            when(carMapper.toEntity(any(CarDto.class))).thenReturn(new Car());
            when(carRepository.saveAll(anyList()))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> carService.saveAllCars(carDtos))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");

            verify(carRepository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("3. Дополнительные тесты")
    class AdditionalTests {

        @Test
        @DisplayName("6. Получение автомобиля с null ID - исключение")
        void getCarById_NullId_ThrowsException() {
            assertThatThrownBy(() -> carService.getCarById(null))
                    .isInstanceOf(PlatformException.CarNotFoundException.class);
        }

        @Test
        @DisplayName("4. Создание автомобиля с null DTO - исключение")
        void createCar_NullDto_ThrowsException() {
            assertThatThrownBy(() -> carService.createCar(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("7. Обновление с null DTO - исключение")
        void updateCar_NullDto_ThrowsException() {

            assertThatThrownBy(() -> carService.updateCar(CAR_ID, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CarDto must not be null");
        }

        @Test
        @DisplayName("5. Получение всех автомобилей без фильтров")
        void getCars_NoFilters_ReturnsAll() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Car> carPage = new PageImpl<>(List.of(testCar), pageable, 1);

            when(carRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(carPage);

            PageResponse<CarDto> result = carService.getCars(
                    null, null, null, null, null, null,
                    null, null, null, null, null, pageable
            );

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("2. Тесты граничных случаев")
    class BoundaryTests {

        @Test
        @DisplayName("2. Получение автомобилей с пагинацией - вторая страница пуста")
        void getCars_SecondPageEmpty_ReturnsEmptyPage() {
            Pageable pageable = PageRequest.of(1, 20);
            Page<Car> emptyPage = new PageImpl<>(List.of(), pageable, 1);

            when(carRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(emptyPage);

            PageResponse<CarDto> result = carService.getCars(
                    null, null, null, null, null, null,
                    null, null, null, null, null, pageable
            );

            assertThat(result).isNotNull();
            assertThat(result.content()).isEmpty();
            assertThat(result.currentPage()).isEqualTo(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("3. Обновление автомобиля - все поля null (ничего не меняется)")
        void updateCar_AllFieldsNull_NoChanges() {
            CarDto updateDto = CarDto.builder().build();

            Car existingCar = Car.builder()
                    .id(CAR_ID)
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();

            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(existingCar));
            when(carRepository.save(any(Car.class))).thenReturn(existingCar);
            when(carMapper.toDto(any())).thenReturn(testCarDto);

            carService.updateCar(CAR_ID, updateDto);

            verify(carRepository).save(carCaptor.capture());
            Car capturedCar = carCaptor.getValue();
            assertThat(capturedCar.getBrand()).isEqualTo("Toyota");
            assertThat(capturedCar.getModel()).isEqualTo("Camry");
            assertThat(capturedCar.getYear()).isEqualTo(2020);
            assertThat(capturedCar.getMileage()).isEqualTo(50000);
            assertThat(capturedCar.getPrice()).isEqualTo(new BigDecimal("2500000"));
        }
    }

    @Nested
    @DisplayName("1. Тесты поиска и фильтрации")
    class SearchTests {

        @Test
        @DisplayName("1. Получение автомобилей с поисковым запросом")
        void getCars_WithSearchQuery_ReturnsPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Car> carPage = new PageImpl<>(List.of(testCar), pageable, 1);

            when(carRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(carPage);

            CarDto expectedDto = CarDto.builder()
                    .id(CAR_ID)
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();

            when(carMapper.toDto(any(Car.class))).thenReturn(expectedDto);

            PageResponse<CarDto> result = carService.getCars(
                    null, null, null, null, null, null,
                    null, null, null, null, "Camry", pageable
            );

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().model()).isEqualTo("Camry");
        }
    }
}