// domain/valueobject/Password.java
package com.carsensor.auth.domain.valueobject;

import com.carsensor.auth.domain.service.PasswordEncoder;

/**
 * Value Object для пароля.
 * Immutable, хранится в зашифрованном виде.
 *
 * <p>Особенности:
 * <ul>
 *   <li>Хранит только зашифрованное значение</li>
 *   <li>Фабричный метод fromRaw() для создания из сырого пароля</li>
 *   <li>Метод matches() для проверки соответствия</li>
 *   <li>Валидация длины сырого пароля (8-100 символов)</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
public record Password(String encryptedValue) {

    private static final int MIN_RAW_LENGTH = 8;
    private static final int MAX_RAW_LENGTH = 100;

    /**
     * Конструктор с валидацией зашифрованного значения.
     *
     * @param encryptedValue зашифрованный пароль
     * @throws IllegalArgumentException если зашифрованное значение пустое
     */
    public Password {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            throw new IllegalArgumentException("Пароль не может быть пустым");
        }
    }

    /**
     * Создает пароль из сырого значения (только для регистрации/смены пароля).
     * Выполняет валидацию и шифрование.
     *
     * @param rawPassword сырой пароль
     * @param encoder     кодировщик паролей
     * @return объект Password с зашифрованным значением
     * @throws IllegalArgumentException если пароль не соответствует требованиям
     */
    public static Password fromRaw(String rawPassword, PasswordEncoder encoder) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Пароль не может быть пустым");
        }

        if (rawPassword.length() < MIN_RAW_LENGTH) {
            throw new IllegalArgumentException(
                    "Пароль должен содержать минимум " + MIN_RAW_LENGTH + " символов. " +
                    "Текущая длина: " + rawPassword.length()
            );
        }

        if (rawPassword.length() > MAX_RAW_LENGTH) {
            throw new IllegalArgumentException(
                    "Пароль не может превышать " + MAX_RAW_LENGTH + " символов. " +
                    "Текущая длина: " + rawPassword.length()
            );
        }

        return new Password(encoder.encode(rawPassword));
    }

    /**
     * Проверяет соответствие сырого пароля зашифрованному.
     *
     * @param rawPassword сырой пароль для проверки
     * @param encoder     кодировщик паролей
     * @return true если пароли совпадают
     */
    public boolean matches(String rawPassword, PasswordEncoder encoder) {
        if (rawPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encryptedValue);
    }

    /**
     * Возвращает зашифрованное значение пароля.
     *
     * @return зашифрованный пароль
     */
    public String getEncryptedValue() {
        return encryptedValue;
    }
}