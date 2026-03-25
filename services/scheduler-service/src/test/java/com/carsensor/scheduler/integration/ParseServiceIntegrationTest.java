package com.carsensor.scheduler.integration;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.scheduler.SchedulerApplication;
import com.carsensor.scheduler.application.service.ParseService;
import com.carsensor.scheduler.domain.parser.CarSensorParser;
import com.carsensor.scheduler.infrastructure.client.CarServiceClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SchedulerApplication.class)
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты ParseSchedulerService")
class ParseServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ParseService parseService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private CarSensorParser carSensorParser;

    @Autowired
    private CarServiceClient carServiceClient;

    private WireMockServer wireMockServer;
    private Thread parsingThread;

    @BeforeEach
    void setUp() {
        assertThat(parseService).isNotNull();
        assertThat(carServiceClient).isNotNull();

        // Сбрасываем состояние
        ReflectionTestUtils.setField(parseService, "parsingInProgress", new AtomicBoolean(false));
        ReflectionTestUtils.setField(parseService, "parseHistory", new ConcurrentLinkedQueue<>());
        ReflectionTestUtils.setField(parseService, "currentStatus",
                new AtomicReference<>(
                        new ParseService.ParseStatus(false, null, null, 0, 0, 0, null, ParseService.ParseState.IDLE)
                ));

        // Настраиваем WireMock
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());

        // Перенаправляем клиент на WireMock
        ReflectionTestUtils.setField(carServiceClient, "carServiceUrl",
                "http://localhost:" + wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        if (parsingThread != null && parsingThread.isAlive()) {
            parsingThread.interrupt();
            try {
                parsingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Nested
    @DisplayName("1. Тесты ручного парсинга")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class ManualParsingTests {

        @BeforeEach
        void setUp() {
            wireMockServer.stubFor(post(urlEqualTo("/api/v1/cars/batch"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"id\":1,\"brand\":\"Toyota\",\"model\":\"Camry\"}," +
                                    "{\"id\":2,\"brand\":\"Honda\",\"model\":\"Civic\"}]")));
        }

        @Test
        @Timeout(5)
        @DisplayName("1.1 Ручной запуск парсинга с валидными данными")
        void parseManually_WithValidData_ShouldReturnCars() {
            CarDto testCar1 = CarDto.builder()
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();

            CarDto testCar2 = CarDto.builder()
                    .brand("Honda")
                    .model("Civic")
                    .year(2021)
                    .mileage(30000)
                    .price(new BigDecimal("1800000"))
                    .build();

            when(carSensorParser.parseCars(anyInt())).thenReturn(List.of(testCar1, testCar2));

            List<CarDto> result = parseService.parseManually();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(CarDto::brand).containsExactly("Toyota", "Honda");
            wireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/v1/cars/batch")));
        }

        @Test
        @Timeout(5)
        @DisplayName("1.2 Ручной запуск парсинга с пустым результатом")
        void parseManually_WithNoCarsFound_ShouldReturnEmptyList() {
            when(carSensorParser.parseCars(anyInt())).thenReturn(List.of());

            List<CarDto> result = parseService.parseManually();

            assertThat(result).isEmpty();
            wireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/v1/cars/batch")));
        }

        @Test
        @Timeout(5)
        @DisplayName("1.3 Ручной запуск парсинга при ошибке парсера")
        void parseManually_WhenParserThrowsException_ShouldReturnEmptyListFromFallback() {
            when(carSensorParser.parseCars(anyInt())).thenThrow(new RuntimeException("Parser error"));

            List<CarDto> result = parseService.parseManually();

            assertThat(result).isEmpty();

            ParseService.ParseStatus status = parseService.getLastParseStatus();
            assertThat(status.state()).isEqualTo(ParseService.ParseState.FAILED);
            assertThat(status.lastError()).isNotEmpty();

            wireMockServer.verify(0, postRequestedFor(urlEqualTo("/api/v1/cars/batch")));
        }
    }

    @Nested
    @DisplayName("2. Тесты статуса парсинга")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class ParseStatusTests {

        @Test
        @Timeout(5)
        @DisplayName("2.1 Проверка статуса до запуска парсинга")
        void getLastParseStatus_Initially_ShouldReturnIdle() {
            ParseService.ParseStatus status = parseService.getLastParseStatus();

            assertThat(status).isNotNull();
            assertThat(status.state()).isEqualTo(ParseService.ParseState.IDLE);
            assertThat(status.inProgress()).isFalse();
        }

        @Test
        @Timeout(5)
        @DisplayName("2.2 Проверка статуса во время парсинга")
        void isParsingInProgress_DuringParsing_ShouldReturnTrue() {
            when(carSensorParser.parseCars(anyInt())).thenAnswer(invocation -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return List.of();
                }
                return List.of();
            });

            parsingThread = new Thread(() -> {
                try {
                    parseService.parseManually();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
            parsingThread.start();

            await().atMost(500, TimeUnit.MILLISECONDS)
                    .until(() -> parseService.isParsingInProgress());

            assertThat(parseService.isParsingInProgress()).isTrue();
        }
    }

    @Nested
    @DisplayName("3. Тесты истории парсинга")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class ParseHistoryTests {

        @BeforeEach
        void setUp() {
            wireMockServer.stubFor(post(urlEqualTo("/api/v1/cars/batch"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"id\":1,\"brand\":\"Toyota\",\"model\":\"Camry\"}]")));
        }

        @Test
        @Timeout(5)
        @DisplayName("3.1 Получение истории парсинга после запуска")
        void getParseHistory_AfterParsing_ShouldContainEntries() {
            ReflectionTestUtils.setField(parseService, "parseHistory", new ConcurrentLinkedQueue<>());

            CarDto testCar = CarDto.builder()
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();

            when(carSensorParser.parseCars(anyInt())).thenReturn(List.of(testCar));

            parseService.parseManually();
            List<ParseService.ParseHistory> history = parseService.getParseHistory(10);

            assertThat(history).isNotEmpty();
            ParseService.ParseHistory lastEntry = history.getFirst();
            assertThat(lastEntry.success()).isTrue();
            assertThat(lastEntry.carsFound()).isEqualTo(1);
            assertThat(lastEntry.carsSaved()).isEqualTo(1);
        }

        @Test
        @Timeout(5)
        @DisplayName("3.2 Получение истории с лимитом")
        void getParseHistory_WithLimit_ShouldReturnLimitedEntries() {
            ReflectionTestUtils.setField(parseService, "parseHistory", new ConcurrentLinkedQueue<>());

            CarDto testCar = CarDto.builder()
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();

            when(carSensorParser.parseCars(anyInt())).thenReturn(List.of(testCar));

            for (int i = 0; i < 5; i++) {
                parseService.parseManually();
            }
            List<ParseService.ParseHistory> history = parseService.getParseHistory(3);

            assertThat(history).hasSize(3);
        }
    }

    @Nested
    @DisplayName("4. Тесты остановки парсинга")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class StopParsingTests {

        @BeforeEach
        void setUp() {
            wireMockServer.stubFor(post(urlEqualTo("/api/v1/cars/batch"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"id\":1,\"brand\":\"Toyota\",\"model\":\"Camry\"}]")));
        }

        @Test
        @Timeout(5)
        @DisplayName("4.1 Остановка текущего парсинга")
        void stopCurrentParsing_WhenParsingInProgress_ShouldStopParsing() {
            when(carSensorParser.parseCars(anyInt())).thenAnswer(invocation -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return List.of();
                }
                return List.of();
            });

            parsingThread = new Thread(() -> {
                try {
                    parseService.parseManually();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
            parsingThread.start();

            await().atMost(500, TimeUnit.MILLISECONDS)
                    .until(() -> parseService.isParsingInProgress());

            parseService.stopCurrentParsing();

            await().atMost(1000, TimeUnit.MILLISECONDS)
                    .until(() -> !parseService.isParsingInProgress());

            ParseService.ParseStatus status = parseService.getLastParseStatus();
            assertThat(status.state()).isIn(ParseService.ParseState.STOPPED, ParseService.ParseState.IDLE);
        }

        @Test
        @Timeout(5)
        @DisplayName("4.2 Остановка когда парсинг не запущен")
        void stopCurrentParsing_WhenNoParsingInProgress_ShouldDoNothing() {
            parseService.stopCurrentParsing();
            assertThat(parseService.isParsingInProgress()).isFalse();
        }
    }

    @Nested
    @DisplayName("5. Тесты с реальными данными")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class RealDataParsingTests {

        @BeforeEach
        void setUp() {
            wireMockServer.stubFor(post(urlEqualTo("/api/v1/cars/batch"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"id\":1,\"brand\":\"トヨタ\",\"model\":\"カローラ\"}]")));
        }

        @Test
        @Timeout(5)
        @DisplayName("5.1 Парсинг с японскими символами")
        void parseManually_WithJapaneseCharacters_ShouldHandleCorrectly() {
            ReflectionTestUtils.setField(parseService, "parseHistory", new ConcurrentLinkedQueue<>());

            CarDto rawCar = CarDto.builder()
                    .brand("トヨタ")
                    .model("カローラ")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();

            when(carSensorParser.parseCars(anyInt())).thenReturn(List.of(rawCar));

            List<CarDto> result = parseService.parseManually();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().brand()).isEqualTo("トヨタ");
            assertThat(result.getFirst().model()).isEqualTo("カローラ");

            wireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/v1/cars/batch"))
                    .withRequestBody(matchingJsonPath("$[0].brand", equalTo("トヨタ")))
                    .withRequestBody(matchingJsonPath("$[0].model", equalTo("カローラ"))));
        }
    }

    @Nested
    @DisplayName("6. Тесты обработки ошибок car-service")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class CarServiceErrorTests {

        @Test
        @Timeout(10)
        @DisplayName("6.1 Ошибка 500 от car-service")
        void parseManually_WhenCarServiceReturns500_ShouldHandleGracefully() {
            wireMockServer.stubFor(post(urlEqualTo("/api/v1/cars/batch"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withBody("Internal Server Error")));

            CarDto testCar = CarDto.builder()
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();

            when(carSensorParser.parseCars(anyInt())).thenReturn(List.of(testCar));

            List<CarDto> result = parseService.parseManually();

            assertThat(result).isEmpty();

            ParseService.ParseStatus status = parseService.getLastParseStatus();
            assertThat(status.state()).isEqualTo(ParseService.ParseState.FAILED);
        }

        @Test
        @Timeout(15)
        @DisplayName("6.2 Таймаут car-service")
        void parseManually_WhenCarServiceTimeout_ShouldHandleGracefully() {
            wireMockServer.stubFor(post(urlEqualTo("/api/v1/cars/batch"))
                    .willReturn(aResponse()
                            .withFixedDelay(8000)
                            .withStatus(200)
                            .withBody("[{\"id\":1,\"brand\":\"Toyota\",\"model\":\"Camry\"}]")));

            CarDto testCar = CarDto.builder()
                    .brand("Toyota")
                    .model("Camry")
                    .year(2020)
                    .mileage(50000)
                    .price(new BigDecimal("2500000"))
                    .build();

            when(carSensorParser.parseCars(anyInt())).thenReturn(List.of(testCar));

            List<CarDto> result = parseService.parseManually();

            assertThat(result).isEmpty();
        }
    }
}