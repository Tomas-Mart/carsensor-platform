package com.carsensor.scheduler.application.service.dictionary.impl;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import com.carsensor.scheduler.application.service.dictionary.DictionaryStatisticsService;
import com.carsensor.scheduler.domain.dictionary.JapaneseCarDictionary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryStatisticsServiceImpl implements DictionaryStatisticsService {

    private final JapaneseCarDictionary japaneseCarDictionary;
    private final Map<Category, AtomicInteger> lastUpdatedTerms = new ConcurrentHashMap<>();
    private final DictionaryImportExportServiceImpl importExportService;

    @Override
    public DictionaryStatistics getStatistics() {
        log.debug("Getting dictionary statistics");

        Map<Category, Integer> termsPerCategory = new EnumMap<>(Category.class);

        termsPerCategory.put(Category.BRAND, japaneseCarDictionary.getBrandMapping().size());
        termsPerCategory.put(Category.MODEL, japaneseCarDictionary.getModelMapping().size());
        termsPerCategory.put(Category.TRANSMISSION, japaneseCarDictionary.getTransmissionMapping().size());
        termsPerCategory.put(Category.DRIVE_TYPE, japaneseCarDictionary.getDriveTypeMapping().size());
        termsPerCategory.put(Category.COLOR, japaneseCarDictionary.getColorMapping().size());

        int totalTerms = termsPerCategory.values().stream().mapToInt(Integer::intValue).sum();

        int lastUpdated = lastUpdatedTerms.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();

        return new DictionaryStatistics(
                totalTerms,
                termsPerCategory,
                lastUpdated,
                importExportService.getLastExportDate()
        );
    }
}