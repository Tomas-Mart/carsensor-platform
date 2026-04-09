package com.carsensor.scheduler.application.service.dictionary;

/**
 * Сервис импорта/экспорта словаря
 */
public interface DictionaryImportExportService {

    void importDictionary(String filePath);

    void exportDictionary(String filePath);
}