package com.carsensor.scheduler.application.service.dictionary;

/**
 * Фасад словаря, объединяющий все операции
 * Используется, когда нужен единый point of entry
 */
public interface DictionaryFacade extends
        DictionaryNormalizationService,
        DictionarySearchService,
        DictionaryManagementService,
        DictionaryImportExportService,
        DictionaryStatisticsService {
}