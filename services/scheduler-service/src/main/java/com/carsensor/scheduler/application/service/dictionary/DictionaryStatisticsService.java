package com.carsensor.scheduler.application.service.dictionary;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Сервис статистики словаря
 */
public interface DictionaryStatisticsService {

    DictionaryStatistics getStatistics();

    record DictionaryStatistics(
            int totalTerms,
            Map<Category, Integer> termsPerCategory,
            int lastUpdatedTerms,
            LocalDateTime lastExportDate
    ) {
    }

    enum Category {
        BRAND, MODEL, TRANSMISSION, DRIVE_TYPE, COLOR
    }
}