package com.carsensor.car.integration.controller;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.common.test.util.TestJwtUtils;
import com.carsensor.platform.dto.CarDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Бизнес-тесты CarController")
class CarControllerBusinessTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarRepository carRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();
        adminToken = TestJwtUtils.createAdminToken();
    }

    private void createTestCars() {
        Car car1 = Car.builder()
                .brand("Toyota")
                .model("Camry")
                .year(2020)
                .mileage(50000)
                .price(new BigDecimal("2500000"))
                .build();

        Car car2 = Car.builder()
                .brand("Toyota")
                .model("RAV4")
                .year(2021)
                .mileage(30000)
                .price(new BigDecimal("3500000"))
                .build();

        Car car3 = Car.builder()
                .brand("Honda")
                .model("Civic")
                .year(2022)
                .mileage(10000)
                .price(new BigDecimal("1800000"))
                .build();

        carRepository.saveAll(List.of(car1, car2, car3));
    }

    @Nested
    @DisplayName("GET /api/v1/cars - фильтрация и пагинация")
    class GetCarsBusinessTests {

        @BeforeEach
        void setUp() {
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
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].brand").value("Toyota"))
                    .andExpect(jsonPath("$.content[1].brand").value("Toyota"));
        }

        @Test
        @DisplayName("Фильтрация по марке Honda")
        void filterByBrand_Honda_ReturnsHondaCars() throws Exception {
            mockMvc.perform(get("/api/v1/cars")
                            .param("brand", "Honda")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].brand").value("Honda"));
        }

        @Test
        @DisplayName("Фильтрация по диапазону цен")
        void filterByPriceRange_ReturnsCarsInRange() throws Exception {
            mockMvc.perform(get("/api/v1/cars")
                            .param("priceFrom", "2000000")
                            .param("priceTo", "3000000")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1)); // Только Camry
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

    @Nested
    @DisplayName("GET /api/v1/cars/{id} - получение по ID")
    class GetCarByIdBusinessTests {

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

    @Nested
    @DisplayName("POST /api/v1/cars - создание")
    class CreateCarBusinessTests {

        @Test
        @DisplayName("Валидные данные - создает автомобиль")
        void validData_CreatesCar() throws Exception {
            CarDto newCar = CarDto.builder()
                    .brand("Mazda")
                    .model("CX-5")
                    .year(2022)
                    .mileage(15000)
                    .price(new BigDecimal("2800000"))
                    .build();

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
            CarDto invalidCar = CarDto.builder()
                    .brand("")
                    .year(1800)
                    .mileage(-100)
                    .build();

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
            CarDto car1 = CarDto.builder()
                    .brand("Mazda")
                    .model("CX-5")
                    .externalId("EXT001")
                    .year(2022)
                    .mileage(15000)
                    .price(new BigDecimal("2800000"))
                    .build();

            mockMvc.perform(post("/api/v1/cars")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(car1)))
                    .andExpect(status().isCreated());

            CarDto car2 = CarDto.builder()
                    .brand("Mazda")
                    .model("CX-5")
                    .externalId("EXT001")
                    .year(2023)
                    .mileage(5000)
                    .price(new BigDecimal("3200000"))
                    .build();

            mockMvc.perform(post("/api/v1/cars")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(car2)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("DUPLICATE_RESOURCE"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/cars/{id} - обновление")
    class UpdateCarBusinessTests {

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
        @DisplayName("Существующий ID - обновляет автомобиль")
        void existingId_UpdatesCar() throws Exception {
            CarDto updateData = CarDto.builder()
                    .brand("Toyota")
                    .model("Camry Updated")
                    .year(2021)
                    .mileage(45000)
                    .price(new BigDecimal("2400000"))
                    .build();

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
            CarDto updateData = CarDto.builder()
                    .brand("Toyota")
                    .model("Camry")
                    .year(2021)
                    .mileage(45000)
                    .price(new BigDecimal("2400000"))
                    .build();

            mockMvc.perform(put("/api/v1/cars/999")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/cars/{id} - удаление")
    class DeleteCarBusinessTests {

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