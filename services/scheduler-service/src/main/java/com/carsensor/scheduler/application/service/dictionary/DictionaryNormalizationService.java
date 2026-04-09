package com.carsensor.scheduler.application.service.dictionary;

/**
 * Сервис нормализации японских терминов (только чтение)
 */
public interface DictionaryNormalizationService {

    String normalizeBrand(String japaneseBrand);

    String normalizeModel(String japaneseModel);

    String normalizeTransmission(String japaneseTransmission);

    String normalizeDriveType(String japaneseDriveType);

    String normalizeColor(String japaneseColor);
}