// domain/service/PasswordEncoder.java
package com.carsensor.auth.domain.service;

/**
 * Интерфейс кодировщика паролей (порт).
 *
 * <p>Определяет контракт для кодирования и проверки паролей.
 * Реализация находится в инфраструктурном слое.
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
public interface PasswordEncoder {

    /**
     * Кодирует сырой пароль.
     *
     * @param rawPassword сырой пароль
     * @return закодированный пароль
     */
    String encode(String rawPassword);

    /**
     * Проверяет соответствие сырого пароля закодированному.
     *
     * @param rawPassword     сырой пароль
     * @param encodedPassword закодированный пароль
     * @return true если пароли совпадают
     */
    boolean matches(String rawPassword, String encodedPassword);
}