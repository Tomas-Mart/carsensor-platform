package com.carsensor.car.integration.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import com.carsensor.car.CarServiceApplication;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.common.test.util.TestJwtUtils;
import com.carsensor.platform.dto.CarDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E интеграционные тесты CarController.
 *
 * <p>Использует реальный HTTP сервер на случайном порту и TestRestTemplate.
 */
@SpringBootTest(
        classes = CarServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("E2E интеграционные тесты CarController")
class CarControllerE2ETest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();
        adminToken = TestJwtUtils.createAdminToken();
        baseUrl = "http://localhost:" + port;

        // Небольшая задержка для старта сервера
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cars - фильтрация")
    class GetCarsE2ETests {

        @BeforeEach
        void setUp() {
            carRepository.deleteAll();
            createTestCars();
        }

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

        @Test
        @DisplayName("Получение всех автомобилей")
        void getAllCars_ReturnsAllCars() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            var response = restTemplate.exchange(
                    baseUrl + "/api/v1/cars",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("Toyota", "Honda");
        }

        @Test
        @DisplayName("Фильтрация по марке Toyota")
        void filterByBrand_Toyota_ReturnsToyotaCars() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            var response = restTemplate.exchange(
                    baseUrl + "/api/v1/cars?brand=Toyota",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("Camry", "RAV4");
            assertThat(response.getBody()).doesNotContain("Honda");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/cars - создание")
    class CreateCarE2ETests {

        @Test
        @DisplayName("Создание автомобиля с валидными данными")
        void validData_CreatesCar() {
            var headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            CarDto newCar = CarDto.builder()
                    .brand("Mazda")
                    .model("CX-5")
                    .year(2022)
                    .mileage(15000)
                    .price(new BigDecimal("2800000"))
                    .build();

            var response = restTemplate.exchange(
                    baseUrl + "/api/v1/cars",
                    HttpMethod.POST,
                    new HttpEntity<>(newCar, headers),
                    CarDto.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(Objects.requireNonNull(response.getBody()).id()).isNotNull();
            assertThat(response.getBody().brand()).isEqualTo("Mazda");
        }
    }
}