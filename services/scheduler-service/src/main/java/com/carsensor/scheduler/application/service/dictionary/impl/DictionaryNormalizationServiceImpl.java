package com.carsensor.scheduler.application.service.dictionary.impl;

import org.springframework.stereotype.Service;
import com.carsensor.scheduler.application.service.dictionary.DictionaryNormalizationService;
import com.carsensor.scheduler.domain.dictionary.JapaneseCarDictionary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryNormalizationServiceImpl implements DictionaryNormalizationService {

    private final JapaneseCarDictionary japaneseCarDictionary;

    @Override
    public String normalizeBrand(String japaneseBrand) {
        log.debug("Normalizing brand: {}", japaneseBrand);
        String normalized = japaneseCarDictionary.normalizeBrand(japaneseBrand);
        log.trace("Normalized brand: {} -> {}", japaneseBrand, normalized);
        return normalized;
    }

    @Override
    public String normalizeModel(String japaneseModel) {
        log.debug("Normalizing model: {}", japaneseModel);
        String normalized = japaneseCarDictionary.normalizeModel(japaneseModel);
        log.trace("Normalized model: {} -> {}", japaneseModel, normalized);
        return normalized;
    }

    @Override
    public String normalizeTransmission(String japaneseTransmission) {
        log.debug("Normalizing transmission: {}", japaneseTransmission);
        String normalized = japaneseCarDictionary.normalizeTransmission(japaneseTransmission);
        log.trace("Normalized transmission: {} -> {}", japaneseTransmission, normalized);
        return normalized;
    }

    @Override
    public String normalizeDriveType(String japaneseDriveType) {
        log.debug("Normalizing drive type: {}", japaneseDriveType);
        String normalized = japaneseCarDictionary.normalizeDriveType(japaneseDriveType);
        log.trace("Normalized drive type: {} -> {}", japaneseDriveType, normalized);
        return normalized;
    }

    @Override
    public String normalizeColor(String japaneseColor) {
        log.debug("Normalizing color: {}", japaneseColor);
        String normalized = japaneseCarDictionary.normalizeColor(japaneseColor);
        log.trace("Normalized color: {} -> {}", japaneseColor, normalized);
        return normalized;
    }
}