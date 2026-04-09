package com.carsensor.auth.domain.valueobject;

import java.util.regex.Pattern;

/**
 * Value Object для email адреса.
 * Immutable, с валидацией в конструкторе.
 *
 * <p>Особенности:
 * <ul>
 *   <li>Автоматическая нормализация (lowercase, trim)</li>
 *   <li>Валидация формата email</li>
 *   <li>Проверка на null и пустые значения</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");

    private static final int MAX_LENGTH = 255;

    /**
     * Конструктор с валидацией и нормализацией.
     *
     * @param value email адрес
     * @throws IllegalArgumentException если email невалидный
     */
    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }

        value = value.toLowerCase().trim();

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Email не может быть длиннее " + MAX_LENGTH + " символов. Текущая длина: " + value.length()
            );
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Некорректный формат email: " + value);
        }
    }

    /**
     * Возвращает нормализованное значение email.
     *
     * @return email в нижнем регистре
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}