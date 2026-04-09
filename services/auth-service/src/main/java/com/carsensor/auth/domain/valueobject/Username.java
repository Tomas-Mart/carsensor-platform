package com.carsensor.auth.domain.valueobject;

/**
 * Value Object для имени пользователя.
 * Immutable, с валидацией в конструкторе.
 *
 * <p>Особенности:
 * <ul>
 *   <li>Автоматическая нормализация (lowercase, trim)</li>
 *   <li>Проверка длины (3-50 символов)</li>
 *   <li>Проверка допустимых символов (буквы, цифры, ., _, -)</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
public record Username(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;
    private static final String ALLOWED_CHARS_PATTERN = "^[a-zA-Z0-9._-]+$";

    /**
     * Конструктор с валидацией и нормализацией.
     *
     * @param value имя пользователя
     * @throws IllegalArgumentException если имя невалидное
     */
    public Username {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }

        value = value.toLowerCase().trim();

        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Имя пользователя должно содержать минимум " + MIN_LENGTH + " символов. " +
                    "Текущая длина: " + value.length()
            );
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Имя пользователя не может превышать " + MAX_LENGTH + " символов. " +
                    "Текущая длина: " + value.length()
            );
        }

        if (!value.matches(ALLOWED_CHARS_PATTERN)) {
            throw new IllegalArgumentException(
                    "Имя пользователя может содержать только латинские буквы, цифры, точки, подчеркивания и дефисы. " +
                    "Недопустимое значение: " + value
            );
        }
    }

    /**
     * Возвращает нормализованное значение имени пользователя.
     *
     * @return имя в нижнем регистре
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}