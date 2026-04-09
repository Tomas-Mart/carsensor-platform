package com.carsensor.scheduler.application.service.parse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий истории парсинга
 */
public interface ParseHistoryService {

    /**
     * Добавление записи в историю
     */
    void addRecord(ParseHistoryRecord record);

    /**
     * Получение последних N записей истории
     *
     * @param limit количество записей
     * @return список записей истории
     */
    List<ParseHistoryRecord> getLastRecords(int limit);

    /**
     * Очистка всей истории
     */
    void clearHistory();

    /**
     * Создание записи истории (утилитарный метод)
     */
    ParseHistoryRecord createRecord(
            LocalDateTime startTime, LocalDateTime endTime,
            int pagesParsed, int carsFound, int carsSaved,
            boolean success, String errorMessage
    );

    /**
     * Запись истории парсинга
     */
    record ParseHistoryRecord(
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