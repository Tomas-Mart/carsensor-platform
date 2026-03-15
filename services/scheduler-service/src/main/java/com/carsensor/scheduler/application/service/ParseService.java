package com.carsensor.scheduler.application.service;

import java.time.LocalDateTime;
import java.util.List;
import com.carsensor.platform.dto.CarDto;

/**
 * Интерфейс сервиса парсинга
 */
public interface ParseService {

    /**
     * Запуск парсинга вручную
     */
    List<CarDto> parseManually();

    /**
     * Запуск парсинга с указанным количеством страниц
     */
    List<CarDto> parsePages(int maxPages);

    /**
     * Парсинг конкретного URL
     */
    CarDto parseSingleCar(String url);

    /**
     * Получение статуса последнего парсинга
     */
    ParseStatus getLastParseStatus();

    /**
     * Получение истории парсинга
     */
    List<ParseHistory> getParseHistory(int limit);

    /**
     * Остановка текущего парсинга
     */
    void stopCurrentParsing();

    /**
     * Проверка, выполняется ли парсинг сейчас
     */
    boolean isParsingInProgress();

    /**
     * Настройка расписания парсинга
     */
    void scheduleParsing(String cronExpression);

    /**
     * Статус парсинга
     */
    record ParseStatus(
            boolean inProgress,
            LocalDateTime lastStartTime,
            LocalDateTime lastEndTime,
            int pagesParsed,
            int carsFound,
            int carsSaved,
            String lastError,
            ParseState state
    ) {
    }

    enum ParseState {
        IDLE,
        STARTING,
        PARSING_LIST,
        PARSING_DETAILS,
        SAVING,
        COMPLETED,
        FAILED,
        STOPPED
    }

    /**
     * Запись истории парсинга
     */
    record ParseHistory(
            LocalDateTime startTime,
            LocalDateTime endTime,
            int pagesParsed,
            int carsFound,
            int carsSaved,
            boolean success,
            String errorMessage
    ) {
    }
}