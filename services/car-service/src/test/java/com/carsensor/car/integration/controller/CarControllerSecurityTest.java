package com.carsensor.car.integration.controller;

import java.math.BigDecimal;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты безопасности CarController.
 *
 * <p>Проверяет доступ к эндпоинтам в зависимости от ролей пользователя.
 * ADMIN имеет полный доступ, USER имеет доступ только на чтение.
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Slf4j
@AutoConfigureMockMvc
@DisplayName("Тесты безопасности CarController")
@SpringBootTest(classes = CarServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CarControllerSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        // 1. Начало теста с четким разделителем
        String testName = testInfo.getDisplayName();
        String paddedName = String.format("%-60s", testName);
        log.info("╔══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║ 🔧 ЗАПУСК ТЕСТА: {} ║", paddedName);
        log.info("╚══════════════════════════════════════════════════════════════════════════════╝");
        // 2. Логирование окружения с временной меткой
        long startTime = System.currentTimeMillis();
        logEnvironmentInfo();

        // 3. Создание токенов
        adminToken = TestJwtUtils.createAdminToken();
        userToken = TestJwtUtils.createUserToken();
        log.debug("✅ Токены созданы: ADMIN={}, USER={}",
                adminToken.substring(0, Math.min(20, adminToken.length())) + "...",
                userToken.substring(0, Math.min(20, userToken.length())) + "...");

        // 4. Ожидание готовности БД с таймаутом
        try {
            waitForDatabase();
            log.debug("✅ База данных готова к работе");
        } catch (Exception e) {
            log.error("❌ Ошибка ожидания базы данных: {}", e.getMessage());
            throw new RuntimeException("Не удалось дождаться базы данных", e);
        }

        // 5. Проверка инициализации БД
        if (!isDatabaseInitialized()) {
            log.error("❌ База данных не инициализирована");
            throw new IllegalStateException("❌ База данных не инициализирована");
        }

        // 6. Логирование порта БД для отладки
        log.debug("📊 Порт базы данных: {}", getDatabasePort());

        // 7. Создание тестовых данных
        createTestCar();
        log.debug("✅ Тестовые данные созданы: Toyota Camry");

        // 8. Логирование времени инициализации
        long duration = System.currentTimeMillis() - startTime;
        log.debug("⏱️ Инициализация теста завершена за {} ms", duration);
    }

    /**
     * Создает тестовый автомобиль для проверки безопасности.
     */
    private void createTestCar() {
        Car car = Car.builder()
                .brand("Toyota")
                .model("Camry")
                .year(2020)
                .mileage(50000)
                .price(new BigDecimal("2500000"))
                .build();
        carRepository.save(car);
    }

    // ============================================================
    // GET /api/v1/cars - проверка доступа на чтение
    // ============================================================

    @Nested
    @DisplayName("GET /api/v1/cars - проверка доступа на чтение")
    @Transactional
    class GetCarsSecurityTests {

        @Test
        @DisplayName("ADMIN может читать - 200")
        void admin_CanRead_ReturnsOk() throws Exception {
            mockMvc.perform(get("/api/v1/cars")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USER может читать - 200")
        void user_CanRead_ReturnsOk() throws Exception {
            mockMvc.perform(get("/api/v1/cars")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Доступ без токена - 401")
        void withoutToken_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/cars"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Доступ с невалидным токеном - 401")
        void withInvalidToken_ReturnsUnauthorized() throws Exception {
            String invalidToken = "invalid.token.here";
            mockMvc.perform(get("/api/v1/cars")
                            .header("Authorization", "Bearer " + invalidToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ============================================================
    // POST /api/v1/cars - проверка прав на создание
    // ============================================================

    @Nested
    @DisplayName("POST /api/v1/cars - проверка прав на создание")
    @Transactional
    class CreateCarSecurityTests {

        /**
         * Создает валидный DTO для создания автомобиля.
         */
        private CarDto getValidCreateDto() {
            return new CarDto(
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
        }

        @Test
        @DisplayName("ADMIN может создавать - 201")
        void admin_CanCreate_ReturnsCreated() throws Exception {
            CarDto newCar = getValidCreateDto();

            mockMvc.perform(post("/api/v1/cars")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCar)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("USER не может создавать - 403")
        void user_CannotCreate_ReturnsForbidden() throws Exception {
            CarDto newCar = getValidCreateDto();

            mockMvc.perform(post("/api/v1/cars")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCar)))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // PUT /api/v1/cars/{id} - проверка прав на обновление
    // ============================================================

    @Nested
    @DisplayName("PUT /api/v1/cars/{id} - проверка прав на обновление")
    @Transactional
    class UpdateCarSecurityTests {

        private Long existingCarId;

        @BeforeEach
        void setUp() {
            Car car = Car.builder()
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();
            existingCarId = carRepository.save(car).getId();
        }

        /**
         * Создает валидный DTO для обновления автомобиля.
         */
        private CarDto getValidUpdateDto() {
            return new CarDto(
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
        }

        @Test
        @DisplayName("ADMIN может обновлять - 200")
        void admin_CanUpdate_ReturnsOk() throws Exception {
            CarDto updateData = getValidUpdateDto();

            mockMvc.perform(put("/api/v1/cars/" + existingCarId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USER не может обновлять - 403")
        void user_CannotUpdate_ReturnsForbidden() throws Exception {
            CarDto updateData = getValidUpdateDto();

            mockMvc.perform(put("/api/v1/cars/" + existingCarId)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // DELETE /api/v1/cars/{id} - проверка прав на удаление
    // ============================================================

    @Nested
    @DisplayName("DELETE /api/v1/cars/{id} - проверка прав на удаление")
    @Transactional
    class DeleteCarSecurityTests {

        private Long existingCarId;

        @BeforeEach
        void setUp() {
            Car car = Car.builder()
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();
            existingCarId = carRepository.save(car).getId();
        }

        @Test
        @DisplayName("ADMIN может удалять - 204")
        void admin_CanDelete_ReturnsNoContent() throws Exception {
            mockMvc.perform(delete("/api/v1/cars/" + existingCarId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("USER не может удалять - 403")
        void user_CannotDelete_ReturnsForbidden() throws Exception {
            mockMvc.perform(delete("/api/v1/cars/" + existingCarId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
// Диагностические тесты
// ============================================================

    @Nested
    @DisplayName("Диагностика окружения")
    @Transactional
    class EnvironmentDiagnosticsTests {

        @BeforeAll
        static void setupDiagnostics() {
            log.info("🔍 Запуск диагностических тестов");
        }

        @Test
        @DisplayName("Проверка порта базы данных")
        void databasePort_ShouldBeValid() {
            int port = getDatabasePort();
            log.info("Порт базы данных: {}", port);
            assertThat(port).isBetween(1024, 65535);
        }

        @Test
        @DisplayName("Проверка инициализации базы данных")
        void databaseInitialized_ShouldBeTrue() {
            boolean initialized = isDatabaseInitialized();
            log.info("База данных инициализирована: {}", initialized);
            assertThat(initialized).isTrue();
        }
    }
}