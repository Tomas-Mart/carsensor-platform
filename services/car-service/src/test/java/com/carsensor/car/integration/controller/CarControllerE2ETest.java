package com.carsensor.car.integration.controller;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.car.CarServiceApplication;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.common.test.util.TestJwtUtils;
import com.carsensor.platform.dto.CarDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E интеграционные тесты CarController.
 *
 * <p>Тестирует полный HTTP стек с реальным сервером и базой данных.
 * Все тесты изолированы, используют встроенную PostgreSQL и автоматический откат транзакций.
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Slf4j
@DisplayName("E2E интеграционные тесты CarController")
@SpringBootTest(classes = CarServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CarControllerE2ETest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HikariDataSource dataSource;

    private String adminToken;

    @BeforeAll
    static void startTestSuite() {
        log.info("═══════════════════════════════════════════════════════════════════════════════");
        log.info("🚀 ЗАПУСК НАБОРА E2E ТЕСТОВ");
        log.info("═══════════════════════════════════════════════════════════════════════════════");
        logEnvironmentInfoStatic();
    }

    @AfterAll
    static void endTestSuite() {
        log.info("═══════════════════════════════════════════════════════════════════════════════");
        log.info("📊 ИТОГОВАЯ СТАТИСТИКА E2E ТЕСТОВ");
        log.info("═══════════════════════════════════════════════════════════════════════════════");
        log.info("✅ Выполнено тестов: {}", getTestCount());
        log.info("⏱️  Общее время: {} секунд",
                Duration.between(getTestStartTime(), Instant.now()).getSeconds());
        log.info("═══════════════════════════════════════════════════════════════════════════════");
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        String secret = TestJwtUtils.getJwtSecret();
        registry.add("app.jwt.secret", () -> secret);
        registry.add("jwt.secret", () -> secret);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("logging.level.com.carsensor.car.infrastructure.security", () -> "DEBUG");
    }

    // ============================================================
    // ЛУЧШИЙ ПОДХОД: вынести проверку в @BeforeEach
    // ============================================================
    @BeforeEach
    void setUp() {
        logEnvironmentInfo();
        log.debug("ObjectMapper: {}", objectMapper.getClass().getSimpleName());
        logEnvironmentInfo();
        adminToken = TestJwtUtils.createAdminToken();
        waitForDatabase();

        // Проверка порта как часть валидации окружения
        if (!isDatabaseInitialized()) {
            throw new IllegalStateException("База данных не инициализирована");
        }

        int port = getDatabasePort();
        if (port <= 0) {
            throw new IllegalStateException("Неверный порт базы данных: " + port);
        }
        log.info("База данных запущена на порту: {}", port);

        awaitServerReady();
    }

    /**
     * Ожидает готовности сервера к приему запросов.
     */
    private void awaitServerReady() {
        int maxAttempts = 60;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                var headers = new HttpHeaders();
                var response = restTemplate.exchange(
                        baseUrl() + "/actuator/health",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                );
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Сервер готов после {} попыток", i + 1);
                    return;
                }
            } catch (Exception e) {
                // Игнорируем ошибки подключения
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.warn("Сервер может быть не полностью готов, продолжаем...");
    }

    // ============================================================
    // GET /api/v1/cars - получение списка автомобилей
    // ============================================================
    @Nested
    @DisplayName("GET /api/v1/cars - получение списка автомобилей")
    class GetCarsE2ETests {

        @BeforeEach
        void setUp() {
            // Полная очистка перед каждым тестом
            carRepository.deleteAll();
            carRepository.flush(); // Важно: принудительно применяем удаление

            // Создаём свежие тестовые данные
            Car toyotaCamry = Car.builder()
                    .brand("Toyota").model("Camry").year(2020).mileage(50000).price(new BigDecimal("2500000")).build();
            Car toyotaRav4 = Car.builder()
                    .brand("Toyota").model("RAV4").year(2021).mileage(45000).price(new BigDecimal("2800000")).build();
            Car hondaCivic = Car.builder()
                    .brand("Honda").model("Civic").year(2021).mileage(30000).price(new BigDecimal("2200000")).build();
            Car hondaAccord = Car.builder()
                    .brand("Honda").model("Accord").year(2022).mileage(25000).price(new BigDecimal("2700000")).build();
            Car nissanXTrail = Car.builder()
                    .brand("Nissan").model("X-Trail").year(2021).mileage(20000).price(new BigDecimal("2700000")).build();
            Car mazdaCx5 = Car.builder()
                    .brand("Mazda").model("CX-5").year(2022).mileage(15000).price(new BigDecimal("2800000")).build();

            carRepository.saveAll(List.of(toyotaCamry, toyotaRav4, hondaCivic, hondaAccord, nissanXTrail, mazdaCx5));
            carRepository.flush(); // Важно: принудительно сохраняем в БД

            // Логируем количество сохраненных записей для отладки
            log.info("Saved {} cars to database", carRepository.count());
        }

        @Test
        @DisplayName("Получение всех автомобилей")
        void getAllCars_ReturnsAllCars() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars?size=50",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            String body = response.getBody();

            // Проверяем бренды
            assertThat(body).contains("Toyota", "Honda", "Nissan", "Mazda");

            // Проверяем модели
            assertThat(body).contains("Camry", "RAV4", "Civic", "Accord", "X-Trail", "CX-5");

            // Проверяем, что нет данных от других тестов
            assertThat(body).doesNotContain("Camry XLE");
        }

        @Test
        @DisplayName("Фильтрация по марке Toyota")
        void filterByBrand_Toyota_ReturnsToyotaCars() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars?brand=Toyota&size=50",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            String body = response.getBody();

            // Должны быть обе Toyota модели
            assertThat(body).contains("Camry", "RAV4");

            // Не должно быть других брендов
            assertThat(body).doesNotContain("Honda", "Nissan", "Mazda");
            assertThat(body).doesNotContain("Civic", "Accord", "X-Trail", "CX-5");
        }

        @Test
        @DisplayName("Фильтрация по диапазону цен")
        void filterByPriceRange_ReturnsCarsInRange() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars?priceFrom=2000000&priceTo=3000000&size=50",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            String body = response.getBody();

            // Все автомобили должны быть в этом диапазоне
            assertThat(body).contains("Camry", "RAV4", "Civic", "Accord", "X-Trail", "CX-5");
        }

        @Test
        @DisplayName("Пагинация - вторая страница")
        void pagination_SecondPage_ReturnsCorrectPage() throws Exception {
            carRepository.deleteAll();
            carRepository.flush();

            List<Car> testCars = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                testCars.add(Car.builder()
                        .brand("Brand" + i)
                        .model("Model" + i)
                        .year(2020 + (i % 5))
                        .mileage(50000 + i * 1000)
                        .price(new BigDecimal("1500000").add(new BigDecimal(i * 10000)))
                        .build());
            }
            carRepository.saveAll(testCars);
            carRepository.flush();

            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars?page=1&size=10&sort=id,ASC",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Парсим JSON для точной проверки
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode content = root.get("content");

            // Проверяем количество элементов на странице
            assertThat(content.size()).isEqualTo(5);

            // Проверяем бренды на второй странице
            List<String> brands = new ArrayList<>();
            for (JsonNode item : content) {
                brands.add(item.get("brand").asText());
            }

            assertThat(brands).containsExactly("Brand10", "Brand11", "Brand12", "Brand13", "Brand14");
        }
    }

    // ============================================================
    // POST /api/v1/cars - создание автомобилей
    // ============================================================
    @Nested
    @DisplayName("POST /api/v1/cars - создание автомобилей")
    class CreateCarE2ETests {

        @BeforeEach
        void setUp() {
            carRepository.deleteAll();
        }

        @Test
        @Transactional
        @DisplayName("Создание автомобиля с валидными данными")
        void validData_CreatesCar() throws Exception {

            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

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

            String requestJson = objectMapper.writeValueAsString(newCar);
            log.debug("Request JSON: {}", requestJson);

            try {
                var response = restTemplate.exchange(
                        baseUrl() + "/api/v1/cars",
                        HttpMethod.POST,
                        new HttpEntity<>(newCar, headers),
                        CarDto.class
                );

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().id()).isNotNull();
                assertThat(response.getBody().brand()).isEqualTo("Mazda");
            } catch (Exception e) {
                // Сохраняем дамп ошибки
                saveFailureDump(
                        "validData_CreatesCar",
                        "Request data: " + newCar + "\nError: " + e.getMessage(),
                        e.getClass().getSimpleName()
                );
                throw e;
            }
        }

        @Test
        @Transactional
        @DisplayName("Создание автомобиля с дублирующим externalId")
        void duplicateExternalId_ReturnsConflict() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            CarDto firstCar = new CarDto(
                    null,                           // id
                    "Toyota",                       // brand
                    "Camry",                        // model
                    2020,                           // year
                    50000,                          // mileage
                    new BigDecimal("2500000"),      // price
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
                    "unique-id-123",                // externalId
                    null,                           // sourceUrl
                    null,                           // parsedAt
                    null,                           // createdAt
                    null,                           // updatedAt
                    null                            // version
            );

            var createResponse = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars",
                    HttpMethod.POST,
                    new HttpEntity<>(firstCar, headers),
                    CarDto.class
            );
            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            CarDto duplicateCar = new CarDto(
                    null,                           // id
                    "Honda",                        // brand
                    "Civic",                        // model
                    2021,                           // year
                    30000,                          // mileage
                    new BigDecimal("2000000"),      // price
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
                    "unique-id-123",                // externalId
                    null,                           // sourceUrl
                    null,                           // parsedAt
                    null,                           // createdAt
                    null,                           // updatedAt
                    null                            // version
            );

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars",
                    HttpMethod.POST,
                    new HttpEntity<>(duplicateCar, headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @Transactional
        @DisplayName("Создание автомобиля с некорректными данными")
        void invalidData_ReturnsBadRequest() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            CarDto invalidCar = new CarDto(
                    null, "", "", 1800, -100, null,  // price = null
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null
            );

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars",
                    HttpMethod.POST,
                    new HttpEntity<>(invalidCar, headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ============================================================
    // GET /api/v1/cars/{id} - получение автомобиля по ID
    // ============================================================
    @Nested
    @DisplayName("GET /api/v1/cars/{id} - получение автомобиля по ID")
    class GetCarByIdE2ETests {

        @BeforeEach
        void setUp() {
            carRepository.deleteAll();
        }

        @Test
        @Transactional
        @DisplayName("Получение существующего автомобиля")
        void existingId_ReturnsCar() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            CarDto newCar = new CarDto(
                    null,                           // id
                    "Nissan",                       // brand
                    "X-Trail",                      // model
                    2021,                           // year
                    20000,                          // mileage
                    new BigDecimal("2700000"),      // price
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
                    null,                           // externalId
                    null,                           // sourceUrl
                    null,                           // parsedAt
                    null,                           // createdAt
                    null,                           // updatedAt
                    null                            // version
            );

            var createResponse = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars",
                    HttpMethod.POST,
                    new HttpEntity<>(newCar, headers),
                    CarDto.class
            );
            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Long carId = Objects.requireNonNull(createResponse.getBody()).id();

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars/" + carId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    CarDto.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().brand()).isEqualTo("Nissan");
            assertThat(response.getBody().model()).isEqualTo("X-Trail");
        }

        @Test
        @Transactional
        @DisplayName("Получение несуществующего автомобиля")
        void nonExistingId_ReturnsNotFound() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars/99999",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ============================================================
    // PUT /api/v1/cars/{id} - обновление автомобилей
    // ============================================================
    @Nested
    @DisplayName("PUT /api/v1/cars/{id} - обновление автомобилей")
    class UpdateCarE2ETests {

        @BeforeEach
        void setUp() {
            carRepository.deleteAll();
        }

        @Test
        @Transactional
        @DisplayName("Обновление существующего автомобиля")
        void existingId_UpdatesCar() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            CarDto newCar = new CarDto(
                    null,                           // id
                    "Toyota",                       // brand
                    "Camry",                        // model
                    2020,                           // year
                    50000,                          // mileage
                    new BigDecimal("2500000"),      // price
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
                    null,                           // externalId
                    null,                           // sourceUrl
                    null,                           // parsedAt
                    null,                           // createdAt
                    null,                           // updatedAt
                    null                            // version
            );

            var createResponse = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars",
                    HttpMethod.POST,
                    new HttpEntity<>(newCar, headers),
                    CarDto.class
            );
            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Long carId = Objects.requireNonNull(createResponse.getBody()).id();

            CarDto updateCar = new CarDto(
                    null,                           // id
                    "Toyota",                       // brand
                    "Camry XLE",                    // model
                    2021,                           // year
                    45000,                          // mileage
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
                    null,                           // externalId
                    null,                           // sourceUrl
                    null,                           // parsedAt
                    null,                           // createdAt
                    null,                           // updatedAt
                    null                            // version
            );

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars/" + carId,
                    HttpMethod.PUT,
                    new HttpEntity<>(updateCar, headers),
                    CarDto.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().model()).isEqualTo("Camry XLE");
        }

        @Test
        @Transactional
        @DisplayName("Обновление несуществующего автомобиля")
        void nonExistingId_ReturnsNotFound() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            CarDto updateCar = new CarDto(
                    null,                           // id
                    "Honda",                        // brand
                    "Civic",                        // model
                    2021,                           // year
                    30000,                          // mileage
                    new BigDecimal("2000000"),      // price
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
                    null,                           // externalId
                    null,                           // sourceUrl
                    null,                           // parsedAt
                    null,                           // createdAt
                    null,                           // updatedAt
                    null                            // version
            );

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars/99999",
                    HttpMethod.PUT,
                    new HttpEntity<>(updateCar, headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ============================================================
    // DELETE /api/v1/cars/{id} - удаление автомобилей
    // ============================================================
    @Nested
    @DisplayName("DELETE /api/v1/cars/{id} - удаление автомобилей")
    class DeleteCarE2ETests {

        @BeforeEach
        void setUp() {
            carRepository.deleteAll();
        }

        @Test
        @Transactional
        @DisplayName("Удаление существующего автомобиля")
        void existingId_DeletesCar() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            CarDto newCar = new CarDto(
                    null,                               // id
                    "Mitsubishi",                       // brand
                    "Outlander",                        // model
                    2020,                               // year
                    40000,                              // mileage
                    new BigDecimal("2300000"),          // price
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

            var createResponse = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars",
                    HttpMethod.POST,
                    new HttpEntity<>(newCar, headers),
                    CarDto.class
            );
            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Long carId = Objects.requireNonNull(createResponse.getBody()).id();

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars/" + carId,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            var getResponse = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars/" + carId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @Transactional
        @DisplayName("Удаление несуществующего автомобиля")
        void nonExistingId_ReturnsNotFound() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            var response = restTemplate.exchange(
                    baseUrl() + "/api/v1/cars/99999",
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @AfterEach
    void checkNoLeaks() {
        int active = dataSource.getHikariPoolMXBean().getActiveConnections();
        if (active > 0) {
            log.warn("⚠️ Утечка соединений! Active connections: {}", active);
        }
    }
}