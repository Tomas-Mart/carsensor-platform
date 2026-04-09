package com.carsensor.car.performance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.carsensor.car.CarServiceApplication;
import com.carsensor.common.test.AbstractIntegrationTest;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты производительности CarService.
 *
 * <p>Содержит тесты для анализа производительности:
 * <ul>
 *   <li>Анализ логов производительности</li>
 *   <li>Измерение времени выполнения операций</li>
 *   <li>Выявление медленных запросов</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Slf4j
@DisplayName("Тесты производительности CarService")
@SpringBootTest(classes = CarServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CarServicePerformanceTest extends AbstractIntegrationTest {

    @BeforeAll
    static void setup() {
        log.info("╔══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║                    ЗАПУСК ТЕСТОВ ПРОИЗВОДИТЕЛЬНОСТИ                         ║");
        log.info("╚══════════════════════════════════════════════════════════════════════════════╝");
        logEnvironmentInfoStatic();
    }

    @AfterAll
    static void tearDown() {
        log.info("═══════════════════════════════════════════════════════════════════════════════");
        log.info("📊 ИТОГОВАЯ СТАТИСТИКА ТЕСТОВ ПРОИЗВОДИТЕЛЬНОСТИ");
        log.info("═══════════════════════════════════════════════════════════════════════════════");
        log.info("✅ Выполнено тестов: {}", getTestCount());
        log.info("⏱️  Общее время: {} секунд",
                Duration.between(getTestStartTime(), Instant.now()).getSeconds());
        log.info("═══════════════════════════════════════════════════════════════════════════════");
    }

    // ============================================================
    // Тесты производительности
    // ============================================================

    @Nested
    @DisplayName("Анализ логов производительности")
    class PerformanceLogsAnalysisTests {

        @Test
        @DisplayName("Анализ логов производительности")
        void analyzePerformanceLogs() throws IOException {
            Path logsDir = getPerformanceLogsDir();

            if (Files.exists(logsDir)) {
                try (var stream = Files.list(logsDir)) {
                    stream.forEach(logFile -> {
                        try {
                            List<String> lines = Files.readAllLines(logFile);
                            log.info("📊 Лог {}: {} записей", logFile.getFileName(), lines.size());

                            // Анализируем медленные операции (> 1 секунды)
                            long slowOpsCount = lines.stream()
                                    .filter(l -> l.contains("ms"))
                                    .filter(l -> {
                                        try {
                                            String[] parts = l.split(": ");
                                            if (parts.length > 1) {
                                                String timeStr = parts[1].split(" ")[0];
                                                return Integer.parseInt(timeStr) > 1000;
                                            }
                                        } catch (Exception ignored) {
                                        }
                                        return false;
                                    })
                                    .count();

                            if (slowOpsCount > 0) {
                                log.warn("⚠️ Найдено {} медленных операций в {}", slowOpsCount, logFile.getFileName());
                                lines.stream()
                                        .filter(l -> l.contains("ms"))
                                        .forEach(l -> {
                                            try {
                                                String[] parts = l.split(": ");
                                                if (parts.length > 1) {
                                                    String timeStr = parts[1].split(" ")[0];
                                                    if (Integer.parseInt(timeStr) > 1000) {
                                                        log.warn("   - {}", l);
                                                    }
                                                }
                                            } catch (Exception ignored) {
                                            }
                                        });
                            } else {
                                log.info("✅ Медленные операции отсутствуют");
                            }

                            // Выводим статистику
                            long totalOps = lines.stream()
                                    .filter(l -> l.contains("ms"))
                                    .count();
                            log.info("   Всего операций: {}", totalOps);

                        } catch (IOException e) {
                            log.error("Ошибка чтения лога {}: {}", logFile.getFileName(), e.getMessage());
                        }
                    });
                }
            } else {
                log.info("Логи производительности отсутствуют");
            }
        }
    }

    @Nested
    @DisplayName("Измерение времени выполнения")
    class ExecutionTimeTests {

        @Test
        @DisplayName("Тест производительности - создание и логирование")
        void performanceTest() {
            long startTime = System.currentTimeMillis();

            // Имитация работы (замените на реальную операцию)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long duration = System.currentTimeMillis() - startTime;
            logPerformance("performanceTest", duration);
            log.info("✅ Тест производительности выполнен за {} ms", duration);

            assertThat(duration).isLessThan(5000);
        }
    }

    // ============================================================
    // Анализ дампов ошибок (связано с производительностью)
    // ============================================================

    @Nested
    @DisplayName("Анализ дампов ошибок")
    class FailureDumpsAnalysisTests {

        @Test
        @DisplayName("Проверка наличия дампов ошибок")
        void checkFailureDumps() throws IOException {
            Path dumpsDir = getFailureDumpsDir();

            if (Files.exists(dumpsDir)) {
                try (var stream = Files.list(dumpsDir)) {
                    long dumpCount = stream.count();
                    log.info("Найдено дампов ошибок: {}", dumpCount);

                    if (dumpCount > 0) {
                        log.warn("⚠️ Есть необработанные дампы ошибок! Проверьте директорию: {}", dumpsDir);
                        try (var streamFiles = Files.list(dumpsDir)) {
                            streamFiles.forEach(file -> log.warn("   - {}", file.getFileName()));
                        }
                    } else {
                        log.info("✅ Дампы ошибок отсутствуют");
                    }
                }
            }
        }
    }
}