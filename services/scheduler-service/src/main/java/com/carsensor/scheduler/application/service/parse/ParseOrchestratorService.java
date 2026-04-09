package com.carsensor.scheduler.application.service.parse;

import java.util.List;
import com.carsensor.platform.dto.CarDto;

/**
 * Оркестратор парсинга - координация всех сервисов.
 *
 * <p>Предоставляет единую точку входа для запуска операций парсинга,
 * управления состоянием и получения истории.
 */
public interface ParseOrchestratorService {

    /**
     * Запуск ручного парсинга (по умолчанию с максимальным количеством страниц).
     *
     * @return список сохраненных автомобилей
     */
    List<CarDto> parseManually();

    /**
     * Запуск запланированного парсинга (по расписанию).
     *
     * @return список сохраненных автомобилей
     */
    List<CarDto> parseScheduled();

    /**
     * Остановка текущего процесса парсинга.
     */
    void stopCurrentParsing();

    /**
     * Получение текущего статуса парсинга.
     *
     * @return статус парсинга
     */
    ParseStateService.ParseStatus getStatus();

    /**
     * Получение истории парсинга.
     *
     * @param limit максимальное количество записей
     * @return список записей истории
     */
    List<ParseHistoryService.ParseHistoryRecord> getHistory(int limit);

    /**
     * Проверка, выполняется ли парсинг в данный момент.
     *
     * @return true если парсинг выполняется, false в противном случае
     */
    boolean isParsingInProgress();

    /**
     * Получение последнего результата парсинга.
     *
     * @return результат последнего парсинга
     */
    ParseStateService.ParseResult getLastResult();

    /**
     * Отмена текущего парсинга (принудительная остановка).
     */
    void cancelParsing();
}