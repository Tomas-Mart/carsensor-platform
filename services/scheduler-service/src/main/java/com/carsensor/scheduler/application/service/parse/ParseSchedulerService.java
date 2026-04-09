package com.carsensor.scheduler.application.service.parse;

/**
 * Управление расписанием парсинга
 */
public interface ParseSchedulerService {

    /**
     * Настройка расписания парсинга
     *
     * @param cronExpression cron выражение (например, "0 0 * * * *")
     */
    void scheduleParsing(String cronExpression);

    /**
     * Запуск запланированного парсинга
     */
    void startScheduledParsing();

    /**
     * Остановка запланированного парсинга
     */
    void stopScheduledParsing();

    /**
     * Получение текущего расписания
     *
     * @return cron выражение или null если не настроено
     */
    String getCurrentSchedule();

    /**
     * Проверка, запущен ли запланированный парсинг
     *
     * @return true если запущен, false в противном случае
     */
    boolean isScheduledRunning();
}