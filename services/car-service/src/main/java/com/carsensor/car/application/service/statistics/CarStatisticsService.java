package com.carsensor.car.application.service.statistics;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Сервис для статистики по автомобилям.
 */
public interface CarStatisticsService {

    /**
     * Получение общего количества автомобилей.
     */
    long getTotalCarsCount();

    /**
     * Получение статистики по маркам.
     */
    Map<String, Long> getStatisticsByBrand();

    /**
     * Получение полной статистики по автомобилям.
     */
    CarStatistics getCarStatistics();

    /**
     * DTO статистики по автомобилям.
     */
    record CarStatistics(
            long totalCars,
            long carsWithPhotos,
            long carsWithoutPhotos,
            double averagePrice,
            double averageMileage,
            int oldestYear,
            int newestYear,
            LocalDateTime lastParsedAt
    ) {
    }
}