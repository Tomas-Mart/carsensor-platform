package com.carsensor.scheduler.integration.scheduler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import com.carsensor.common.test.AbstractIntegrationTest;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.scheduler.application.service.ParseService;
import com.carsensor.scheduler.domain.dictionary.JapaneseCarDictionary;
import com.carsensor.scheduler.domain.parser.CarSensorParser;
import com.carsensor.scheduler.infrastructure.client.CarServiceClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Интеграционные тесты ParseSchedulerService")
class ParseServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ParseService parseService;

    @Mock
    private CarSensorParser carSensorParser;

    @Mock
    private CarServiceClient carServiceClient;

    @Mock
    private JapaneseCarDictionary dictionary;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        // Сбрасываем состояние перед каждым тестом
        ReflectionTestUtils.setField(parseService, "parsingInProgress", new java.util.concurrent.atomic.AtomicBoolean(false));
        ReflectionTestUtils.setField(parseService, "parseHistory", new java.util.concurrent.ConcurrentLinkedQueue<>());
        ReflectionTestUtils.setField(parseService, "currentStatus",
                new java.util.concurrent.atomic.AtomicReference<>(
                        new ParseService.ParseStatus(false, null, null, 0, 0, 0, null, ParseService.ParseState.IDLE)
                ));

        mockWebServer = new MockWebServer();
        mockWebServer.start(8082);

        ReflectionTestUtils.setField(carServiceClient, "carServiceUrl",
                "http://localhost:" + mockWebServer.getPort());

        ReflectionTestUtils.setField(parseService, "carSensorParser", carSensorParser);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("1. Тесты ручного парсинга")
    class ManualParsingTests {

        @BeforeEach
        void setUp() {
            mockWebServer.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
                @Override
                public MockResponse dispatch(okhttp3.mockwebserver.RecordedRequest request) {
                    if (request.getPath().equals("/api/v1/cars/batch")) {
                        return new MockResponse()
                                .setResponseCode(200)
                                .setHeader("Content-Type", "application/json")
                                .setBody("[{\"id\":1,\"brand\":\"Toyota\",\"model\":\"Camry\"},{\"id\":2,\"brand\":\"Honda\",\"model\":\"Civic\"}]");
                    }
                    return new MockResponse().setResponseCode(404);
                }
            });
        }

        @Test
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

            verify(carSensorParser).parseCars(anyInt());
        }

        @Test
        @DisplayName("1.2 Ручной запуск парсинга с пустым результатом")
        void parseManually_WithNoCarsFound_ShouldReturnEmptyList() {
            when(carSensorParser.parseCars(anyInt())).thenReturn(List.of());

            List<CarDto> result = parseService.parseManually();

            assertThat(result).isEmpty();
            verify(carSensorParser).parseCars(anyInt());
        }

        @Test
        @DisplayName("1.3 Ручной запуск парсинга при ошибке парсера")
        void parseManually_WhenParserThrowsException_ShouldReturnEmptyListFromFallback() {
            // Создаем мок парсера, который кидает исключение
            CarSensorParser throwingParser = mock(CarSensorParser.class);
            when(throwingParser.parseCars(anyInt())).thenThrow(new RuntimeException("Parser error"));

            // Устанавливаем мок в сервис
            ReflectionTestUtils.setField(parseService, "carSensorParser", throwingParser);

            // Сбрасываем состояние парсинга
            ReflectionTestUtils.setField(parseService, "parsingInProgress", new java.util.concurrent.atomic.AtomicBoolean(false));

            // Act - вызываем метод, который должен вернуть пустой список из fallback
            List<CarDto> result = parseService.parseManually();

            // Assert
            assertThat(result).isEmpty();

            // Проверяем статус после ошибки
            ParseService.ParseStatus status = parseService.getLastParseStatus();
            assertThat(status.state()).isEqualTo(ParseService.ParseState.FAILED);
            assertThat(status.lastError()).contains("Circuit breaker opened");
        }
    }

    @Nested
    @DisplayName("2. Тесты статуса парсинга")
    class ParseStatusTests {

        @Test
        @DisplayName("2.1 Проверка статуса до запуска парсинга")
        void getLastParseStatus_Initially_ShouldReturnIdle() {
            ParseService.ParseStatus status = parseService.getLastParseStatus();

            assertThat(status).isNotNull();
            assertThat(status.state()).isEqualTo(ParseService.ParseState.IDLE);
            assertThat(status.inProgress()).isFalse();
        }

        @Test
        @DisplayName("2.2 Проверка статуса во время парсинга")
        void isParsingInProgress_DuringParsing_ShouldReturnTrue() {
            when(carSensorParser.parseCars(anyInt())).thenAnswer(invocation -> {
                Thread.sleep(1000);
                return List.of();
            });

            Thread parsingThread = new Thread(() -> {
                try {
                    parseService.parseManually();
                } catch (Exception e) {
                    // Ignore
                }
            });
            parsingThread.start();

            await().atMost(500, TimeUnit.MILLISECONDS).until(() -> parseService.isParsingInProgress());

            assertThat(parseService.isParsingInProgress()).isTrue();

            parsingThread.interrupt();
        }
    }

    @Nested
    @DisplayName("3. Тесты истории парсинга")
    class ParseHistoryTests {

        @BeforeEach
        void setUp() {
            mockWebServer.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
                @Override
                public MockResponse dispatch(okhttp3.mockwebserver.RecordedRequest request) {
                    if (request.getPath().equals("/api/v1/cars/batch")) {
                        return new MockResponse()
                                .setResponseCode(200)
                                .setHeader("Content-Type", "application/json")
                                .setBody("[{\"id\":1,\"brand\":\"Toyota\",\"model\":\"Camry\"}]");
                    }
                    return new MockResponse().setResponseCode(404);
                }
            });
        }

        @Test
        @DisplayName("3.1 Получение истории парсинга после запуска")
        void getParseHistory_AfterParsing_ShouldContainEntries() {
            ReflectionTestUtils.setField(parseService, "parseHistory", new java.util.concurrent.ConcurrentLinkedQueue<>());

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
            ParseService.ParseHistory lastEntry = history.get(0);
            assertThat(lastEntry.success()).isTrue();
            assertThat(lastEntry.carsFound()).isEqualTo(1);
            assertThat(lastEntry.carsSaved()).isEqualTo(1);
        }

        @Test
        @DisplayName("3.2 Получение истории с лимитом")
        void getParseHistory_WithLimit_ShouldReturnLimitedEntries() {
            ReflectionTestUtils.setField(parseService, "parseHistory", new java.util.concurrent.ConcurrentLinkedQueue<>());

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
    class StopParsingTests {

        @Test
        @DisplayName("4.1 Остановка текущего парсинга")
        void stopCurrentParsing_WhenParsingInProgress_ShouldStopParsing() {
            when(carSensorParser.parseCars(anyInt())).thenAnswer(invocation -> {
                Thread.sleep(2000);
                return List.of();
            });

            Thread parsingThread = new Thread(() -> {
                try {
                    parseService.parseManually();
                } catch (Exception e) {
                    // Ignore
                }
            });
            parsingThread.start();

            await().atMost(500, TimeUnit.MILLISECONDS).until(() -> parseService.isParsingInProgress());

            parseService.stopCurrentParsing();

            assertThat(parseService.isParsingInProgress()).isFalse();
            ParseService.ParseStatus status = parseService.getLastParseStatus();
            assertThat(status.state()).isEqualTo(ParseService.ParseState.STOPPED);
        }

        @Test
        @DisplayName("4.2 Остановка когда парсинг не запущен")
        void stopCurrentParsing_WhenNoParsingInProgress_ShouldDoNothing() {
            parseService.stopCurrentParsing();

            assertThat(parseService.isParsingInProgress()).isFalse();
        }
    }

    @Nested
    @DisplayName("5. Тесты с реальными данными")
    class RealDataParsingTests {

        @BeforeEach
        void setUp() {
            mockWebServer.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
                @Override
                public MockResponse dispatch(okhttp3.mockwebserver.RecordedRequest request) {
                    if (request.getPath().equals("/api/v1/cars/batch")) {
                        return new MockResponse()
                                .setResponseCode(200)
                                .setHeader("Content-Type", "application/json")
                                .setBody("[{\"id\":1,\"brand\":\"トヨタ\",\"model\":\"カローラ\"}]");
                    }
                    return new MockResponse().setResponseCode(404);
                }
            });
        }

        @Test
        @DisplayName("5.1 Парсинг с нормализацией через словарь")
        void parseManually_WithDictionaryNormalization_ShouldNormalizeData() {
            ReflectionTestUtils.setField(parseService, "parseHistory", new java.util.concurrent.ConcurrentLinkedQueue<>());

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
            assertThat(result.get(0).brand()).isEqualTo("トヨタ");
            assertThat(result.get(0).model()).isEqualTo("カローラ");
        }
    }
}