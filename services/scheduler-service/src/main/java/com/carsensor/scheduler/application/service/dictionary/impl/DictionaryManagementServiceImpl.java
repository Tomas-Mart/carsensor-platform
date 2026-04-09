package com.carsensor.scheduler.application.service.dictionary.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import com.carsensor.scheduler.application.service.dictionary.DictionaryManagementService;
import com.carsensor.scheduler.application.service.dictionary.DictionaryStatisticsService.Category;
import com.carsensor.scheduler.domain.dictionary.JapaneseCarDictionary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class DictionaryManagementServiceImpl implements DictionaryManagementService {

    private final JapaneseCarDictionary japaneseCarDictionary;
    private final Map<Category, AtomicInteger> lastUpdatedTerms = new ConcurrentHashMap<>();

    @Override
    public void addTerm(String category, String japaneseTerm, String normalizedTerm) {
        log.info("Adding term - Category: {}, Japanese: {}, Normalized: {}",
                category, japaneseTerm, normalizedTerm);

        try {
            Category cat = Category.valueOf(category.toUpperCase());

            switch (cat) {
                case BRAND:
                    Map<String, String> brandMap = new HashMap<>(japaneseCarDictionary.getBrandMapping());
                    brandMap.put(japaneseTerm, normalizedTerm);
                    updateBrandMapping(brandMap);
                    break;
                case MODEL:
                    Map<String, String> modelMap = new HashMap<>(japaneseCarDictionary.getModelMapping());
                    modelMap.put(japaneseTerm, normalizedTerm);
                    updateModelMapping(modelMap);
                    break;
                case TRANSMISSION:
                    Map<String, String> transmissionMap = new HashMap<>(japaneseCarDictionary.getTransmissionMapping());
                    transmissionMap.put(japaneseTerm, normalizedTerm);
                    updateTransmissionMapping(transmissionMap);
                    break;
                case DRIVE_TYPE:
                    Map<String, String> driveTypeMap = new HashMap<>(japaneseCarDictionary.getDriveTypeMapping());
                    driveTypeMap.put(japaneseTerm, normalizedTerm);
                    updateDriveTypeMapping(driveTypeMap);
                    break;
                case COLOR:
                    Map<String, String> colorMap = new HashMap<>(japaneseCarDictionary.getColorMapping());
                    colorMap.put(japaneseTerm, normalizedTerm);
                    updateColorMapping(colorMap);
                    break;
            }

            lastUpdatedTerms.computeIfAbsent(cat, k -> new AtomicInteger(0)).incrementAndGet();
            log.info("Term added successfully to category: {}", category);

        } catch (IllegalArgumentException e) {
            log.error("Invalid category: {}", category);
            throw new IllegalArgumentException("Invalid category: " + category);
        }
    }

    @Override
    public void removeTerm(String category, String japaneseTerm) {
        log.info("Removing term - Category: {}, Japanese: {}", category, japaneseTerm);

        try {
            Category cat = Category.valueOf(category.toUpperCase());

            switch (cat) {
                case BRAND:
                    Map<String, String> brandMap = new HashMap<>(japaneseCarDictionary.getBrandMapping());
                    brandMap.remove(japaneseTerm);
                    updateBrandMapping(brandMap);
                    break;
                case MODEL:
                    Map<String, String> modelMap = new HashMap<>(japaneseCarDictionary.getModelMapping());
                    modelMap.remove(japaneseTerm);
                    updateModelMapping(modelMap);
                    break;
                case TRANSMISSION:
                    Map<String, String> transmissionMap = new HashMap<>(japaneseCarDictionary.getTransmissionMapping());
                    transmissionMap.remove(japaneseTerm);
                    updateTransmissionMapping(transmissionMap);
                    break;
                case DRIVE_TYPE:
                    Map<String, String> driveTypeMap = new HashMap<>(japaneseCarDictionary.getDriveTypeMapping());
                    driveTypeMap.remove(japaneseTerm);
                    updateDriveTypeMapping(driveTypeMap);
                    break;
                case COLOR:
                    Map<String, String> colorMap = new HashMap<>(japaneseCarDictionary.getColorMapping());
                    colorMap.remove(japaneseTerm);
                    updateColorMapping(colorMap);
                    break;
            }

            log.info("Term removed successfully from category: {}", category);

        } catch (IllegalArgumentException e) {
            log.error("Invalid category: {}", category);
            throw new IllegalArgumentException("Invalid category: " + category);
        }
    }

    @Override
    public Map<String, String> getTermsByCategory(String category) {
        log.debug("Getting terms for category: {}", category);

        try {
            Category cat = Category.valueOf(category.toUpperCase());

            return switch (cat) {
                case BRAND -> new HashMap<>(japaneseCarDictionary.getBrandMapping());
                case MODEL -> new HashMap<>(japaneseCarDictionary.getModelMapping());
                case TRANSMISSION -> new HashMap<>(japaneseCarDictionary.getTransmissionMapping());
                case DRIVE_TYPE -> new HashMap<>(japaneseCarDictionary.getDriveTypeMapping());
                case COLOR -> new HashMap<>(japaneseCarDictionary.getColorMapping());
            };

        } catch (IllegalArgumentException e) {
            log.error("Invalid category: {}", category);
            throw new IllegalArgumentException("Invalid category: " + category);
        }
    }

    private void updateBrandMapping(Map<String, String> newMapping) {
        log.warn("Dynamic update of brand mapping not yet implemented");
    }

    private void updateModelMapping(Map<String, String> newMapping) {
        log.warn("Dynamic update of model mapping not yet implemented");
    }

    private void updateTransmissionMapping(Map<String, String> newMapping) {
        log.warn("Dynamic update of transmission mapping not yet implemented");
    }

    private void updateDriveTypeMapping(Map<String, String> newMapping) {
        log.warn("Dynamic update of drive type mapping not yet implemented");
    }

    private void updateColorMapping(Map<String, String> newMapping) {
        log.warn("Dynamic update of color mapping not yet implemented");
    }
}