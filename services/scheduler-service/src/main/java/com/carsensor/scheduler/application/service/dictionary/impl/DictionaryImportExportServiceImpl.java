package com.carsensor.scheduler.application.service.dictionary.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.carsensor.scheduler.application.service.dictionary.DictionaryImportExportService;
import com.carsensor.scheduler.application.service.dictionary.DictionaryManagementService;
import com.carsensor.scheduler.domain.dictionary.JapaneseCarDictionary;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryImportExportServiceImpl implements DictionaryImportExportService {

    private final JapaneseCarDictionary japaneseCarDictionary;

    private final DictionaryManagementService dictionaryManagementService;

    @Getter
    private LocalDateTime lastExportDate;

    @Override
    public void importDictionary(String filePath) {
        log.info("Importing dictionary from file: {}", filePath);

        if (filePath == null || filePath.trim().isEmpty()) {
            log.error("File path is null or empty");
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            log.error("Dictionary file not found: {}", filePath);
            throw new RuntimeException("Dictionary file not found: " + filePath);
        }

        if (!Files.isReadable(path)) {
            log.error("Dictionary file is not readable: {}", filePath);
            throw new RuntimeException("Dictionary file is not readable: " + filePath);
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
                        dictionaryManagementService.addTerm(category, japaneseTerm, normalizedTerm);
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

        Path path = Paths.get(filePath);

        // Создание директории если не существует
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            log.error("Error creating export directory", e);
            throw new RuntimeException("Failed to create export directory: " + filePath, e);
        }

        // Запись словаря в файл с try-with-resources (автоматическое закрытие)
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("# Dictionary Export\n");
            writer.write("# Format: CATEGORY|JAPANESE_TERM|NORMALIZED_TERM\n\n");

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

    private void exportCategory(
            BufferedWriter writer, String categoryName,
            Map<String, String> mapping
    ) throws IOException {
        writer.write("\n# " + categoryName + "\n");
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            writer.write(String.format("%s|%s|%s\n",
                    categoryName,
                    entry.getKey(),
                    entry.getValue()));
        }
    }
}