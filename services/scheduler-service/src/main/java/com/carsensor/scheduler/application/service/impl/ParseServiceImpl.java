package com.carsensor.scheduler.application.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.scheduler.application.service.ParseService;
import com.carsensor.scheduler.config.JwtProperties;
import com.carsensor.scheduler.config.ParserProperties;
import com.carsensor.scheduler.domain.parser.CarSensorParser;
import com.carsensor.scheduler.infrastructure.client.CarServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация сервиса парсинга
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParseServiceImpl implements ParseService {

    private final ParserProperties parserProperties;
    private final JwtProperties jwtProperties;
    private final CarSensorParser carSensorParser;
    private final CarServiceClient carServiceClient;

    @Value("${parser.max-pages:5}")
    private int defaultMaxPages;

    // Состояние парсинга
    private final AtomicBoolean parsingInProgress = new AtomicBoolean(false);
    private final AtomicReference<ParseStatus> currentStatus = new AtomicReference<>(
            new ParseStatus(false, null, null, 0, 0, 0, null, ParseState.IDLE)
    );
    private final ConcurrentLinkedQueue<ParseHistory> parseHistory = new ConcurrentLinkedQueue<>();

    @Override
    @Scheduled(cron = "0 0 * * * *")
    @CircuitBreaker(name = "carParser", fallbackMethod = "parseFallback")
    @Retry(name = "carParser")
    public List<CarDto> parseManually() {
        log.info("Manual parsing triggered");
        return parsePages(defaultMaxPages);
    }

    @Override
    public List<CarDto> parsePages(int maxPages) {
        if (!parsingInProgress.compareAndSet(false, true)) {
            log.warn("Parsing already in progress");
            throw new IllegalStateException("Parsing already in progress");
        }

        log.info("Starting parsing with max pages: {}", maxPages);

        LocalDateTime startTime = LocalDateTime.now();
        List<CarDto> result = new ArrayList<>();
        String error = null;
        ParseState state = ParseState.STARTING;

        try {
            updateStatus(true, startTime, null, 0, 0, 0, null, ParseState.PARSING_LIST);

            List<CarDto> parsedCars = carSensorParser.parseCars(maxPages);

            if (parsedCars.isEmpty()) {
                log.warn("No cars found during parsing");
                updateStatus(false, startTime, LocalDateTime.now(), maxPages, 0, 0, null, ParseState.COMPLETED);
                return result;
            }

            updateStatus(true, startTime, null, maxPages, parsedCars.size(), 0, null, ParseState.SAVING);

            List<CarDto> savedCars = carServiceClient.saveCars(parsedCars);
            result.addAll(savedCars);

            log.info("Successfully parsed and saved {} cars", savedCars.size());

            updateStatus(false, startTime, LocalDateTime.now(), maxPages, parsedCars.size(), savedCars.size(), null, ParseState.COMPLETED);

        } catch (Exception e) {
            error = e.getMessage();
            log.error("Error during parsing: {}", error, e);
            updateStatus(false, startTime, LocalDateTime.now(), maxPages, 0, 0, error, ParseState.FAILED);
            throw new RuntimeException("Parse failed", e);
        } finally {
            parsingInProgress.set(false);
            addToHistory(startTime, LocalDateTime.now(), maxPages, result.size(), result.size(), error == null, error);
        }

        return result;
    }

    @Override
    public CarDto parseSingleCar(String url) {
        log.info("Parsing single car from URL: {}", url);
        return carSensorParser.parseSingleCar(url);
    }

    @Override
    public ParseStatus getLastParseStatus() {
        return currentStatus.get();
    }

    @Override
    public List<ParseHistory> getParseHistory(int limit) {
        return parseHistory.stream()
                .limit(limit)
                .toList();
    }

    @Override
    public void stopCurrentParsing() {
        if (parsingInProgress.get()) {
            log.info("Stopping current parsing process");
            carSensorParser.stopParsing();
            ParseStatus status = currentStatus.get();
            updateStatus(false, status.lastStartTime(), LocalDateTime.now(),
                    status.pagesParsed(), status.carsFound(), status.carsSaved(),
                    "Stopped by user", ParseState.STOPPED);
            parsingInProgress.set(false);
        }
    }

    @Override
    public boolean isParsingInProgress() {
        return parsingInProgress.get();
    }

    @Override
    public void scheduleParsing(String cronExpression) {
        log.info("Scheduling parsing with cron: {}", cronExpression);
        // В реальном проекте здесь должна быть динамическая настройка расписания
        // Например, через TaskScheduler или Quartz
        throw new UnsupportedOperationException("Dynamic scheduling not implemented yet");
    }

    /**
     * Fallback метод для Circuit Breaker
     */
    public List<CarDto> parseFallback(Exception e) {
        log.error("Circuit breaker opened for car parser: {}", e.getMessage());
        ParseStatus status = currentStatus.get();
        updateStatus(false, status.lastStartTime(), LocalDateTime.now(),
                status.pagesParsed(), status.carsFound(), status.carsSaved(),
                "Circuit breaker opened: " + e.getMessage(), ParseState.FAILED);
        return List.of();
    }

    // Приватные вспомогательные методы
    private void updateStatus(boolean inProgress, LocalDateTime startTime, LocalDateTime endTime,
                              int pagesParsed, int carsFound, int carsSaved,
                              String error, ParseState state) {
        currentStatus.set(new ParseStatus(
                inProgress,
                startTime,
                endTime,
                pagesParsed,
                carsFound,
                carsSaved,
                error,
                state
        ));
    }

    private void addToHistory(LocalDateTime startTime, LocalDateTime endTime,
                              int pagesParsed, int carsFound, int carsSaved,
                              boolean success, String errorMessage) {
        parseHistory.offer(new ParseHistory(
                startTime, endTime, pagesParsed, carsFound, carsSaved, success, errorMessage
        ));

        // Ограничиваем историю последними 100 записями
        while (parseHistory.size() > 100) {
            parseHistory.poll();
        }
    }
}