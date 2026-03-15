package com.carsensor.car.integration.controller;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.common.test.util.TestJwtUtils;
import com.carsensor.platform.dto.CarDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты безопасности CarController
 * Проверяют авторизацию, аутентификацию и права доступа
 */
@AutoConfigureWireMock(port = 0)
@DisplayName("Тесты безопасности CarController")
class CarControllerSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;
    private String invalidToken = "invalid.token.here";

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();

        adminToken = TestJwtUtils.createAdminToken();
        userToken = TestJwtUtils.createUserToken();

        createTestCar();
    }

    private void createTestCar() {
        com.carsensor.car.domain.entity.Car car = com.carsensor.car.domain.entity.Car.builder()
                .brand("Toyota")
                .model("Camry")
                .year(2020)
                .mileage(50000)
                .price(new BigDecimal("2500000"))
                .build();
        carRepository.save(car);
    }

    @Nested
    @DisplayName("GET /api/v1/cars - проверка доступа")
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
            mockMvc.perform(get("/api/v1/cars")
                            .header("Authorization", "Bearer " + invalidToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/cars - проверка прав")
    class CreateCarSecurityTests {

        private CarDto getValidCreateDto() {
            return CarDto.builder()
                    .brand("Mazda")
                    .model("CX-5")
                    .year(2022)
                    .mileage(15000)
                    .price(new BigDecimal("2800000"))
                    .build();
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

    @Nested
    @DisplayName("PUT /api/v1/cars/{id} - проверка прав")
    class UpdateCarSecurityTests {

        private CarDto getValidUpdateDto() {
            return CarDto.builder()
                    .brand("Toyota")
                    .model("Camry Updated")
                    .year(2021)
                    .mileage(45000)
                    .price(new BigDecimal("2400000"))
                    .build();
        }

        private Long createTestCarAndGetId() {
            com.carsensor.car.domain.entity.Car car = com.carsensor.car.domain.entity.Car.builder()
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();
            return carRepository.save(car).getId();
        }

        @Test
        @DisplayName("ADMIN может обновлять - 200")
        void admin_CanUpdate_ReturnsOk() throws Exception {
            Long carId = createTestCarAndGetId();
            CarDto updateData = getValidUpdateDto();

            mockMvc.perform(put("/api/v1/cars/" + carId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USER не может обновлять - 403")
        void user_CannotUpdate_ReturnsForbidden() throws Exception {
            Long carId = createTestCarAndGetId();
            CarDto updateData = getValidUpdateDto();

            mockMvc.perform(put("/api/v1/cars/" + carId)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/cars/{id} - проверка прав")
    class DeleteCarSecurityTests {

        @Test
        @DisplayName("ADMIN может удалять - 204")
        void admin_CanDelete_ReturnsNoContent() throws Exception {
            // Создаем автомобиль специально для этого теста
            createTestCar();

            mockMvc.perform(delete("/api/v1/cars/1")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("USER не может удалять - 403")
        void user_CannotDelete_ReturnsForbidden() throws Exception {
            // Создаем автомобиль специально для этого теста
            createTestCar();

            mockMvc.perform(delete("/api/v1/cars/1")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }
    }
}