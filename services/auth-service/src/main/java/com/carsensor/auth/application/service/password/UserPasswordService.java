// application/service/UserPasswordService.java
package com.carsensor.auth.application.service.password;

/**
 * Сервис для управления паролями пользователей.
 * Отвечает за смену и сброс паролей.
 * Принцип: Single Responsibility - только операции с паролями.
 */
public interface UserPasswordService {

    /**
     * Смена пароля пользователем
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * Сброс пароля (для администратора)
     */
    void resetPassword(Long userId, String newPassword);
}