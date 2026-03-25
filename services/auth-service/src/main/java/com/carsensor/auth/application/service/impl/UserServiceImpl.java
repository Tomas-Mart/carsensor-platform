package com.carsensor.auth.application.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.UserService;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.domain.repository.RoleRepository;
import com.carsensor.auth.domain.repository.UserRepository;
import com.carsensor.platform.dto.UserDto;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация сервиса для работы с пользователями.
 *
 * <p>Содержит бизнес-логику для управления пользователями,
 * включая CRUD операции, управление ролями, блокировку/разблокировку
 * и смену паролей.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // В реальном проекте здесь должен быть сервис для получения текущего пользователя
    private String getCurrentUsername() {
        // Временная заглушка - в реальном проекте получать из SecurityContext
        return "current_user";
    }

    /**
     * Проверяет, имеет ли текущий пользователь доступ к ресурсу.
     *
     * @param username      имя пользователя, чей ресурс проверяется
     * @param resourceOwner владелец ресурса
     * @throws PlatformException.AccessDeniedException если доступ запрещен
     */
    public void checkUserAccess(String username, String resourceOwner) {
        if (!username.equals(resourceOwner)) {
            throw new PlatformException.AccessDeniedException(
                    username,
                    "У вас нет доступа к этому ресурсу");
        }
    }

    /**
     * Проверяет, имеет ли текущий пользователь права на редактирование.
     *
     * @param user пользователь для проверки
     * @return true если имеет права
     */
    private boolean hasPermissionToEdit(User user) {
        // Реализация проверки прав
        return getCurrentUsername().equals(user.getUsername()) || isAdmin();
    }

    /**
     * Проверяет, является ли текущий пользователь администратором.
     *
     * @return true если администратор
     */
    private boolean isAdmin() {
        // Временная заглушка - в реальном проекте проверять роли
        return false;
    }

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("Creating new user: {}", userDto.username());

        validateUserUniqueness(userDto.username(), userDto.email());

        Set<Role> roles = getDefaultRoles();

        User user = User.builder()
                .username(userDto.username())
                .email(userDto.email())
                .password(passwordEncoder.encode(userDto.password()))
                .firstName(userDto.firstName())
                .lastName(userDto.lastName())
                .isActive(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());

        return mapToDto(savedUser);
    }

    @Override
    public UserDto getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);
        return userRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + id));
    }

    @Override
    public UserDto getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username)
                .map(this::mapToDto)
                .orElseThrow(() -> new PlatformException.UserNotFoundException(username));
    }

    @Override
    public UserDto getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .map(this::mapToDto)
                .orElseThrow(() -> new PlatformException.UserNotFoundException(email));
    }

    @Override
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pageable: {}", pageable);
        return userRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        log.info("Updating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + id));

        // Проверка уникальности при изменении username/email
        if (!user.getUsername().equals(userDto.username()) &&
                userRepository.existsByUsername(userDto.username())) {
            throw new PlatformException.DuplicateResourceException("User", "username: " + userDto.username());
        }

        if (!user.getEmail().equals(userDto.email()) &&
                userRepository.existsByEmail(userDto.email())) {
            throw new PlatformException.DuplicateResourceException("User", "email: " + userDto.email());
        }

        // Проверка прав доступа
        if (!hasPermissionToEdit(user)) {
            throw new PlatformException.AccessDeniedException(
                    getCurrentUsername(),
                    "У вас нет прав для редактирования этого пользователя");
        }

        updateUserFields(user, userDto);

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", id);

        return mapToDto(updatedUser);
    }

    @Override
    @Transactional
    public UserDto patchUser(Long id, UserDto userDto) {
        log.info("Patching user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + id));

        patchUserFields(user, userDto);

        User patchedUser = userRepository.save(user);
        log.info("User patched successfully: {}", id);

        return mapToDto(patchedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);

        if (!userRepository.existsById(id)) {
            throw new PlatformException.UserNotFoundException("id: " + id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully: {}", id);
    }

    @Override
    @Transactional
    public void blockUser(Long id) {
        log.info("Blocking user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + id));

        user.setActive(false);
        userRepository.save(user);

        log.info("User blocked successfully: {}", id);
    }

    @Override
    @Transactional
    public void unblockUser(Long id) {
        log.info("Unblocking user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + id));

        user.setActive(true);
        userRepository.save(user);

        log.info("User unblocked successfully: {}", id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public List<UserDto> searchUsers(String query) {
        log.debug("Searching users by query: {}", query);
        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserStatistics getUserStatistics() {
        log.debug("Fetching user statistics");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long blockedUsers = totalUsers - activeUsers;

        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime weekAgo = today.minusDays(7);
        LocalDateTime monthAgo = today.minusDays(30);

        long newUsersToday = userRepository.countByCreatedAtAfter(today);
        long newUsersThisWeek = userRepository.countByCreatedAtAfter(weekAgo);
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(monthAgo);

        return new UserStatistics(
                totalUsers,
                activeUsers,
                blockedUsers,
                newUsersToday,
                newUsersThisWeek,
                newUsersThisMonth
        );
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + userId));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new PlatformException.InvalidCredentialsException();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userId);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        log.info("Resetting password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", userId);
    }

    @Override
    @Transactional
    public UserDto assignRoles(Long userId, List<String> roleNames) {
        log.info("Assigning roles {} to user: {}", roleNames, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + userId));

        Set<Role> roles = getRolesByName(roleNames);
        user.getRoles().addAll(roles);

        User updatedUser = userRepository.save(user);
        log.info("Roles assigned successfully to user: {}", userId);

        return mapToDto(updatedUser);
    }

    @Override
    @Transactional
    public UserDto removeRoles(Long userId, List<String> roleNames) {
        log.info("Removing roles {} from user: {}", roleNames, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + userId));

        Set<Role> rolesToRemove = getRolesByName(roleNames);
        user.getRoles().removeAll(rolesToRemove);

        User updatedUser = userRepository.save(user);
        log.info("Roles removed successfully from user: {}", userId);

        return mapToDto(updatedUser);
    }

    // Приватные вспомогательные методы
    private void validateUserUniqueness(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new PlatformException.DuplicateResourceException("User", "username: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new PlatformException.DuplicateResourceException("User", "email: " + email);
        }
    }

    private Set<Role> getDefaultRoles() {
        return Set.of(
                roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new PlatformException.UserNotFoundException("ROLE_USER"))
        );
    }

    private Set<Role> getRolesByName(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }
        return roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new PlatformException.UserNotFoundException("Role: " + name)))
                .collect(Collectors.toSet());
    }

    private void updateUserFields(User user, UserDto userDto) {
        user.setUsername(userDto.username());
        user.setEmail(userDto.email());
        if (userDto.password() != null && !userDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.password()));
        }
        user.setFirstName(userDto.firstName());
        user.setLastName(userDto.lastName());
        user.setActive(userDto.isActive());

        if (userDto.roles() != null && !userDto.roles().isEmpty()) {
            user.setRoles(getRolesByName(userDto.roles()));
        }
    }

    private void patchUserFields(User user, UserDto userDto) {
        // Проверка и обновление username
        if (userDto.username() != null) {
            if (!user.getUsername().equals(userDto.username()) &&
                    userRepository.existsByUsername(userDto.username())) {
                throw new PlatformException.DuplicateResourceException(
                        "User", "username: " + userDto.username()
                );
            }
            user.setUsername(userDto.username());
        }

        // Проверка и обновление email
        if (userDto.email() != null) {
            if (!user.getEmail().equals(userDto.email()) &&
                    userRepository.existsByEmail(userDto.email())) {
                throw new PlatformException.DuplicateResourceException(
                        "User", "email: " + userDto.email()
                );
            }
            user.setEmail(userDto.email());
        }

        // Обновление пароля (если предоставлен)
        if (userDto.password() != null && !userDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.password()));
        }

        // Обновление имени
        if (userDto.firstName() != null) {
            user.setFirstName(userDto.firstName());
        }

        // Обновление фамилии
        if (userDto.lastName() != null) {
            user.setLastName(userDto.lastName());
        }

        // Обновление ролей (если предоставлены)
        if (userDto.roles() != null && !userDto.roles().isEmpty()) {
            user.setRoles(getRolesByName(userDto.roles()));
        }

        // isActive не меняем при patch - это делается отдельными методами block/unblock
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.isActive())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}