package com.carsensor.platform.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.experimental.UtilityClass;

/**
 * Утилиты для работы с датами
 */
@UtilityClass
public class DateUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter RU_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Парсинг даты из строки ISO формата
     */
    public LocalDateTime parseIso(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Форматирование даты в русский формат
     */
    public String formatRu(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(RU_FORMATTER);
    }

    /**
     * Проверка, что дата не старше указанного количества дней
     */
    public boolean isNotOlderThan(LocalDateTime dateTime, int days) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.isAfter(LocalDateTime.now().minusDays(days));
    }
}