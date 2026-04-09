package com.carsensor.scheduler.application.service.dictionary.impl;

import java.util.Map;
import org.springframework.stereotype.Service;
import com.carsensor.scheduler.application.service.dictionary.DictionaryFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryFacadeImpl implements DictionaryFacade {

    private final DictionarySearchServiceImpl searchService;
    private final DictionaryStatisticsServiceImpl statisticsService;
    private final DictionaryManagementServiceImpl managementService;
    private final DictionaryImportExportServiceImpl importExportService;
    private final DictionaryNormalizationServiceImpl normalizationService;

    @Override
    public String normalizeBrand(String japaneseBrand) {
        return normalizationService.normalizeBrand(japaneseBrand);
    }

    @Override
    public String normalizeModel(String japaneseModel) {
        return normalizationService.normalizeModel(japaneseModel);
    }

    @Override
    public String normalizeTransmission(String japaneseTransmission) {
        return normalizationService.normalizeTransmission(japaneseTransmission);
    }

    @Override
    public String normalizeDriveType(String japaneseDriveType) {
        return normalizationService.normalizeDriveType(japaneseDriveType);
    }

    @Override
    public String normalizeColor(String japaneseColor) {
        return normalizationService.normalizeColor(japaneseColor);
    }

    @Override
    public String findBrandInText(String text) {
        return searchService.findBrandInText(text);
    }

    @Override
    public String findModelInText(String text) {
        return searchService.findModelInText(text);
    }

    @Override
    public void addTerm(String category, String japaneseTerm, String normalizedTerm) {
        managementService.addTerm(category, japaneseTerm, normalizedTerm);
    }

    @Override
    public void removeTerm(String category, String japaneseTerm) {
        managementService.removeTerm(category, japaneseTerm);
    }

    @Override
    public Map<String, String> getTermsByCategory(String category) {
        return managementService.getTermsByCategory(category);
    }

    @Override
    public void importDictionary(String filePath) {
        importExportService.importDictionary(filePath);
    }

    @Override
    public void exportDictionary(String filePath) {
        importExportService.exportDictionary(filePath);
    }

    @Override
    public DictionaryStatistics getStatistics() {
        return statisticsService.getStatistics();
    }
}