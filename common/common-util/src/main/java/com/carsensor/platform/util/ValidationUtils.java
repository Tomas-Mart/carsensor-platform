package com.carsensor.platform.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

/**
 * Утилиты для валидации
 */
@UtilityClass
public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+7|8)[\\s-]?\\(?\\d{3}\\)?[\\s-]?\\d{3}[\\s-]?\\d{2}[\\s-]?\\d{2}$"
    );

    /**
     * Проверка корректности email
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Проверка корректности телефона
     */
    public boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        // Удаляем все пробелы, дефисы, скобки для проверки
        String cleaned = phone.replaceAll("[\\s\\-()]", "");
        return PHONE_PATTERN.matcher(cleaned).matches();
    }

    /**
     * Проверка, что цена положительная
     */
    public boolean isPositivePrice(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Проверка года выпуска
     */
    public boolean isValidYear(Integer year) {
        if (year == null) {
            return false;
        }
        int currentYear = java.time.Year.now().getValue();
        return year >= 1900 && year <= currentYear + 1;
    }
}