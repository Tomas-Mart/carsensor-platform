package com.carsensor.car.integration.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.car.CarServiceApplication;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.common.test.util.TestJwtUtils;
import com.carsensor.platform.dto.CarDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Слайс-тесты CarController для бизнес-логики.
 *
 * <p>Тестирует контроллер с использованием MockMvc без реального HTTP сервера.
 * Быстрее полных интеграционных тестов, но использует реальную базу данных.
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Slf4j
@AutoConfigureMockMvc
@DisplayName("Слайс-тесты CarController (бизнес-логика)")
@SpringBootTest(classes = CarServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CarControllerBusinessTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    private String adminToken;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void startTestSuite() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("🚀 ЗАПУСК СЛАЙС-ТЕСТОВ BUSINESS");
        log.info("═══════════════════════════════════════════════════════════════");
        logEnvironmentInfoStatic();
    }

    @BeforeAll
    static void cacheTestData() throws IOException {
        Path cacheFile = getTestDataCache().resolve("test-cars.json");
        List<CarDto> cachedTestCars;
        if (Files.exists(cacheFile)) {
            // Загружаем из кэша
            String json = Files.readString(cacheFile);
            cachedTestCars = objectMapper.readValue(json, new TypeReference<>() {
            });
            log.info("Загружено из кэша {} автомобилей", cachedTestCars.size());
        } else {
            // Создаем и сохраняем в кэш
            cachedTestCars = List.of(
                    new CarDto(null, "Toyota", "Camry", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
                    new CarDto(null, "Honda", "Civic", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
            );
            Files.writeString(cacheFile, objectMapper.writeValueAsString(cachedTestCars));
            log.info("Сохранено в кэш {} автомобилей", cachedTestCars.size());
        }
    }

    @AfterAll
    static void endTestSuite() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 ИТОГОВАЯ СТАТИСТИКА");
        log.info("✅ Выполнено тестов: {}", getTestCount());
        log.info("═══════════════════════════════════════════════════════════════");
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        String testName = testInfo.getDisplayName();
        String paddedName = String.format("%-60s", testName);
        log.info("╔══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║ 🔧 ЗАПУСК ТЕСТА: {} ║", paddedName);
        log.info("╚══════════════════════════════════════════════════════════════════════════════╝");

        logEnvironmentInfo();
        adminToken = TestJwtUtils.createAdminToken();
    }

    /**
     * Создает тестовые автомобили для проверки фильтрации и пагинации.
     */
    private void createTestCars() {
        carRepository.saveAll(List.of(
                Car.builder()
                        .brand("Toyota").model("Camry").year(2020)
                        .mileage(50000).price(new BigDecimal("2500000")).build(),
                Car.builder()
                        .brand("Toyota").model("RAV4").year(2021)
                        .mileage(30000).price(new BigDecimal("3500000")).build(),
                Car.builder()
                        .brand("Honda").model("Civic").year(2022)
                        .mileage(10000).price(new BigDecimal("1800000")).build()
        ));
    }

    // ============================================================
    // GET /api/v1/cars - фильтрация и пагинация
    // ============================================================
    @Nested
    @DisplayName("GET /api/v1/cars - фильтрация и пагинация")
    @Transactional
    class GetCarsBusinessTests {

        @BeforeEach
        void setUp() {
            carRepository.deleteAll();
            createTestCars();
        }

        @Test
        @DisplayName("Получение всех автомобилей")
        void getAllCars_ReturnsAllCars() throws Exception {
            mockMvc.perform(get("/api/v1/cars")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(3));
        }

        @Test
        @DisplayName("Фильтрация по марке Toyota")
        void filterByBrand_Toyota_ReturnsToyotaCars() throws Exception {
            mockMvc.perform(get("/api/v1/cars")
                            .param("brand", "Toyota")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("Фильтрация по диапазону цен")
        void filterByPriceRange_ReturnsCarsInRange() throws Exception {
            mockMvc.perform(get("/api/v1/cars")
                            .param("priceFrom", "2000000")
                            .param("priceTo", "3000000")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("Сортировка по цене (убывание)")
        void sortByPriceDesc_ReturnsSortedCars() throws Exception {
            mockMvc.perform(get("/api/v1/cars")
                            .param("sort", "price,desc")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].price").value(3500000))
                    .andExpect(jsonPath("$.content[1].price").value(2500000))
                    .andExpect(jsonPath("$.content[2].price").value(1800000));
        }

        @Test
        @DisplayName("Пагинация - вторая страница")
        void pagination_SecondPage_ReturnsCorrectPage() throws Exception {
            mockMvc.perform(get("/api/v1/cars")
                            .param("page", "1")
                            .param("size", "2")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.current_page").value(1))
                    .andExpect(jsonPath("$.total_pages").value(2));
        }
    }

    // ============================================================
    // GET /api/v1/cars/{id} - получение по ID
    // ============================================================
    @Nested
    @DisplayName("GET /api/v1/cars/{id} - получение по ID")
    @Transactional
    class GetCarByIdBusinessTests {

        private Long existingCarId;

        @BeforeEach
        void setUp() {
            carRepository.deleteAll();
            Car car = Car.builder()
                    .brand("Toyota").model("Camry").year(2020)
                    .mileage(50000).price(new BigDecimal("2500000")).build();
            existingCarId = carRepository.save(car).getId();
        }

        @Test
        @DisplayName("Существующий ID - возвращает автомобиль")
        void existingId_ReturnsCar() throws Exception {
            mockMvc.perform(get("/api/v1/cars/" + existingCarId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(existingCarId))
                    .andExpect(jsonPath("$.brand").value("Toyota"))
                    .andExpect(jsonPath("$.model").value("Camry"));
        }

        @Test
        @DisplayName("Несуществующий ID - 404")
        void nonExistingId_ReturnsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/cars/999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("CAR_NOT_FOUND"));
        }
    }

    // ============================================================
    // POST /api/v1/cars - создание
    // ============================================================
    @Nested
    @DisplayName("POST /api/v1/cars - создание")
    @Transactional
    class CreateCarBusinessTests {

        @BeforeEach
        void setUp() {
            carRepository.deleteAll();
        }

        @Test
        @DisplayName("Валидные данные - создает автомобиль")
        void validData_CreatesCar() throws Exception {
            CarDto newCar = new CarDto(
                    null,                       // id
                    "Mazda",                    // brand
                    "CX-5",                     // model
                    2022,                       // year
                    15000,                      // mileage
                    new BigDecimal("2800000"),  // price
                    null,                       // description
                    null,                       // originalBrand
                    null,                       // originalModel
                    null,                       // exteriorColor
                    null,                       // interiorColor
                    null,                       // engineCapacity
                    null,                       // transmission
                    null,                       // driveType
                    null,                       // photoUrls
                    null,                       // mainPhotoUrl
                    null,                       // externalId
                    null,                       // sourceUrl
                    null,                       // parsedAt
                    null,                       // createdAt
                    null,                       // updatedAt
                    null                        // version
            );
            mockMvc.perform(post("/api/v1/cars")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCar)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.brand").value("Mazda"));
        }

        @Test
        @DisplayName("Невалидные данные - 400")
        void invalidData_ReturnsBadRequest() throws Exception {
            CarDto invalidCar = new CarDto(
                    null,                       // id
                    "",                         // brand
                    null,                       // model
                    1800,                       // year
                    -100,                       // mileage
                    null,                       // price
                    null,                       // description
                    null,                       // originalBrand
                    null,                       // originalModel
                    null,                       // exteriorColor
                    null,                       // interiorColor
                    null,                       // engineCapacity
                    null,                       // transmission
                    null,                       // driveType
                    null,                       // photoUrls
                    null,                       // mainPhotoUrl
                    null,                       // externalId
                    null,                       // sourceUrl
                    null,                       // parsedAt
                    null,                       // createdAt
                    null,                       // updatedAt
                    null                        // version
            );
            mockMvc.perform(post("/api/v1/cars")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidCar)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("Дублирующийся externalId - 409")
        void duplicateExternalId_ReturnsConflict() throws Exception {
            CarDto car1 = new CarDto(
                    null,                           // id
                    "Mazda",                        // brand
                    "CX-5",                         // model
                    2022,                           // year
                    15000,                          // mileage
                    new BigDecimal("2800000"),      // price
                    null,                           // description
                    null,                           // originalBrand
                    null,                           // originalModel
                    null,                           // exteriorColor
                    null,                           // interiorColor
                    null,                           // engineCapacity
                    null,                           // transmission
                    null,                           // driveType
                    null,                           // photoUrls
                    null,                           // mainPhotoUrl
                    "EXT001",                       // externalId
                    null,                           // sourceUrl
                    null,                           // parsedAt
                    null,                           // createdAt
                    null,                           // updatedAt
                    null                            // version
            );
            mockMvc.perform(post("/api/v1/cars")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(car1)))
                    .andExpect(status().isCreated());

            CarDto car2 = new CarDto(
                    null,                           // id
                    "Mazda",                        // brand
                    "CX-5",                         // model
                    2023,                           // year
                    5000,                           // mileage
                    new BigDecimal("3200000"),      // price
                    null,                           // description
                    null,                           // originalBrand
                    null,                           // originalModel
                    null,                           // exteriorColor
                    null,                           // interiorColor
                    null,                           // engineCapacity
                    null,                           // transmission
                    null,                           // driveType
                    null,                           // photoUrls
                    null,                           // mainPhotoUrl
                    "EXT001",                       // externalId
                    null,                           // sourceUrl
                    null,                           // parsedAt
                    null,                           // createdAt
                    null,                           // updatedAt
                    null                            // version
            );
            mockMvc.perform(post("/api/v1/cars")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(car2)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("DUPLICATE_RESOURCE"));
        }
    }

    // ============================================================
    // PUT /api/v1/cars/{id} - обновление
    // ============================================================
    @Nested
    @DisplayName("PUT /api/v1/cars/{id} - обновление")
    @Transactional
    class UpdateCarBusinessTests {

        private Long existingCarId;

        @BeforeEach
        void setUp() {
            carRepository.deleteAll();
            Car car = Car.builder()
                    .brand("Toyota").model("Camry").year(2020)
                    .mileage(50000).price(new BigDecimal("2500000")).build();
            existingCarId = carRepository.save(car).getId();
        }

        @Test
        @DisplayName("Существующий ID - обновляет автомобиль")
        void existingId_UpdatesCar() throws Exception {
            CarDto updateData = new CarDto(
                    null,                               // id
                    "Toyota",                           // brand
                    "Camry Updated",                    // model
                    2021,                               // year
                    45000,                              // mileage
                    new BigDecimal("2400000"),          // price
                    null,                               // description
                    null,                               // originalBrand
                    null,                               // originalModel
                    null,                               // exteriorColor
                    null,                               // interiorColor
                    null,                               // engineCapacity
                    null,                               // transmission
                    null,                               // driveType
                    null,                               // photoUrls
                    null,                               // mainPhotoUrl
                    null,                               // externalId
                    null,                               // sourceUrl
                    null,                               // parsedAt
                    null,                               // createdAt
                    null,                               // updatedAt
                    null                                // version
            );
            mockMvc.perform(put("/api/v1/cars/" + existingCarId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.model").value("Camry Updated"));
        }

        @Test
        @DisplayName("Несуществующий ID - 404")
        void nonExistingId_ReturnsNotFound() throws Exception {
            CarDto updateData = new CarDto(
                    null,                               // id
                    "Toyota",                           // brand
                    "Camry",                            // model
                    2021,                               // year
                    45000,                              // mileage
                    new BigDecimal("2400000"),          // price
                    null,                               // description
                    null,                               // originalBrand
                    null,                               // originalModel
                    null,                               // exteriorColor
                    null,                               // interiorColor
                    null,                               // engineCapacity
                    null,                               // transmission
                    null,                               // driveType
                    null,                               // photoUrls
                    null,                               // mainPhotoUrl
                    null,                               // externalId
                    null,                               // sourceUrl
                    null,                               // parsedAt
                    null,                               // createdAt
                    null,                               // updatedAt
                    null                                // version
            );
            mockMvc.perform(put("/api/v1/cars/999")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isNotFound());
        }
    }

    // ============================================================
    // DELETE /api/v1/cars/{id} - удаление
    // ============================================================
    @Nested
    @DisplayName("DELETE /api/v1/cars/{id} - удаление")
    @Transactional
    class DeleteCarBusinessTests {

        private Long existingCarId;

        @BeforeEach
        void setUp() {
            carRepository.deleteAll();
            Car car = Car.builder()
                    .brand("Toyota").model("Camry").year(2020)
                    .mileage(50000).price(new BigDecimal("2500000")).build();
            existingCarId = carRepository.save(car).getId();
        }

        @Test
        @DisplayName("Существующий ID - удаляет автомобиль")
        void existingId_DeletesCar() throws Exception {
            mockMvc.perform(delete("/api/v1/cars/" + existingCarId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Несуществующий ID - 404")
        void nonExistingId_ReturnsNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/cars/999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }
}