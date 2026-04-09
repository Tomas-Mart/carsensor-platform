// application/service/UserManagementService.java
package com.carsensor.auth.application.service.management;

import java.util.List;
import com.carsensor.auth.application.dto.UserDto;

/**
 * Сервис для управления пользователями (административные функции).
 * Отвечает за блокировку, разблокировку, управление ролями.
 * Принцип: Single Responsibility - только административные операции.
 */
public interface UserManagementService {

    /**
     * Блокировка пользователя
     */
    void blockUser(Long id);

    /**
     * Разблокировка пользователя
     */
    void unblockUser(Long id);

    /**
     * Назначение ролей пользователю
     */
    UserDto assignRoles(Long userId, List<String> roleNames);

    /**
     * Удаление ролей у пользователя
     */
    UserDto removeRoles(Long userId, List<String> roleNames);
}