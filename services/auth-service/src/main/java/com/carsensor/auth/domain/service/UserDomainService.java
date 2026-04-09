package com.carsensor.auth.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.domain.valueobject.Email;
import com.carsensor.auth.domain.valueobject.Password;
import com.carsensor.auth.domain.valueobject.Username;

/**
 * Доменный сервис для работы с пользователями.
 *
 * <p>Содержит бизнес-логику, которая не помещается в entity.
 * Работает только с entity и value objects, не знает о DTO.
 *
 * <p>Принципы:
 * <ul>
 *   <li>Single Responsibility - только операции с пользователями</li>
 *   <li>Dependency Inversion - зависит от абстракций (интерфейсов)</li>
 *   <li>Domain-Driven Design - использует value objects и entity</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
public interface UserDomainService {

    // ========== ОПЕРАЦИИ СОЗДАНИЯ И ПОЛУЧЕНИЯ ==========

    /**
     * Создание нового пользователя.
     * Выполняет проверку уникальности username и email.
     *
     * @param username  имя пользователя (value object)
     * @param email     email адрес (value object)
     * @param password  пароль (value object)
     * @param firstName имя
     * @param lastName  фамилия
     * @param roles     набор ролей
     * @return созданный пользователь
     * @throws IllegalArgumentException если username или email уже существуют
     */
    User createUser(
            Username username,
            Email email,
            Password password,
            String firstName,
            String lastName,
            Set<Role> roles
    );

    /**
     * Получение пользователя по ID.
     *
     * @param id идентификатор пользователя
     * @return Optional с пользователем или пустой Optional
     */
    Optional<User> getUserById(Long id);

    /**
     * Получение пользователя по username.
     *
     * @param username имя пользователя (value object)
     * @return Optional с пользователем или пустой Optional
     */
    Optional<User> getUserByUsername(Username username);

    /**
     * Получение пользователя по email.
     *
     * @param email email адрес (value object)
     * @return Optional с пользователем или пустой Optional
     */
    Optional<User> getUserByEmail(Email email);


    // ========== БИЗНЕС-ОПЕРАЦИИ ==========

    /**
     * Блокировка пользователя.
     * Устанавливает флаг блокировки и время блокировки.
     *
     * @param user пользователь для блокировки
     */
    void blockUser(User user);

    /**
     * Разблокировка пользователя.
     * Снимает флаг блокировки и сбрасывает счетчик неудачных попыток.
     *
     * @param user пользователь для разблокировки
     */
    void unblockUser(User user);

    /**
     * Смена пароля пользователя.
     *
     * @param user        пользователь
     * @param newPassword новый пароль (value object)
     */
    void changePassword(User user, Password newPassword);

    /**
     * Обновление профиля пользователя.
     *
     * @param user      пользователь
     * @param firstName имя (может быть null)
     * @param lastName  фамилия (может быть null)
     */
    void updateProfile(User user, String firstName, String lastName);


    // ========== ПРОВЕРКИ ==========

    /**
     * Проверка существования пользователя по username.
     *
     * @param username имя пользователя (value object)
     * @return true если пользователь существует
     */
    boolean existsByUsername(Username username);

    /**
     * Проверка существования пользователя по email.
     *
     * @param email email адрес (value object)
     * @return true если пользователь существует
     */
    boolean existsByEmail(Email email);


    // ========== ПОИСК ==========

    /**
     * Поиск пользователей по имени или фамилии.
     *
     * @param query поисковый запрос
     * @return список найденных пользователей
     */
    List<User> searchUsers(String query);


    // ========== УПРАВЛЕНИЕ РОЛЯМИ ==========

    /**
     * Назначение ролей пользователю.
     *
     * @param user  пользователь
     * @param roles набор ролей для назначения
     * @return обновленный пользователь
     */
    User assignRoles(User user, Set<Role> roles);

    /**
     * Удаление ролей у пользователя.
     *
     * @param user  пользователь
     * @param roles набор ролей для удаления
     * @return обновленный пользователь
     */
    User removeRoles(User user, Set<Role> roles);
}