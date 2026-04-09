package com.carsensor.scheduler.application.service.dictionary;

/**
 * Сервис поиска терминов в тексте
 */
public interface DictionarySearchService {

    String findBrandInText(String text);

    String findModelInText(String text);
}