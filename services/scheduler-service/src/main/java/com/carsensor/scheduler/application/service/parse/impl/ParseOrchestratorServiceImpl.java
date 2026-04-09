package com.carsensor.scheduler.application.service.parse.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.scheduler.application.service.parse.ParseCoreService;
import com.carsensor.scheduler.application.service.parse.ParseHistoryService;
import com.carsensor.scheduler.application.service.parse.ParseOrchestratorService;
import com.carsensor.scheduler.application.service.parse.ParseSchedulerService;
import com.carsensor.scheduler.application.service.parse.ParseStateService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParseOrchestratorServiceImpl implements ParseOrchestratorService {

    private final ParseCoreService parseCoreService;
    private final ParseStateService stateService;
    private final ParseHistoryService historyService;
    private final ParseSchedulerService schedulerService;

    @Value("${parser.max-pages:5}")
    private int defaultMaxPages;

    @Override
    @Scheduled(cron = "0 0 * * * *")
    @CircuitBreaker(name = "carParser", fallbackMethod = "parseFallback")
    @Retry(name = "carParser")
    public List<CarDto> parseManually() {
        log.info("Manual parsing triggered");
        return executeParsing(defaultMaxPages, "MANUAL");
    }

    @Override
    public List<CarDto> parseScheduled() {
        log.info("Scheduled parsing triggered");
        return executeParsing(defaultMaxPages, "SCHEDULED");
    }

    private List<CarDto> executeParsing(int maxPages, String triggerType) {
        // Проверка, не выполняется ли уже парсинг
        if (stateService.isParsingInProgress()) {
            log.warn("Parsing already in progress");
            throw new IllegalStateException("Parsing already in progress");
        }

        log.info("Starting {} parsing with max pages: {}", triggerType, maxPages);

        // Начинаем парсинг
        stateService.startParsing();

        LocalDateTime startTime = LocalDateTime.now();
        List<CarDto> result = new ArrayList<>();
        String error = null;
        int parsedCount = 0;
        int savedCount = 0;

        try {
            // Парсинг списка автомобилей
            List<CarDto> parsedCars = parseCoreService.parsePages(maxPages);
            parsedCount = parsedCars.size();

            if (parsedCars.isEmpty()) {
                log.warn("No cars found during parsing");
                // Завершаем с пустым результатом
                stateService.finishParsing(new ParseStateService.ParseResult(
                        startTime, maxPages, 0, 0, result, null
                ));
                return result;
            }

            // Сохраняем результаты
            result.addAll(parsedCars);
            savedCount = result.size();

            log.info("Successfully parsed and saved {} cars", savedCount);

            // Успешное завершение
            stateService.finishParsing(new ParseStateService.ParseResult(
                    startTime, maxPages, parsedCount, savedCount, result, null
            ));

        } catch (Exception e) {
            error = e.getMessage();
            log.error("Error during parsing: {}", error, e);
            stateService.failParsing(error);
            throw new RuntimeException("Parse failed", e);
        } finally {
            addToHistory(startTime, LocalDateTime.now(), maxPages, parsedCount, savedCount, error == null, error);
        }

        return result;
    }

    @Override
    public void stopCurrentParsing() {
        stateService.stopParsing();
    }

    @Override
    public boolean isParsingInProgress() {
        return stateService.isParsingInProgress();
    }

    @Override
    public ParseStateService.ParseStatus getStatus() {
        return stateService.getLastParseStatus();
    }

    @Override
    public List<ParseHistoryService.ParseHistoryRecord> getHistory(int limit) {
        return historyService.getLastRecords(limit);
    }

    @Override
    public ParseStateService.ParseResult getLastResult() {
        ParseStateService.ParseStatus status = stateService.getLastParseStatus();
        return new ParseStateService.ParseResult(
                status.lastStartTime(),
                status.pagesParsed(),
                status.carsFound(),
                status.carsSaved(),
                null,
                status.lastError()
        );
    }

    @Override
    public void cancelParsing() {
        stateService.stopParsing();
        log.info("Parsing cancelled");
    }

    @SuppressWarnings("unused")
    public List<CarDto> parseFallback(Exception e) {
        log.error("Circuit breaker opened for car parser: {}", e.getMessage());
        stateService.failParsing("Circuit breaker opened: " + e.getMessage());
        return List.of();
    }

    private void addToHistory(
            LocalDateTime startTime, LocalDateTime endTime,
            int pagesParsed, int carsFound, int carsSaved,
            boolean success, String errorMessage
    ) {
        ParseHistoryService.ParseHistoryRecord record = historyService.createRecord(
                startTime, endTime, pagesParsed, carsFound, carsSaved, success, errorMessage
        );
        historyService.addRecord(record);
    }
}