// application/service/UserQueryService.java
package com.carsensor.auth.application.service.query;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.carsensor.auth.application.dto.UserDto;

/**
 * Сервис для запросов на чтение пользователей.
 * Отвечает за получение данных, поиск, проверку существования.
 * Принцип: CQRS - разделение команд и запросов.
 */
public interface UserQueryService {

    /**
     * Получение пользователя по ID
     */
    Optional<UserDto> getUserById(Long id);

    /**
     * Получение пользователя по username
     */
    Optional<UserDto> getUserByUsername(String username);

    /**
     * Получение пользователя по email
     */
    Optional<UserDto> getUserByEmail(String email);

    /**
     * Получение всех пользователей с пагинацией
     */
    Page<UserDto> getAllUsers(Pageable pageable);

    /**
     * Проверка существования пользователя по username
     */
    boolean existsByUsername(String username);

    /**
     * Проверка существования пользователя по email
     */
    boolean existsByEmail(String email);

    /**
     * Поиск пользователей по имени или фамилии
     */
    List<UserDto> searchUsers(String query);
}