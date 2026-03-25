package com.carsensor.common.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class TempDirInitializer {

    private static final Logger log = Logger.getLogger(TempDirInitializer.class.getName());
    private static volatile boolean initialized = false;

    static {
        initialize();
    }

    private static void initialize() {
        if (initialized) {
            return;
        }

        try {
            // Получаем корневую директорию проекта
            String userDir = System.getProperty("user.dir");

            /// Определяем правильный путь
            // Определяем правильный путь
            Path tmpPath = (userDir.endsWith("auth-service") || userDir.contains("services/auth-service"))
                    ? Path.of(userDir, "target", "tmp")
                    : Path.of(userDir, "services", "auth-service", "target", "tmp");

            // Создаем директорию
            if (!Files.exists(tmpPath)) {
                Files.createDirectories(tmpPath);
                log.info("Created temp directory: " + tmpPath.toAbsolutePath());
            }

            // Устанавливаем system property
            String absolutePath = tmpPath.toAbsolutePath().toString();
            System.setProperty("java.io.tmpdir", absolutePath);
            log.info("Set java.io.tmpdir to: " + absolutePath);

            initialized = true;

        } catch (Exception e) {
            log.severe("Failed to initialize temp directory: " + e.getMessage());
            // Используем системную tmp как fallback
            String systemTmp = System.getProperty("java.io.tmpdir", "/tmp");
            System.setProperty("java.io.tmpdir", systemTmp);
            log.info("Using system temp directory: " + systemTmp);
            initialized = true;
        }
    }

    public static void ensureInitialized() {
        // Просто вызываем статический блок
    }
}