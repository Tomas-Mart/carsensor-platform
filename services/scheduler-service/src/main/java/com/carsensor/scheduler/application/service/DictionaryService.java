package com.carsensor.scheduler.application.service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Интерфейс сервиса словаря для перевода японских терминов
 */
public interface DictionaryService {

    /**
     * Нормализация марки
     */
    String normalizeBrand(String japaneseBrand);

    /**
     * Нормализация модели
     */
    String normalizeModel(String japaneseModel);

    /**
     * Нормализация типа трансмиссии
     */
    String normalizeTransmission(String japaneseTransmission);

    /**
     * Нормализация типа привода
     */
    String normalizeDriveType(String japaneseDriveType);

    /**
     * Нормализация цвета
     */
    String normalizeColor(String japaneseColor);

    /**
     * Поиск марки в тексте
     */
    String findBrandInText(String text);

    /**
     * Поиск модели в тексте
     */
    String findModelInText(String text);

    /**
     * Добавление нового термина в словарь
     */
    void addTerm(String category, String japaneseTerm, String normalizedTerm);

    /**
     * Удаление термина из словаря
     */
    void removeTerm(String category, String japaneseTerm);

    /**
     * Получение всех терминов по категории
     */
    Map<String, String> getTermsByCategory(String category);

    /**
     * Импорт словаря из файла
     */
    void importDictionary(String filePath);

    /**
     * Экспорт словаря в файл
     */
    void exportDictionary(String filePath);

    /**
     * Получение статистики по словарю
     */
    DictionaryStatistics getStatistics();

    /**
     * Категории словаря
     */
    enum Category {
        BRAND,
        MODEL,
        TRANSMISSION,
        DRIVE_TYPE,
        COLOR
    }

    /**
     * Статистика словаря
     */
    record DictionaryStatistics(
            int totalTerms,
            Map<Category, Integer> termsPerCategory,
            int lastUpdatedTerms,
            LocalDateTime lastExportDate
    ) {
    }
}