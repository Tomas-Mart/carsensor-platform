// application/service/UserCommandService.java
package com.carsensor.auth.application.service.command;

import com.carsensor.auth.application.dto.UserDto;

/**
 * Сервис для командных операций с пользователями.
 * Отвечает за создание, обновление и удаление пользователей.
 * Принцип: Single Responsibility - только операции записи.
 */
public interface UserCommandService {

    /**
     * Создание нового пользователя
     */
    UserDto createUser(UserDto userDto);

    UserDto register(UserDto userDto);

    /**
     * Полное обновление пользователя
     */
    UserDto updateUser(Long id, UserDto userDto);

    /**
     * Частичное обновление пользователя
     */
    UserDto patchUser(Long id, UserDto userDto);

    /**
     * Удаление пользователя
     */
    void deleteUser(Long id);
}