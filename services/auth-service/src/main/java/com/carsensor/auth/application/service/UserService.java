package com.carsensor.auth.application.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.carsensor.platform.dto.UserDto;

/**
 * Интерфейс сервиса для работы с пользователями
 */
public interface UserService {

    /**
     * Создание нового пользователя
     */
    UserDto createUser(UserDto userDto);

    /**
     * Получение пользователя по ID
     */
    UserDto getUserById(Long id);

    /**
     * Получение пользователя по username
     */
    UserDto getUserByUsername(String username);

    /**
     * Получение пользователя по email
     */
    UserDto getUserByEmail(String email);

    /**
     * Получение всех пользователей с пагинацией
     */
    Page<UserDto> getAllUsers(Pageable pageable);

    /**
     * Обновление пользователя
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

    /**
     * Блокировка пользователя
     */
    void blockUser(Long id);

    /**
     * Разблокировка пользователя
     */
    void unblockUser(Long id);

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

    /**
     * Получение статистики по пользователям
     */
    UserStatistics getUserStatistics();

    /**
     * Смена пароля пользователя
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * Сброс пароля (для администратора)
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * Назначение ролей пользователю
     */
    UserDto assignRoles(Long userId, List<String> roleNames);

    /**
     * Удаление ролей у пользователя
     */
    UserDto removeRoles(Long userId, List<String> roleNames);

    /**
     * DTO для статистики пользователей
     */
    record UserStatistics(
            long totalUsers,
            long activeUsers,
            long blockedUsers,
            long newUsersToday,
            long newUsersThisWeek,
            long newUsersThisMonth
    ) {
    }
}