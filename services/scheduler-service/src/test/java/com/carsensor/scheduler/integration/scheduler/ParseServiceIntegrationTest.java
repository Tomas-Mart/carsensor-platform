package com.carsensor.scheduler.integration.scheduler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.scheduler.application.service.ParseService;
import com.carsensor.scheduler.domain.parser.CarSensorParser;
import com.carsensor.scheduler.infrastructure.client.CarServiceClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@DisplayName("Интеграционные тесты ParseSchedulerService")
class ParseServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ParseService parseService;

    @MockitoBean  // Новая аннотация для Spring Boot 3.4+
    private CarSensorParser carSensorParser;

    @Autowired
    private CarServiceClient carServiceClient;

    private MockWebServer mockWebServer;
    private Thread parsingThread;

    @BeforeEach
    void setUp() throws IOException {
        // Сбрасываем состояние перед каждым тестом
        ReflectionTestUtils.setField(parseService, "parsingInProgress", new AtomicBoolean(false));
        ReflectionTestUtils.setField(parseService, "parseHistory", new ConcurrentLinkedQueue<>());
        ReflectionTestUtils.setField(parseService, "currentStatus",
                new AtomicReference<>(
                        new ParseService.ParseStatus(false, null, null, 0, 0, 0, null, ParseService.ParseState.IDLE)
                ));

        // Настраиваем MockWebServer на случайный свободный порт
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Перенаправляем CarServiceClient на MockWebServer
        ReflectionTestUtils.setField(carServiceClient, "carServiceUrl",
                "http://localhost:" + mockWebServer.getPort());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (parsingThread != null && parsingThread.isAlive()) {
            parsingThread.interrupt();
            try {
                parsingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        mockWebServer.shutdown();
    }

    private MockResponse createSuccessResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    @Nested
    @DisplayName("1. Тесты ручного парсинга")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class ManualParsingTests {

        @BeforeEach
        void setUp() {
            mockWebServer.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
                @Override
                public @NotNull MockResponse dispatch(@NotNull RecordedRequest request) {
                    assert request.getPath() != null;
                    if (request.getPath().equals("/api/v1/cars/batch")) {
                        return createSuccessResponse("[{\"id\":1,\"brand\":\"Toyota\",\"model\":\"Camry\"}," +
                                "{\"id\":2,\"brand\":\"Honda\",\"model\":\"Civic\"}]");
                    }
                    return new MockResponse().setResponseCode(404);
                }
            });
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
        }

        @Test
        @Timeout(5)
        @DisplayName("1.2 Ручной запуск парсинга с пустым результатом")
        void parseManually_WithNoCarsFound_ShouldReturnEmptyList() {
            when(carSensorParser.parseCars(anyInt())).thenReturn(List.of());

            List<CarDto> result = parseService.parseManually();

            assertThat(result).isEmpty();
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
            assertThat(status.lastError()).isNotEmpty();  // Исправлено: isPresent() -> isNotEmpty()
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
                    // Ожидаемо при прерывании
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
            mockWebServer.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
                @Override
                public @NotNull MockResponse dispatch(@NotNull RecordedRequest request) {
                    assert request.getPath() != null;
                    if (request.getPath().equals("/api/v1/cars/batch")) {
                        return createSuccessResponse("[{\"id\":1,\"brand\":\"Toyota\",\"model\":\"Camry\"}]");
                    }
                    return new MockResponse().setResponseCode(404);
                }
            });
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
            mockWebServer.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
                @Override
                public @NotNull MockResponse dispatch(@NotNull RecordedRequest request) {
                    assert request.getPath() != null;
                    if (request.getPath().equals("/api/v1/cars/batch")) {
                        return createSuccessResponse("[{\"id\":1,\"brand\":\"トヨタ\",\"model\":\"カローラ\"}]");
                    }
                    return new MockResponse().setResponseCode(404);
                }
            });
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
            assertThat(result.getFirst().model()).isEqualTo("カローラ");  // Было brand(), теперь model()
        }
    }
}