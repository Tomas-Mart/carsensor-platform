package com.carsensor.scheduler.application.service.dictionary;

import java.util.Map;

/**
 * Сервис управления терминами словаря
 */
public interface DictionaryManagementService {

    void addTerm(String category, String japaneseTerm, String normalizedTerm);

    void removeTerm(String category, String japaneseTerm);

    Map<String, String> getTermsByCategory(String category);
}