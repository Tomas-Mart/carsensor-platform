package com.carsensor.scheduler.application.service.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import com.carsensor.scheduler.application.service.DictionaryService;
import com.carsensor.scheduler.domain.dictionary.JapaneseCarDictionary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация сервиса словаря для перевода японских терминов
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryServiceImpl implements DictionaryService {

    private final JapaneseCarDictionary japaneseCarDictionary;

    // Статистика изменений словаря
    private final Map<Category, AtomicInteger> lastUpdatedTerms = new ConcurrentHashMap<>();
    private LocalDateTime lastExportDate;

    // Путь для экспорта/импорта словаря
    private static final String DICTIONARY_EXPORT_PATH = "config/dictionary/export/";
    private static final String DICTIONARY_IMPORT_PATH = "config/dictionary/import/";

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

    @Override
    public String findBrandInText(String text) {
        log.debug("Finding brand in text: {}", text);
        Optional<String> brand = japaneseCarDictionary.findBrand(text);
        if (brand.isPresent()) {
            log.debug("Found brand: {} in text", brand.get());
            return brand.get();
        }
        log.debug("No brand found in text");
        return null;
    }

    @Override
    public String findModelInText(String text) {
        log.debug("Finding model in text: {}", text);
        Optional<String> model = japaneseCarDictionary.findModel(text);
        if (model.isPresent()) {
            log.debug("Found model: {} in text", model.get());
            return model.get();
        }
        log.debug("No model found in text");
        return null;
    }

    @Override
    public void addTerm(String category, String japaneseTerm, String normalizedTerm) {
        log.info("Adding term - Category: {}, Japanese: {}, Normalized: {}",
                category, japaneseTerm, normalizedTerm);

        try {
            Category cat = Category.valueOf(category.toUpperCase());

            switch (cat) {
                case BRAND:
                    // Добавление в словарь марок
                    Map<String, String> brandMap = new HashMap<>(japaneseCarDictionary.getBrandMapping());
                    brandMap.put(japaneseTerm, normalizedTerm);
                    updateBrandMapping(brandMap);
                    break;

                case MODEL:
                    // Добавление в словарь моделей
                    Map<String, String> modelMap = new HashMap<>(japaneseCarDictionary.getModelMapping());
                    modelMap.put(japaneseTerm, normalizedTerm);
                    updateModelMapping(modelMap);
                    break;

                case TRANSMISSION:
                    // Добавление в словарь трансмиссий
                    Map<String, String> transmissionMap = new HashMap<>(japaneseCarDictionary.getTransmissionMapping());
                    transmissionMap.put(japaneseTerm, normalizedTerm);
                    updateTransmissionMapping(transmissionMap);
                    break;

                case DRIVE_TYPE:
                    // Добавление в словарь типов привода
                    Map<String, String> driveTypeMap = new HashMap<>(japaneseCarDictionary.getDriveTypeMapping());
                    driveTypeMap.put(japaneseTerm, normalizedTerm);
                    updateDriveTypeMapping(driveTypeMap);
                    break;

                case COLOR:
                    // Добавление в словарь цветов
                    Map<String, String> colorMap = new HashMap<>(japaneseCarDictionary.getColorMapping());
                    colorMap.put(japaneseTerm, normalizedTerm);
                    updateColorMapping(colorMap);
                    break;
            }

            // Обновление статистики
            lastUpdatedTerms.computeIfAbsent(cat, k -> new AtomicInteger(0))
                    .incrementAndGet();

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

    @Override
    public void importDictionary(String filePath) {
        log.info("Importing dictionary from file: {}", filePath);

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.error("Dictionary file not found: {}", filePath);
            throw new RuntimeException("Dictionary file not found: " + filePath);
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int importedCount = 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    String category = parts[0].trim();
                    String japaneseTerm = parts[1].trim();
                    String normalizedTerm = parts[2].trim();

                    try {
                        addTerm(category, japaneseTerm, normalizedTerm);
                        importedCount++;
                    } catch (Exception e) {
                        log.warn("Failed to import term: {} | {} | {} - {}",
                                category, japaneseTerm, normalizedTerm, e.getMessage());
                    }
                }
            }

            log.info("Dictionary import completed. Imported {} terms", importedCount);

        } catch (IOException e) {
            log.error("Error importing dictionary from file: {}", filePath, e);
            throw new RuntimeException("Error importing dictionary", e);
        }
    }

    @Override
    public void exportDictionary(String filePath) {
        log.info("Exporting dictionary to file: {}", filePath);

        // Создание директории если не существует
        try {
            Path path = Paths.get(filePath).getParent();
            if (path != null) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            log.error("Error creating export directory", e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(filePath), StandardCharsets.UTF_8)) {

            // Запись заголовка
            writer.write("# Dictionary Export\n");
            writer.write("# Format: CATEGORY|JAPANESE_TERM|NORMALIZED_TERM\n\n");

            // Экспорт всех категорий
            exportCategory(writer, "BRAND", japaneseCarDictionary.getBrandMapping());
            exportCategory(writer, "MODEL", japaneseCarDictionary.getModelMapping());
            exportCategory(writer, "TRANSMISSION", japaneseCarDictionary.getTransmissionMapping());
            exportCategory(writer, "DRIVE_TYPE", japaneseCarDictionary.getDriveTypeMapping());
            exportCategory(writer, "COLOR", japaneseCarDictionary.getColorMapping());

            lastExportDate = LocalDateTime.now();
            log.info("Dictionary export completed to: {}", filePath);

        } catch (IOException e) {
            log.error("Error exporting dictionary to file: {}", filePath, e);
            throw new RuntimeException("Error exporting dictionary", e);
        }
    }

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
                lastExportDate
        );
    }

    // Приватные вспомогательные методы

    private void exportCategory(BufferedWriter writer, String categoryName,
                                Map<String, String> mapping) throws IOException {
        writer.write("\n# " + categoryName + "\n");
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            writer.write(String.format("%s|%s|%s\n",
                    categoryName,
                    entry.getKey(),
                    entry.getValue()));
        }
    }

    // Методы для обновления маппингов (требуют доступа к private полям JapaneseCarDictionary)
    // В реальном проекте нужно добавить соответствующие методы в JapaneseCarDictionary

    private void updateBrandMapping(Map<String, String> newMapping) {
        // Этот метод требует модификации JapaneseCarDictionary
        // Пока используем рефлексию или добавляем метод в JapaneseCarDictionary
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