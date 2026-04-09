package com.carsensor.auth.domain.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.domain.repository.UserRepository;
import com.carsensor.auth.domain.service.UserDomainService;
import com.carsensor.auth.domain.valueobject.Email;
import com.carsensor.auth.domain.valueobject.Password;
import com.carsensor.auth.domain.valueobject.Username;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация доменного сервиса для работы с пользователями.
 *
 * <p>Содержит бизнес-логику, связанную с управлением пользователями:
 * <ul>
 *   <li>Создание пользователя с проверкой уникальности</li>
 *   <li>Поиск пользователя по различным критериям</li>
 *   <li>Блокировка/разблокировка пользователя</li>
 *   <li>Смена пароля и обновление профиля</li>
 *   <li>Управление ролями пользователя</li>
 *   <li>Поиск и проверка существования пользователей</li>
 * </ul>
 *
 * <p>Данный сервис работает только с доменными объектами (entity и value objects)
 * и не зависит от DTO и внешних слоев. Это обеспечивает чистоту архитектуры
 * и возможность тестирования бизнес-логики в изоляции.
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserDomainServiceImpl implements UserDomainService {

    private final UserRepository userRepository;

    // ============================================================
    // ОПЕРАЦИИ СОЗДАНИЯ И ПОЛУЧЕНИЯ
    // ============================================================

    /**
     * {@inheritDoc}
     *
     * <p>Выполняет проверку уникальности username и email перед созданием.
     * При нарушении уникальности выбрасывает исключение.
     *
     * @param username  имя пользователя (value object)
     * @param email     email адрес (value object)
     * @param password  пароль (value object, уже зашифрован)
     * @param firstName имя пользователя
     * @param lastName  фамилия пользователя
     * @param roles     начальный набор ролей
     * @return созданный пользователь
     * @throws PlatformException.DuplicateResourceException если username или email уже существуют
     */
    @Override
    public User createUser(
            Username username,
            Email email,
            Password password,
            String firstName,
            String lastName,
            Set<Role> roles
    ) {
        log.info("Создание нового пользователя: {}", username.value());

        // Проверка уникальности username (извлекаем строковое значение из Value Object)
        if (userRepository.existsByUsername(username.value())) {
            log.warn("Попытка создания пользователя с уже существующим username: {}", username.value());
            throw new PlatformException.DuplicateResourceException("User", "username: " + username.value());
        }

        // Проверка уникальности email (извлекаем строковое значение из Value Object)
        if (userRepository.existsByEmail(email.value())) {
            log.warn("Попытка создания пользователя с уже существующим email: {}", email.value());
            throw new PlatformException.DuplicateResourceException("User", "email: " + email.value());
        }

        // Создание пользователя через фабричный метод entity
        User user = User.create(username, email, password, firstName, lastName, roles);

        User savedUser = userRepository.save(user);
        log.info("Пользователь успешно создан с id: {}", savedUser.getId());

        return savedUser;
    }

    /**
     * {@inheritDoc}
     *
     * @param id идентификатор пользователя
     * @return Optional с пользователем или пустой Optional
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        log.debug("Поиск пользователя по id: {}", id);
        return userRepository.findById(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param username имя пользователя (value object)
     * @return Optional с пользователем или пустой Optional
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(Username username) {
        log.debug("Поиск пользователя по username: {}", username.value());
        // Извлекаем строковое значение из Value Object
        return userRepository.findByUsername(username.value());
    }

    /**
     * {@inheritDoc}
     *
     * @param email email адрес (value object)
     * @return Optional с пользователем или пустой Optional
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(Email email) {
        log.debug("Поиск пользователя по email: {}", email.value());
        // Извлекаем строковое значение из Value Object
        return userRepository.findByEmail(email.value());
    }

    // ============================================================
    // БИЗНЕС-ОПЕРАЦИИ
    // ============================================================

    /**
     * {@inheritDoc}
     *
     * @param user пользователь для блокировки
     */
    @Override
    public void blockUser(User user) {
        log.info("Блокировка пользователя: {}", user.getUsername());
        user.block();
        userRepository.save(user);
        log.info("Пользователь успешно заблокирован: {}", user.getUsername());
    }

    /**
     * {@inheritDoc}
     *
     * @param user пользователь для разблокировки
     */
    @Override
    public void unblockUser(User user) {
        log.info("Разблокировка пользователя: {}", user.getUsername());
        user.unblock();
        userRepository.save(user);
        log.info("Пользователь успешно разблокирован: {}", user.getUsername());
    }

    /**
     * {@inheritDoc}
     *
     * @param user        пользователь
     * @param newPassword новый пароль (value object)
     */
    @Override
    public void changePassword(User user, Password newPassword) {
        log.info("Смена пароля для пользователя: {}", user.getUsername());
        user.changePassword(newPassword);
        userRepository.save(user);
        log.info("Пароль успешно изменен для пользователя: {}", user.getUsername());
    }

    /**
     * {@inheritDoc}
     *
     * @param user      пользователь
     * @param firstName имя (может быть null)
     * @param lastName  фамилия (может быть null)
     */
    @Override
    public void updateProfile(User user, String firstName, String lastName) {
        log.info("Обновление профиля пользователя: {}", user.getUsername());
        user.updateProfile(firstName, lastName);
        userRepository.save(user);
        log.info("Профиль пользователя успешно обновлен: {}", user.getUsername());
    }

    // ============================================================
    // ПРОВЕРКИ СУЩЕСТВОВАНИЯ
    // ============================================================

    /**
     * {@inheritDoc}
     *
     * @param username имя пользователя (value object)
     * @return true если пользователь существует
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(Username username) {
        log.debug("Проверка существования username: {}", username.value());
        // Извлекаем строковое значение из Value Object
        return userRepository.existsByUsername(username.value());
    }

    /**
     * {@inheritDoc}
     *
     * @param email email адрес (value object)
     * @return true если пользователь существует
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        log.debug("Проверка существования email: {}", email.value());
        // Извлекаем строковое значение из Value Object
        return userRepository.existsByEmail(email.value());
    }

    // ============================================================
    // ПОИСК
    // ============================================================

    /**
     * {@inheritDoc}
     *
     * @param query поисковый запрос
     * @return список найденных пользователей
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> searchUsers(String query) {
        log.debug("Поиск пользователей по запросу: {}", query);
        return userRepository.searchByFirstNameOrLastName(query);
    }

    // ============================================================
    // УПРАВЛЕНИЕ РОЛЯМИ
    // ============================================================

    /**
     * {@inheritDoc}
     *
     * @param user  пользователь
     * @param roles набор ролей для назначения
     * @return обновленный пользователь
     */
    @Override
    public User assignRoles(User user, Set<Role> roles) {
        log.info("Назначение ролей {} пользователю: {}",
                roles.stream().map(Role::getName).toList(),
                user.getUsername()
        );

        roles.forEach(user::addRole);
        User updatedUser = userRepository.save(user);

        log.info("Роли успешно назначены пользователю: {}", user.getUsername());
        return updatedUser;
    }

    /**
     * {@inheritDoc}
     *
     * @param user  пользователь
     * @param roles набор ролей для удаления
     * @return обновленный пользователь
     */
    @Override
    public User removeRoles(User user, Set<Role> roles) {
        log.info("Удаление ролей {} у пользователя: {}",
                roles.stream().map(Role::getName).toList(),
                user.getUsername()
        );

        roles.forEach(user::removeRole);
        User updatedUser = userRepository.save(user);

        log.info("Роли успешно удалены у пользователя: {}", user.getUsername());
        return updatedUser;
    }
}