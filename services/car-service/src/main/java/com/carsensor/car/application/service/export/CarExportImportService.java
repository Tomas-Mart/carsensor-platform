package com.carsensor.car.application.service.export;

import java.util.List;

/**
 * Сервис для экспорта/импорта данных об автомобилях.
 */
public interface CarExportImportService {

    /**
     * Экспорт автомобилей в CSV.
     */
    byte[] exportCarsToCsv(List<Long> carIds);

    /**
     * Импорт автомобилей из CSV.
     */
    void importCarsFromCsv(byte[] csvData);
}